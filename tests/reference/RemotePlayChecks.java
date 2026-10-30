// SPDX-License-Identifier: GPL-3.0-or-later
import com.armsx2.remoteplay.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import org.json.JSONObject;
import kotlin.Unit;

/** Actual pinned wire protocol and viewer input; no sockets, native VM or Android UI. */
public final class RemotePlayChecks {
    private static int checked;
    private static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);checked++;}
    public static void main(String[] args)throws Exception{
        RemotePlayProtocol p=RemotePlayProtocol.INSTANCE;
        RemotePlayProtocol.PadState state=new RemotePlayProtocol.PadState(0x3fff,0,255,64,192,13,255);
        kotlin.Pair<Integer,RemotePlayProtocol.PadState> result=p.decodePad(p.encodePad(Integer.MAX_VALUE,state));
        check(result!=null&&result.getFirst()==Integer.MAX_VALUE&&result.getSecond().equals(state),"all input fields round-trip");
        ByteBuffer pad=p.encodePad(Integer.MIN_VALUE,state);
        check(pad.remaining()==16&&pad.get(0)==1&&pad.get(9)==0&&pad.get(10)==(byte)255,"official little-endian pad layout");
        for(int size=0;size<16;size++){ByteBuffer shortPad=pad.duplicate();shortPad.limit(size);check(p.decodePad(shortPad)==null,"truncated pad rejected "+size);}
        check(p.axisByte(-1f)==0&&p.axisByte(0f)==128&&p.axisByte(1f)==255&&p.axisByte(9f)==255,"stick endpoints, neutral and clamp");
        check(p.getPAD_KEYCODES()[0]==19&&p.getPAD_KEYCODES()[10]==96,"Up and Cross use correct donor routes");
        ByteBuffer pcm=ByteBuffer.allocateDirect(16);for(int i=0;i<16;i++)pcm.put(i,(byte)(i+1));
        byte[] decoded=p.decodeAudio(p.encodeAudio(7,pcm,4));
        check(decoded!=null&&decoded.length==16&&decoded[15]==16,"stereo PCM retained");
        check(p.decodeAudio(ByteBuffer.allocate(8))==null,"empty audio rejected");
        byte[] opus={1,2,3},previous={4,5};
        RemotePlayProtocol.OpusPacket packet=p.decodeOpus(p.encodeOpus(4,opus,previous));
        check(packet!=null&&packet.getSeq()==4&&Arrays.equals(packet.getFrame(),opus)&&Arrays.equals(packet.getPrevious(),previous),"Opus with recovery frame");
        ByteBuffer bad=p.encodeOpus(4,opus,previous);bad.putShort(2,(short)2000);
        check(p.decodeOpus(bad)==null,"oversized Opus rejected");
        check(p.decodeOpus(ByteBuffer.wrap(new byte[]{5,0,0,0,0,0,0,0,0}))==null,"zero Opus frame rejected");
        RemotePlayProtocol.Rumble rumble=p.decodeRumble(p.encodeRumble(55,255,18,true));
        check(rumble!=null&&rumble.getLarge()==255&&rumble.getSmall()==18&&rumble.getPaused(),"rumble motors and independent pause flag");
        check(p.isWaiting(p.encodeWaiting()),"approval wait message preserved");
        check(p.isBye(p.encodeBye(p.byeCode("denied")))&&p.byeReason(p.encodeBye(p.byeCode("denied"))).equals("denied"),"approval refusal round-trip");
        JSONObject offer=new JSONObject().put("t","offer").put("v",1).put("sdp","v=0\r\na=ice-ufrag:test\r\n");
        String code=p.encodeCode(offer);
        check(code.startsWith("ARMSX2-RP1."),"compatible invite prefix");
        JSONObject unpacked=p.decodeCode("Chat message before "+code+" after");
        check(unpacked!=null&&unpacked.getString("sdp").equals(offer.getString("sdp")),"pasted whole chat accepted");
        check(p.decodeCode("wrong code")==null&&p.decodeCode("ARMSX2-RP1.abc")==null,"bad compressed invite rejected");
        check(p.decodeCode(code.substring(0,code.length()-5))==null,"truncated deflate stream rejected");
        check(p.decodeCode(p.encodeCode(new JSONObject().put("sdp",String.join("",Collections.nCopies(70_000,"x")))))==null,"inflation bounded to 64 KiB");
        RemotePlayLobby lobby=RemotePlayLobby.INSTANCE;
        check("K7M2QX".equals(lobby.normalize(" k7m-2qx ")),"lobby codes normalize");
        check(lobby.normalize("O00000")==null&&lobby.normalize("too-long")==null,"ambiguous or invalid code rejected");
        check("K7M-2QX".equals(lobby.display("K7M2QX")),"readable lobby code");
        RemoteAddress address=RemoteAddress.INSTANCE;
        check(address.parse("192.168.1.20").getSecond()==38433,"default signaling port");
        check(address.parse("host.local:42000").getSecond()==42000,"manual signaling port");
        for(String invalid:new String[]{"",":42","host:0","host:65536","host:abc","http://host","host/path","host bad","host:1:2"})check(address.parse(invalid)==null,"invalid address rejected "+invalid);
        List<RemotePlayProtocol.PadState> sent=new ArrayList<>();
        ViewerInput input=new ViewerInput(next->{sent.add(next);return Unit.INSTANCE;});
        input.setTouch(state);input.releaseAll();
        check(sent.size()==2&&sent.get(0).equals(state)&&sent.get(1).equals(new RemotePlayProtocol.PadState()),"held buttons, sticks and triggers clear on lifecycle release");
        input.releaseAll();check(sent.size()==2,"duplicate release does not resend stale state");
        System.out.println("PASS: "+checked+" Remote Play protocol/input/address cases; no network or device execution.");
    }
}

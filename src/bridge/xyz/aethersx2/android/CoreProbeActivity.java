// SPDX-License-Identifier: GPL-3.0-or-later
package xyz.aethersx2.android;
import org.local.aetherarms.Bridge;
import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.util.Log;

/** Local debug-only probe, exercising the real ARMSX2 core through the adapter. */
public final class CoreProbeActivity extends Activity implements SurfaceHolder.Callback {
    private Thread worker;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Class<?> host = Class.forName("xyz.aethersx2.android.NativeLibrary");
            if (!(Boolean)host.getMethod("initializeOnce", android.content.Context.class, boolean.class).invoke(null, getApplicationContext(), false)) throw new IllegalStateException("Legacy host init failed");
            String bios = getIntent().getStringExtra("bios");
            if (bios != null) getSharedPreferences(getPackageName()+"_preferences", MODE_PRIVATE).edit().putString("Filenames/BIOS", bios).commit();
            Bridge.syncSettings();
            LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
            Button stop = new Button(this); stop.setText("Aether UI + ARMSX2 core — stop test");
            stop.setOnClickListener(v -> finish()); layout.addView(stop);
            SurfaceView surface = new SurfaceView(this); surface.getHolder().addCallback(this);
            layout.addView(surface, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(layout);
        } catch (Exception e) { Log.e("AetherArmsBridge", "Probe startup failed", e); throw new IllegalStateException(e); }
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Bridge.changeSurface(holder.getSurface(), width, height, getWindowManager().getDefaultDisplay().getRefreshRate());
        if (worker == null) {
            worker = new Thread(() -> Bridge.runVMThread(this, getIntent().getStringExtra("game"), null), "ArmsxProbeVM");
            worker.start();
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        Bridge.stopVMThreadLoop(false); Bridge.changeSurface(null,0,0,60);
    }
}

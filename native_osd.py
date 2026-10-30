# SPDX-License-Identifier: GPL-3.0-or-later
"""Build a small JNI adapter to the pinned donor's existing native OSD queue."""
from pathlib import Path
import hashlib
import json
import os
import struct
import subprocess
from check_alignment import inspect_elf

ROOT = Path(__file__).resolve().parent


def _at(data, va, size):
    phoff = struct.unpack_from('<Q', data, 32)[0]
    stride, count = struct.unpack_from('<HH', data, 54)
    for index in range(count):
        kind, flags, off, addr, _, length, _, _ = struct.unpack_from('<IIQQQQQQ', data, phoff + index * stride)
        if kind == 1 and flags & 1 and addr <= va and va + size <= addr + length:
            return data[off + va - addr:off + va - addr + size]
    raise AssertionError('OSD binding is outside an executable ELF segment')


def prepare_native_osd(work, sdk, prefix, env, lock):
    spec = json.loads((ROOT / 'config/native-osd.json').read_text())
    assert spec['donor_apk_sha256'] == lock['sha256'], 'Review native OSD adapter on core upgrade'
    donor = ROOT / 'donor/lib/arm64-v8a'
    assert hashlib.sha256((donor/'libc++_shared.so').read_bytes()).hexdigest() == spec['libcxx_sha256']
    build = ROOT / 'build/native-osd'
    build.mkdir(parents=True, exist_ok=True)
    rows = []
    for variant in spec['variants']:
        data = (donor/variant['library']).read_bytes()
        assert hashlib.sha256(data).hexdigest() == variant['sha256'], variant['library']
        listing = subprocess.run(['readelf','--dyn-syms','--wide',str(donor/variant['library'])],env=env,text=True,capture_output=True,check=True).stdout
        anchor = next(int(line.split()[1],16) for line in listing.splitlines() if line.split() and line.split()[-1] == variant['anchor_symbol'])
        assert anchor == variant['anchor_va'], 'Review native OSD load-base anchor'

        prefix_bytes = bytes.fromhex(variant['function_prefix_hex'])
        assert len(prefix_bytes) == 64 and _at(data,variant['function_va'],64) == prefix_bytes
        call = _at(data,variant['reference_call_va'],4)
        assert call.hex() == variant['reference_call_hex']
        opcode = struct.unpack('<I',call)[0]
        immediate = opcode & 0x3ffffff
        if immediate & (1<<25): immediate -= 1<<26
        assert opcode >> 26 == 0x25 and variant['reference_call_va'] + 4*immediate == variant['function_va']
        rows.append('{"'+variant['library']+'",'+str(variant['anchor_va'])+','+str(variant['function_va'])+',{'+','.join(str(b) for b in prefix_bytes)+'}}')
    (build/'native_osd_binding.h').write_text(
        '#pragma once\n#include <cstdint>\nstruct OsdBinding { const char* library; uintptr_t anchor, function; unsigned char prefix[64]; };\n'
        'static constexpr OsdBinding OSD_BINDINGS[] = {'+','.join(rows)+'};\n')
    ndk = Path(os.environ.get('AETHER_ANDROID_NDK',str(sdk/'ndk/26.1.10909125')))
    sysroot = ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot'
    assert (sysroot/'usr/include/c++/v1/string').is_file(), 'NDK C++ headers/sysroot missing'
    flags = ['--target=aarch64-linux-android26','--sysroot='+str(sysroot),'-std=c++17','-O2',
             '-fno-termux-rpath','-nostdinc++','-isystem',str(sysroot/'usr/include/c++/v1'),'-nostdlib++',
             '-fvisibility=hidden','-Wl,-z,max-page-size=16384','-Wl,-z,common-page-size=16384',
             '-L'+str(donor),'-L'+str(ndk/'toolchains/llvm/prebuilt/linux-x86_64/lib/clang/17/lib/linux/aarch64'),
             '-Wl,--exclude-libs,ALL']
    output = work/'lib/arm64-v8a/libhybridosd.so'
    subprocess.run([str(prefix/'bin/clang++'),*flags,'-shared','-fPIC','-Wl,--no-undefined',
                    '-Wl,-soname,libhybridosd.so','-I'+str(build),str(ROOT/'native/core_osd.cpp'),
                    '-lc++_shared','-ldl','-o',str(output)],env=env,check=True,capture_output=True,text=True)
    dynamic = subprocess.run(['readelf','-d',str(output)],env=env,text=True,capture_output=True,check=True).stdout
    assert 'RPATH' not in dynamic and 'RUNPATH' not in dynamic, 'Do not embed build-machine library paths'
    exports = subprocess.run(['readelf','--dyn-syms','--wide',str(output)],env=env,text=True,capture_output=True,check=True).stdout
    assert 'Java_org_local_aetherarms_CoreOsd_post' in exports
    alignment = inspect_elf(output.read_bytes())
    assert alignment['elf_layout_16k'], alignment['issues']
    # Pure ABI regression with the donor's libc++ runtime. Does not load any core or start a VM.
    test = build/'osd-abi-check'
    subprocess.run([str(prefix/'bin/clang++'),*flags,str(ROOT/'tests/NativeOsdAbiChecks.cpp'),
                    '-lc++_shared','-o',str(test)],env=env,check=True,capture_output=True,text=True)
    result = subprocess.run([str(test)],env={**env,'LD_LIBRARY_PATH':str(donor)+':'+str(prefix/'lib')},check=True,capture_output=True,text=True)
    report = {'binding_file':'config/native-osd.json','variants_verified':len(rows),'native_core_binaries_modified':False,
              'adapter_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'adapter_elf_16k':True,
              'abi_host_check':result.stdout.strip(),'device_app_or_vm_launched':False}
    (ROOT/'reports/native-osd.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

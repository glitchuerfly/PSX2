#!/usr/bin/env python3
"""Inspect ELF64 LOAD/RELRO and APK ZIP alignment without modifying binaries."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile

PAGE = 16384

def inspect_elf(data):
    if data[:6] != b'\x7fELF\x02\x01':
        raise ValueError('Expected little-endian ELF64')
    phoff = struct.unpack_from('<Q', data, 32)[0]
    entsize, count = struct.unpack_from('<HH', data, 54)
    if entsize < 56 or phoff + entsize * count > len(data):
        raise ValueError('Invalid program header table')
    loads, relro, issues = [], [], []
    for i in range(count):
        kind, flags, off, va, _, size, mem, align = struct.unpack_from('<IIQQQQQQ', data, phoff + i * entsize)
        if kind == 1:
            item = dict(offset=off, virtual_address=va, file_size=size, memory_size=mem, flags=flags, alignment=align)
            loads.append(item)
            if align < PAGE or align & (align - 1):
                issues.append(f'LOAD {i}: alignment {align} below 16384 or invalid')
            if (off - va) % PAGE:
                issues.append(f'LOAD {i}: file offset and virtual address differ modulo 16384')
            if off + size > len(data) or size > mem:
                issues.append(f'LOAD {i}: invalid segment bounds')
        elif kind == 0x6474e552:
            relro.append(dict(virtual_address=va, memory_size=mem, end=va + mem))
    if not loads:
        issues.append('No LOAD segments')
    for left, right in zip(loads, loads[1:]):
        if (left['virtual_address'] + left['memory_size'] + PAGE - 1) // PAGE > right['virtual_address'] // PAGE:
            if left['flags'] != right['flags']:
                issues.append('LOAD segments with different permissions share a 16 KB page')
    load_ok = not issues
    for item in relro:
        dedicated = any(x['virtual_address'] == item['virtual_address'] and x['memory_size'] == item['memory_size'] for x in loads)
        # A standalone RELRO LOAD may end partway through a 16 KB page
        # when the next writable LOAD is on a separate page (Android guide).
        boundary = (item['end'] + PAGE - 1) // PAGE * PAGE
        shares_tail = any(x['flags'] & 2 and x['virtual_address'] < boundary and x['virtual_address'] + x['memory_size'] > item['end'] for x in loads)
        item['dedicated_load'] = dedicated
        item['end_aligned_16k'] = item['end'] % PAGE == 0
        if not item['end_aligned_16k'] and (not dedicated or shares_tail):
            issues.append('RELRO protection overlaps writable data in a 16 KB page')
    return dict(load_segments_16k=load_ok, elf_layout_16k=not issues, loads=loads, relro=relro, issues=issues,
                sha256=hashlib.sha256(data).hexdigest())

def inspect_apk(apk):
    libraries = {}
    with zipfile.ZipFile(apk) as z, open(apk, 'rb') as stream:
        for info in z.infolist():
            if not (info.filename.startswith('lib/') and info.filename.endswith('.so')):
                continue
            stream.seek(info.header_offset)
            header = stream.read(30)
            name_len, extra_len = struct.unpack_from('<HH', header, 26)
            offset = info.header_offset + 30 + name_len + extra_len
            entry = inspect_elf(z.read(info))
            entry.update(zip_method=info.compress_type, zip_data_offset=offset,
                         zip_alignment_16k=offset % PAGE == 0 if info.compress_type == zipfile.ZIP_STORED else None)
            libraries[info.filename] = entry
    return dict(apk=str(apk), sha256=hashlib.sha256(Path(apk).read_bytes()).hexdigest(),
                all_elf_layouts_16k=bool(libraries) and all(x['elf_layout_16k'] for x in libraries.values()),
                all_stored_native_entries_zip_aligned_16k=all(x['zip_alignment_16k'] is not False for x in libraries.values()),
                runtime_16k_tested=False, libraries=libraries)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    report = inspect_apk(args.apk)
    encoded = json.dumps(report, indent=2) + '\n'
    if args.output:
        args.output.write_text(encoded)
    print(encoded, end='')

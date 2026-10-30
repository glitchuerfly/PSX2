// SPDX-License-Identifier: GPL-3.0-or-later
// Verify the Android NDK std::string representation used by the pinned core call ABI.
#include <cassert>
#include <cstring>
#include <iostream>
#include <string>

int main() {
    static_assert(sizeof(std::string) == 24);
    std::string small("status");
    const auto* bytes = reinterpret_cast<const unsigned char*>(&small);
    assert(bytes[0] == 12 && std::memcmp(bytes + 1, "status", 6) == 0);
    std::string large("Controller configuration loaded with a long message");
    const auto* words = reinterpret_cast<const uintptr_t*>(&large);
    assert((words[0] & 1) == 1 && words[1] == large.size());
    assert(reinterpret_cast<const char*>(words[2]) == large.data());
    std::string moved(std::move(large));
    assert(large.empty() && moved == "Controller configuration loaded with a long message");
    std::cout << "PASS: 24-byte NDK short/long strings, ownership transfer and donor libc++ allocation/destruction; no core loaded.\n";
}

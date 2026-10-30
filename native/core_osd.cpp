// SPDX-License-Identifier: GPL-3.0-or-later
// Hybrid adapter: call the pinned GPL core's original, mutex-protected OSD queue.
#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdint>
#include <string>
#include <utility>
#include "native_osd_binding.h"

static_assert(sizeof(void*) == 8 && sizeof(std::string) == 24, "Review Android core C++ ABI");
using AddMessage = void (*)(std::string, std::string, float);

extern "C" JNIEXPORT jboolean JNICALL
Java_org_local_aetherarms_CoreOsd_post(JNIEnv* env, jclass, jstring message) {
    if (!message || env->GetStringUTFLength(message) > 512) return JNI_FALSE;
    for (const auto& binding : OSD_BINDINGS) {
        void* handle = dlopen(binding.library, RTLD_NOW | RTLD_NOLOAD);
        if (!handle) continue;
        void* anchor = dlsym(handle, "Java_kr_co_iefriends_pcsx2_NativeApp_runVMThread");
        Dl_info image{};
        bool valid = anchor && dladdr(anchor, &image) && image.dli_fbase &&
            reinterpret_cast<uintptr_t>(anchor) - reinterpret_cast<uintptr_t>(image.dli_fbase) == binding.anchor;
        auto* entry = valid ? reinterpret_cast<const unsigned char*>(image.dli_fbase) + binding.function : nullptr;
        if (!entry || std::memcmp(entry, binding.prefix, sizeof(binding.prefix)) != 0) {
            dlclose(handle);
            return JNI_FALSE; // Never call a stale offset after a core upgrade.
        }
        const char* text = env->GetStringUTFChars(message, nullptr);
        if (!text) { dlclose(handle); return JNI_FALSE; }
        bool posted = false;
        try {
            std::string body(text);
            env->ReleaseStringUTFChars(message, text);
            text = nullptr;
            // The original queue owns scale, inset, ordering, font, fading and OSD visibility.
            reinterpret_cast<AddMessage>(const_cast<unsigned char*>(entry))(
                std::string("hybrid_controller_config"), std::move(body), 3.0f);
            posted = true;
        } catch (...) { }
        if (text) env->ReleaseStringUTFChars(message, text);
        dlclose(handle);
        return posted ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

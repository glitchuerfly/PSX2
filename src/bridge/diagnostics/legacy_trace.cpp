#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>

static thread_local const JNINativeInterface_* original;
static thread_local JNINativeInterface_ traced;
static jmethodID get_method(JNIEnv* env, jclass clazz, const char* name, const char* sig) {
    __android_log_print(ANDROID_LOG_INFO,"AetherArmsTrace","GetMethodID %s %s",name,sig);
    env->functions=original;
    jmethodID result=original->GetMethodID(env,clazz,name,sig);
    env->functions=&traced;
    return result;
}
static jmethodID get_static_method(JNIEnv* env, jclass clazz, const char* name, const char* sig) {
    __android_log_print(ANDROID_LOG_INFO,"AetherArmsTrace","GetStaticMethodID %s %s",name,sig);
    env->functions=original;
    jmethodID result=original->GetStaticMethodID(env,clazz,name,sig);
    env->functions=&traced;
    return result;
}
static jclass find_class(JNIEnv* env, const char* name) {
    __android_log_print(ANDROID_LOG_INFO,"AetherArmsTrace","FindClass %s",name);
    env->functions=original;
    jclass result=original->FindClass(env,name);
    env->functions=&traced;
    return result;
}
static jfieldID get_field(JNIEnv* env,jclass clazz,const char* name,const char* sig) {
    __android_log_print(ANDROID_LOG_INFO,"AetherArmsTrace","GetFieldID %s %s",name,sig);
    env->functions=original;
    jfieldID result=original->GetFieldID(env,clazz,name,sig);
    env->functions=&traced;
    return result;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_org_local_aetherarms_Bridge_traceInitialize(JNIEnv* env,jclass,jobject context,jstring root,jstring cache,jstring device) {
    void* handle=dlopen("libemucore.so",RTLD_NOW|RTLD_NOLOAD);
    if (!handle) { __android_log_print(ANDROID_LOG_ERROR,"AetherArmsTrace","%s",dlerror()); return false; }
    auto init=reinterpret_cast<jboolean(*)(JNIEnv*,jclass,jobject,jstring,jstring,jstring)>(dlsym(handle,"Java_xyz_aethersx2_android_NativeLibrary_initialize"));
    if (!init) return false;
    jclass clazz=env->FindClass("xyz/aethersx2/android/NativeLibrary");
    original=env->functions;
    traced=*original;
    traced.GetMethodID=get_method;
    traced.GetStaticMethodID=get_static_method;
    traced.FindClass=find_class;
    traced.GetFieldID=get_field;
    env->functions=&traced;
    jboolean result=init(env,clazz,context,root,cache,device);
    env->functions=original;
    __android_log_print(ANDROID_LOG_INFO,"AetherArmsTrace","Legacy initialize returned %d",result);
    return result;
}

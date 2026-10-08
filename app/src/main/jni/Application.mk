# Modernised for NDK r27+ (tested against 29.0.13113456).
#
# Changes from the 2013 original:
#   APP_STL      stlport_shared no longer exists in the NDK. The cores are built
#                with a statically linked libc++ so each plugin .so is
#                self-contained and no libc++_shared.so has to ship alongside.
#   APP_ABI      armeabi/armeabi-v7a were removed from modern NDKs. 64-bit ABIs
#                are the default here.
#   APP_PLATFORM lowest supported API, aligned with minSdk 21 in build.gradle.
APP_STL := c++_static
APP_ABI := arm64-v8a x86_64
APP_PLATFORM := android-21

# These emulator sources predate C99/C++11 and rely on constructs that are hard
# errors under modern clang (implicit int, implicit function declarations,
# K&R-style definitions, `register`). They are third-party cores, so the flags
# below restore the permissive dialect they were written against instead of
# rewriting ~600 vendored files.
#
# C++14 rather than the NDK default of C++17: libmodplug declares its sample
# buffers `register`, which ISO C++17 deleted outright.
APP_CFLAGS := -fcommon \
             -Wno-implicit-function-declaration -Wno-implicit-int \
             -Wno-int-conversion -Wno-incompatible-pointer-types \
             -Wno-return-type -Wno-deprecated-non-prototype \
             -Wno-pointer-sign -Wno-format -Wno-user-defined-warnings \
             -DLITTLE_ENDIAN=1
APP_CPPFLAGS := -std=c++14 -fpermissive -fcommon -w -Wno-register \
             -Wno-deprecated-non-prototype -Wno-register -DLITTLE_ENDIAN=1

LOCAL_PATH := $(call my-dir)

U3 := uade3
SRC := $(U3)/src
FE := $(SRC)/frontends/common
FIN := $(SRC)/frontends/include
ZAK := $(U3)/zakalwe
BEN := $(U3)/bencodetools

# ---------------------------------------------------------------------------
# uadecore (m68k Amiga emulator).  Android can not exec a separate uadecore
# binary, so it is compiled into this library and runs in a thread.  Core
# sources are built with exit() redirected to uadecore_terminate() so that an
# exit inside the emulator only ends the emulator thread.
# ---------------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE := uade_core

LOCAL_SRC_FILES := \
	$(SRC)/newcpu.c \
	$(SRC)/memory.c \
	$(SRC)/custom.c \
	$(SRC)/cia.c \
	$(SRC)/audio.c \
	$(SRC)/cpustbl.c \
	$(SRC)/cpudefs.c \
	$(SRC)/missing.c \
	$(SRC)/sd-sound.c \
	$(SRC)/md-support.c \
	$(SRC)/cfgfile.c \
	$(SRC)/fpp.c \
	$(SRC)/debug.c \
	$(SRC)/readcpu.c \
	$(SRC)/uade.c \
	$(SRC)/uademain.c \
	$(SRC)/sinctable.c \
	$(SRC)/text_scope.c \
	$(SRC)/write_audio.c \
	$(SRC)/state_detection.c \
	$(SRC)/uade_logging.c \
	$(SRC)/binary_deduper.c \
	$(SRC)/cpuemu1.c \
	$(SRC)/cpuemu2.c \
	$(SRC)/cpuemu3.c \
	$(SRC)/cpuemu4.c \
	$(SRC)/cpuemu5.c \
	$(SRC)/cpuemu6.c \
	$(SRC)/cpuemu7.c \
	$(SRC)/cpuemu8.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/$(SRC) \
	$(LOCAL_PATH)/$(SRC)/include \
	$(LOCAL_PATH)/$(FE) \
	$(LOCAL_PATH)/$(FIN) \
	$(LOCAL_PATH)/$(ZAK)/include \
	$(LOCAL_PATH)/$(BEN)/include

LOCAL_CFLAGS := $(MY_CFLAGS) -Dexit=uadecore_terminate -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0

include $(BUILD_STATIC_LIBRARY)

# ---------------------------------------------------------------------------
# libuade frontend (common) + vendored libzakalwe and bencode-tools.
# ---------------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE := uade_common

LOCAL_SRC_FILES := \
	$(FE)/amifilemagic.c \
	$(FE)/eagleplayer.c \
	$(FE)/effects.c \
	$(FE)/fifo.c \
	$(FE)/rmc.c \
	$(FE)/songdb.c \
	$(FE)/songinfo.c \
	$(FE)/support.c \
	$(FE)/uadeconf.c \
	$(FE)/uadecontrol.c \
	$(FE)/uadestate.c \
	$(FE)/uadeutils.c \
	$(FE)/uadeipc.c \
	$(FE)/unixatomic.c \
	$(FE)/unixsupport.c \
	$(FE)/unixwalkdir.c \
	$(FE)/vparray.c \
	$(FE)/uade_android.c \
	$(ZAK)/array.c \
	$(ZAK)/base.c \
	$(ZAK)/bitfield.c \
	$(ZAK)/endianess.c \
	$(ZAK)/file.c \
	$(ZAK)/input.c \
	$(ZAK)/math.c \
	$(ZAK)/mem.c \
	$(ZAK)/norm_inv_table_65536.c \
	$(ZAK)/random.c \
	$(ZAK)/string.c \
	$(ZAK)/str_array.c \
	$(ZAK)/time.c \
	$(ZAK)/unix-support.c \
	$(BEN)/bencode.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/$(SRC)/include \
	$(LOCAL_PATH)/$(FE) \
	$(LOCAL_PATH)/$(FIN) \
	$(LOCAL_PATH)/$(ZAK)/include \
	$(LOCAL_PATH)/$(BEN)/include

LOCAL_CFLAGS := $(MY_CFLAGS) -DUADE_HAVE_CYGWIN

include $(BUILD_STATIC_LIBRARY)

# ---------------------------------------------------------------------------
# JNI wrapper shared library.  The Java side loads "libuade.so".
# ---------------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE := uade

LOCAL_SRC_FILES := UADEPlugin.cpp

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/$(FE) \
	$(LOCAL_PATH)/$(FIN) \
	$(LOCAL_PATH)/$(SRC)/include

LOCAL_CFLAGS := $(MY_CFLAGS)
LOCAL_CPPFLAGS := $(MY_CPPFLAGS)
LOCAL_LDLIBS := -llog

LOCAL_WHOLE_STATIC_LIBRARIES := uade_core uade_common

include $(BUILD_SHARED_LIBRARY)

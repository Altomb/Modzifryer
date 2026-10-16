LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := sidplay2

LOCAL_SRC_FILES :=  SidplayPlugin.cpp \
        resid/envelope.cpp \
        resid/extfilt.cpp \
        resid/filter.cpp \
        resid/pot.cpp \
        resid/sid.cpp \
        resid/version.cpp \
        resid/voice.cpp \
        resid/wave.cpp \
        resid/wave6581__ST.cpp \
        resid/wave6581_P_T.cpp \
        resid/wave6581_PS_.cpp \
        resid/wave6581_PST.cpp \
        resid/wave8580__ST.cpp \
        resid/wave8580_P_T.cpp \
        resid/wave8580_PS_.cpp \
        resid/wave8580_PST.cpp \
        resid-builder/src/resid-builder.cpp \
        resid-builder/src/resid-emu.cpp \
        libsidplay2/src/config.cpp \
        libsidplay2/src/event.cpp \
        libsidplay2/src/mixer.cpp \
        libsidplay2/src/player.cpp \
        libsidplay2/src/psiddrv.cpp \
        libsidplay2/src/reloc65.c \
        libsidplay2/src/mos6510/mos6510.cpp \
        libsidplay2/src/mos656x/mos656x.cpp \
        libsidplay2/src/mos6526/mos6526.cpp \
        libsidplay2/src/sid6526/sid6526.cpp \
        libsidplay2/src/sidtune/IconInfo.cpp \
        libsidplay2/src/sidtune/InfoFile.cpp \
        libsidplay2/src/sidtune/MUS.cpp \
        libsidplay2/src/sidtune/p00.cpp \
        libsidplay2/src/sidtune/PP20.cpp \
        libsidplay2/src/sidtune/prg.cpp \
        libsidplay2/src/sidtune/PSID.cpp \
        libsidplay2/src/sidtune/SidTune.cpp \
        libsidplay2/src/sidtune/SidTuneTools.cpp \
        libsidplay2/src/xsid/xsid.cpp

LOCAL_C_INCLUDES  += $(LOCAL_PATH)
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/include
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/include/sidplay
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/include/sidplay/imp
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/mos6510
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/mos6526
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/mos656x
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/sid6526
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/sidtune
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/libsidplay2/src/xsid
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/resid
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/resid-builder/include
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/resid-builder/include/sidplay/builders
LOCAL_C_INCLUDES  += $(LOCAL_PATH)/resid-builder/src

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

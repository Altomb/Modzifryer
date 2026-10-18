# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
 
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := unrar

# Object list mirrors 'OBJECTS' from unrar/makefile.
FILES = unrar/rar.cpp unrar/strlist.cpp unrar/strfn.cpp unrar/pathfn.cpp unrar/smallfn.cpp unrar/global.cpp unrar/file.cpp unrar/filefn.cpp unrar/filcreat.cpp \
	unrar/archive.cpp unrar/arcread.cpp unrar/unicode.cpp unrar/system.cpp unrar/crypt.cpp unrar/crc.cpp unrar/rawread.cpp unrar/encname.cpp \
	unrar/resource.cpp unrar/match.cpp unrar/timefn.cpp unrar/rdwrfn.cpp unrar/consio.cpp unrar/options.cpp unrar/errhnd.cpp unrar/rarvm.cpp \
	unrar/secpassword.cpp unrar/rijndael.cpp unrar/getbits.cpp unrar/sha1.cpp unrar/sha256.cpp unrar/blake2s.cpp unrar/hash.cpp unrar/extinfo.cpp \
	unrar/extract.cpp unrar/volume.cpp unrar/list.cpp unrar/find.cpp unrar/unpack.cpp unrar/headers.cpp unrar/threadpool.cpp unrar/rs16.cpp \
	unrar/cmddata.cpp unrar/ui.cpp unrar/largepage.cpp

# Object list mirrors 'LIB_OBJ' from unrar/makefile (the libunrar target).
LIB_FILES = unrar/filestr.cpp unrar/scantree.cpp unrar/dll.cpp unrar/qopen.cpp

LOCAL_SRC_FILES := UnRar.cpp $(FILES) $(LIB_FILES)

# unrar throws (rar exceptions in errhnd.cpp, std::bad_alloc in unpack);
# ndk-build disables exceptions by default. RTTI is not used.
LOCAL_CPP_FEATURES := exceptions

# -D_POSIX_C_SOURCE=200809L selects unrar's utimensat()-based timestamp code
# (UNIX_TIME_NS). Its alternative, lutimes(), only exists in bionic from API 26,
# while this app supports API 23.
LOCAL_CFLAGS := -DSILENT -DRARDLL -DRAR_SMP -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_POSIX_C_SOURCE=200809L -I$(LOCAL_PATH)/unrar $(MY_CFLAGS)
LOCAL_LDLIBS := -llog -lz -pthread

include $(BUILD_SHARED_LIBRARY)

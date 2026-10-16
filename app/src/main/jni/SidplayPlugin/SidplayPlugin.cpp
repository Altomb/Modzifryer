#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <android/log.h>
#include "com_ssb_droidsound_plugins_SidplayPlugin.h"

#include <sidplay/SidTune.h>
#include <sidplay/sidplay2.h>
#include <sidplay/sidiptr.h>
#include <sidplay/sidbuilder.h>
#include <sidplay/builders/resid.h>


#define INFO_TITLE 0
#define INFO_AUTHOR 1
#define INFO_LENGTH 2
#define INFO_TYPE 3
#define INFO_COPYRIGHT 4
#define INFO_GAME 5
#define INFO_SUBTUNES 6
#define INFO_STARTTUNE 7

#define INFO_SILENCE 100
#define INFO_SIDMODEL 101

static jstring NewString(JNIEnv *env, const char *str)
{
	static jchar *temp, *ptr;

	temp = (jchar *) malloc((strlen(str) + 1) * sizeof(jchar));

	ptr = temp;
	while(*str) {
		unsigned char c = (unsigned char)*str++;
		*ptr++ = (c < 0x7f && c >= 0x20) || c >= 0xa0 || c == 0xa ? c : '?';
	}
	jstring j = env->NewString(temp, ptr - temp);

	free(temp);

	return j;
}

#define OPT_FILTER 1
#define OPT_RESAMPLING 2
#define OPT_NTSC 3
#define OPT_MODEL 4

struct Player
{
	Player(ISidUnknown *engine, ISidUnknown *builder)
		: sidemu(engine), sidbuilder(builder), sidemulation(builder) {}

	SidTune *sidtune;
	SidIPtr<ISidplay2> sidemu;
	SidIPtr<IReSIDBuilder> sidbuilder;
	ISidUnknown *sidemulation;
	SidTuneInfo sidInfo;
	sid2_config_t cfg;
	bool silent;
};

static bool filter_cfg = true;
static bool filter_set = false;
static int ntsc_cfg = false;
static bool ntsc_set = false;
static bool override_cfg = false;
static bool use_6581 = false;


JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1setOption(JNIEnv *env, jclass cl, jint what, jint val)
{
	__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "Setting %d to %d", what, val);
	switch(what) {
	case OPT_FILTER:
		filter_cfg = val ? true : false;
		filter_set = true;
		break;
	case OPT_NTSC:
		ntsc_cfg = val;
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "NTSC %d", ntsc_cfg);
		ntsc_set = true;
		break;
	case OPT_MODEL:
		break;
	}
}


JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1canHandle(JNIEnv *, jobject, jstring)
{
	return true;
}

JNIEXPORT jlong JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1load(JNIEnv *env, jobject obj, jbyteArray bArray, jint size)
{
	__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "in load()");

	jbyte *ptr = env->GetByteArrayElements(bArray, NULL);

	SidTune *tune = new SidTune((uint_least8_t*)ptr, size);

	if(!tune->getStatus()) {
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "FAILED!");
		delete tune;
		env->ReleaseByteArrayElements(bArray, ptr, 0);
		return 0;
	}

	ISidUnknown *engine = sidplay2::create();
	if (engine == NULL) {
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "sidplay2::create failed");
		delete tune;
		env->ReleaseByteArrayElements(bArray, ptr, 0);
		return 0;
	}

	ISidUnknown *builder = ReSIDBuilderCreate("ReSID");
	if (builder == NULL) {
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "ReSIDBuilderCreate failed");
		delete tune;
		env->ReleaseByteArrayElements(bArray, ptr, 0);
		return 0;
	}

	Player *player = new Player(engine, builder);
	player->sidtune = tune;

	tune->selectSong(0);
	tune->getInfo(player->sidInfo);

	SidTuneInfo &i = player->sidInfo;

	__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "INFO CL %x SP %x / I %x L %x NO %x LEN %d", i.clockSpeed, i.songSpeed, i.initAddr, i.loadAddr, i.songs, i.c64dataLen);

	sid2_config_t cfg = player->sidemu->config();
	cfg.clockForced  = false;

	cfg.clockSpeed   = (ntsc_cfg & 2) ? SID2_CLOCK_NTSC : SID2_CLOCK_CORRECT;
	cfg.clockDefault = (ntsc_cfg  & 1) ? SID2_CLOCK_NTSC : SID2_CLOCK_PAL;
	cfg.frequency    = 44100;
	cfg.playback     = sid2_stereo;
	cfg.precision    = 16;

	sid2_model_t model = use_6581 ? SID2_MOS6581 : SID2_MOS8580;
	cfg.sidModel     = override_cfg ? model : SID2_MODEL_CORRECT;
	cfg.sidDefault   = model;

	cfg.sidSamples    = true;
	cfg.environment   = sid2_envR;
	cfg.forceDualSids = false;
	cfg.emulateStereo = false;
	cfg.optimisation  = SID2_MAX_OPTIMISATION;

	int rc = player->sidemu->load(player->sidtune);

	__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "LOAD %d", rc);

	if(rc < 0) {
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "LOAD FAILED: %s", player->sidemu->error());
		delete player;
		env->ReleaseByteArrayElements(bArray, ptr, 0);
		return 0;
	}

	player->sidbuilder->create(1);
	player->sidbuilder->filter(filter_cfg);
	player->sidbuilder->sampling(44100);
	cfg.sidEmulation = player->sidemulation;

	rc = player->sidemu->config(cfg);
	if(rc < 0) {
		__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "CONFIG FAILED: %s", player->sidemu->error());
		delete player;
		env->ReleaseByteArrayElements(bArray, ptr, 0);
		return 0;
	}

	player->cfg = cfg;

	__android_log_print(ANDROID_LOG_VERBOSE, "SidplayPlugin", "load sid done");

	env->ReleaseByteArrayElements(bArray, ptr, 0);
	return (long)player;
}

JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1unload(JNIEnv *, jobject, jlong song)
{
	Player *player = (Player*)song;
	if (player->sidtune)
		delete player->sidtune;
	delete player;
}

JNIEXPORT jint JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1getSoundData(JNIEnv *env, jobject obj, jlong song, jshortArray sArray, jint size)
{
	Player *player = (Player*)song;

	jshort *ptr = env->GetShortArrayElements(sArray, NULL);
	int orgsize = size;

	if(filter_set) {
		player->sidbuilder->filter(filter_cfg);
		filter_set = false;
	}

	player->sidemu->play(ptr, size*2);

	env->ReleaseShortArrayElements(sArray, ptr, 0);
	return orgsize;
}

JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1seekTo(JNIEnv *, jobject, jlong, jint)
{
	return false;
}

JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1setTune(JNIEnv *env, jobject obj, jlong song, jint tune)
{
	Player *player = (Player*)song;

	player->sidtune->selectSong(tune+1);
	if(player->sidemu->load(player->sidtune) < 0)
		return false;
	player->sidemu->config(player->cfg);
	return true;
}

JNIEXPORT jstring JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1getStringInfo(JNIEnv *env, jobject obj, jlong song, jint what)
{
	Player *player = (Player*)song;
	switch(what)
	{
	case INFO_TITLE:
		return NewString(env, player->sidInfo.infoString[0]);
	case INFO_AUTHOR:
		return NewString(env, player->sidInfo.infoString[1]);
	case INFO_COPYRIGHT:
		return NewString(env, player->sidInfo.infoString[2]);
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_com_ssb_droidsound_plugins_SidplayPlugin_N_1getIntInfo(JNIEnv *env, jobject obj, jlong song, jint what)
{
	Player *player = (Player*)song;
	switch(what)
	{
	case INFO_LENGTH:
		return -1;
	case INFO_SUBTUNES:
		return player->sidInfo.songs;
	case INFO_STARTTUNE:
		return player->sidInfo.startSong-1;
	case INFO_SIDMODEL:
		return player->sidInfo.sidModel1;

	case INFO_SILENCE:
		return 0;
	}
	return -1;
}

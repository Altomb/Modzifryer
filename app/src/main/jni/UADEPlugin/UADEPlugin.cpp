#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>
#include <signal.h>

#include <android/log.h>
#include "com_ssb_droidsound_plugins_UADEPlugin.h"

extern "C" {
#include <uade/uade.h>
#include <uade/uadestate.h>
#include <uade/effects.h>
}

#define TAG "UADEPlugin"

#define INFO_TITLE 0
#define INFO_AUTHOR 1
#define INFO_LENGTH 2
#define INFO_TYPE 3
#define INFO_COPYRIGHT 4
#define INFO_GAME 5
#define INFO_SUBTUNES 6
#define INFO_STARTTUNE 7

#define OPT_FILTER 1
#define OPT_RESAMPLING 2
#define OPT_NTSC 3
#define OPT_SPEEDHACK 4
#define OPT_PANNING 5

static struct uade_state *g_state = NULL;

static long g_current_song = 0;

static int g_min_subsong = 0;
static int g_max_subsong = 0;
static int g_def_subsong = 0;
static char g_format[256] = "";

#define MAX_BASEDIR 512
static char g_basedir[MAX_BASEDIR] = "";

static int g_filter_on = 1;
static int g_ntsc = 0;
static int g_speedhack = 0;
static int g_panning_enabled = 0;
static double g_panning = 0.5;

static void logmsg(const char *fmt, ...)
{
	va_list ap;
	va_start(ap, fmt);
	__android_log_vprint(ANDROID_LOG_VERBOSE, TAG, fmt, ap);
	va_end(ap);
}

static jstring NewString(JNIEnv *env, const char *str)
{
	static jchar *temp, *ptr;

	temp = (jchar *) malloc((strlen(str) + 1) * sizeof(jchar));

	ptr = temp;
	while (*str) {
		unsigned char c = (unsigned char) *str++;
		*ptr++ = (c < 0x7f && c >= 0x20) || c >= 0xa0 || c == 0xa ? c : '?';
	}
	jstring j = env->NewString(temp, ptr - temp);

	free(temp);

	return j;
}

/* Apply the current user options to the state's extra config so that they are
 * picked up the next time a song is prepared. */
static void apply_options(void)
{
	if (g_state == NULL)
		return;

	struct uade_config *uc = &g_state->extraconfig;

	uade_config_set_option(uc, g_ntsc ? UC_NTSC : UC_PAL, NULL);

	if (g_speedhack)
		uade_config_set_option(uc, UC_SPEED_HACK, NULL);

	if (g_panning_enabled) {
		char buf[32];
		snprintf(buf, sizeof(buf), "%f", g_panning);
		uade_config_set_option(uc, UC_PANNING_VALUE, buf);
	} else {
		uade_config_set_option(uc, UC_NO_PANNING, NULL);
	}
}

JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1init(JNIEnv *env, jobject obj, jstring basedir)
{
	jboolean iscopy;
	const char *s = env->GetStringUTFChars(basedir, &iscopy);
	strncpy(g_basedir, s, MAX_BASEDIR - 1);
	g_basedir[MAX_BASEDIR - 1] = 0;
	env->ReleaseStringUTFChars(basedir, s);

	logmsg("N_init baseDir '%s'", g_basedir);

	signal(SIGPIPE, SIG_IGN);

	if (g_state != NULL)
		return;

	struct uade_config *uc = uade_new_config();
	if (uc == NULL) {
		logmsg("uade_new_config failed");
		return;
	}

	uade_config_set_option(uc, UC_BASE_DIR, g_basedir);
	uade_config_set_option(uc, UC_FREQUENCY, "44100");

	g_state = uade_new_state(uc);
	free(uc);

	if (g_state == NULL) {
		logmsg("uade_new_state failed");
		return;
	}

	apply_options();
}

JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1exit(JNIEnv *env, jobject obj)
{
	if (g_state != NULL) {
		uade_cleanup_state(g_state);
		g_state = NULL;
	}
	g_current_song = 0;
}

JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1setOption(JNIEnv *env, jclass cl, jint what, jint val)
{
	switch (what) {
	case OPT_FILTER:
		g_filter_on = val ? 1 : 0;
		if (g_state != NULL && g_current_song != 0)
			uade_set_filter_state(g_state, g_filter_on);
		break;
	case OPT_NTSC:
		g_ntsc = val ? 1 : 0;
		apply_options();
		break;
	case OPT_SPEEDHACK:
		g_speedhack = val ? 1 : 0;
		apply_options();
		break;
	case OPT_PANNING:
		if (val >= 4) {
			g_panning_enabled = 0;
		} else {
			g_panning = 0.25 * (4 - val);
			g_panning_enabled = 1;
		}
		apply_options();
		if (g_state != NULL && g_current_song != 0) {
			if (g_panning_enabled) {
				uade_effect_pan_set_amount(g_state, g_panning);
				uade_effect_enable(g_state, UADE_EFFECT_PAN);
			} else {
				uade_effect_disable(g_state, UADE_EFFECT_PAN);
			}
		}
		break;
	default:
		break;
	}
}

JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1canHandle(JNIEnv *env, jobject obj, jstring name)
{
	return true;
}

JNIEXPORT jlong JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1load(JNIEnv *env, jobject obj, jbyteArray module, jint size)
{
	return 0;
}

JNIEXPORT jlong JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1loadFile(JNIEnv *env, jobject obj, jstring fname)
{
	jboolean iscopy;
	const char *filename = env->GetStringUTFChars(fname, &iscopy);

	logmsg("loadFile '%s'", filename);

	if (g_state == NULL) {
		env->ReleaseStringUTFChars(fname, filename);
		return 0;
	}

	if (g_current_song != 0) {
		uade_stop(g_state);
		g_current_song = 0;
	}

	apply_options();

	int rc = uade_play(filename, -1, g_state);
	if (rc != 1) {
		logmsg("uade_play('%s') failed: %d", filename, rc);
		env->ReleaseStringUTFChars(fname, filename);
		return 0;
	}

	const struct uade_song_info *info = uade_get_song_info(g_state);
	if (info != NULL) {
		g_min_subsong = info->subsongs.min;
		g_max_subsong = info->subsongs.max;
		g_def_subsong = info->subsongs.def;
		if (info->formatname[0] != 0)
			strncpy(g_format, info->formatname, sizeof(g_format) - 1);
		else
			strncpy(g_format, info->playername, sizeof(g_format) - 1);
		g_format[sizeof(g_format) - 1] = 0;
		logmsg("playing with %s (%d..%d subsongs)", g_format,
		       g_min_subsong, g_max_subsong);
	} else {
		g_min_subsong = g_max_subsong = g_def_subsong = 0;
		g_format[0] = 0;
	}

	uade_set_filter_state(g_state, g_filter_on);

	g_current_song = 1;

	env->ReleaseStringUTFChars(fname, filename);
	return 1;
}

JNIEXPORT void JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1unload(JNIEnv *env, jobject obj, jlong song)
{
	if (g_state != NULL && g_current_song != 0)
		uade_stop(g_state);
	g_current_song = 0;
}

JNIEXPORT jint JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1getSoundData(JNIEnv *env, jobject obj, jlong song, jshortArray sArray, jint size)
{
	if (g_state == NULL || g_current_song == 0)
		return -1;

	if (size <= 0)
		return 0;

	jshort *dest = env->GetShortArrayElements(sArray, NULL);
	if (dest == NULL)
		return -1;

	ssize_t bytes = uade_read(dest, (size_t) size * 2, g_state);

	env->ReleaseShortArrayElements(sArray, dest, 0);

	if (bytes <= 0)
		return -1;

	return (jint)(bytes / 2);
}

JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1seekTo(JNIEnv *env, jobject obj, jlong song, jint msec)
{
	if (g_state == NULL || g_current_song == 0)
		return false;

	if (uade_seek(UADE_SEEK_SONG_RELATIVE, msec / 1000.0, 0, g_state) != 0)
		return false;

	return true;
}

JNIEXPORT jboolean JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1setTune(JNIEnv *env, jobject obj, jlong song, jint tune)
{
	if (g_state == NULL || g_current_song == 0)
		return false;

	if (uade_seek(UADE_SEEK_SUBSONG_RELATIVE, 0.0, g_min_subsong + tune, g_state) != 0)
		return false;

	return true;
}

JNIEXPORT jstring JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1getStringInfo(JNIEnv *env, jobject obj, jlong song, jint what)
{
	if (what == INFO_TYPE)
		return NewString(env, g_format);
	return NewString(env, "");
}

JNIEXPORT jint JNICALL Java_com_ssb_droidsound_plugins_UADEPlugin_N_1getIntInfo(JNIEnv *env, jobject obj, jlong song, jint what)
{
	if (g_state == NULL || g_current_song == 0)
		return -1;

	switch (what) {
	case INFO_LENGTH:
		return -1;
	case INFO_SUBTUNES:
		return g_max_subsong - g_min_subsong + 1;
	case INFO_STARTTUNE:
		return g_def_subsong - g_min_subsong;
	}
	return -1;
}

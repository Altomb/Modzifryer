/*
 * Android in-process uadecore launcher.
 *
 * Upstream libuade starts uadecore as a separate process using fork()+execlp().
 * Android applications can not execute a helper binary shipped inside the APK,
 * so instead the uadecore is compiled into this same shared library and its
 * main loop is run in a dedicated thread.  A socketpair provides the exact same
 * full-duplex control channel that the separate process would use.
 */

#ifdef __ANDROID__

#include <errno.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include <uade/uadeipc.h>
#include <uade/unixatomic.h>

extern int uadecore_main(int argc, char **argv);

struct uadecore_thread {
	pthread_t tid;
	char inarg[32];
	char outarg[32];
	char *argv[6];
};

static struct uadecore_thread *g_core = NULL;

static void *uadecore_thread_main(void *arg)
{
	struct uadecore_thread *t = arg;
	uadecore_main(5, t->argv);
	return NULL;
}

/*
 * Core source files are compiled with -Dexit=uadecore_terminate so that an
 * exit() inside the emulator only terminates the emulator thread instead of
 * the whole application.
 */
void uadecore_terminate(int code) __attribute__((noreturn));

void uadecore_terminate(int code)
{
	if (g_core != NULL && pthread_equal(pthread_self(), g_core->tid))
		pthread_exit((void *)(intptr_t) code);
	exit(code);
}

int uade_arch_spawn(struct uade_ipc *ipc, pid_t *uadepid, const char *uadename,
		    const int *keep_fds)
{
	int fds[2];
	struct uadecore_thread *t;

	(void) uadename;
	(void) keep_fds;

	if (socketpair(AF_UNIX, SOCK_STREAM, 0, fds)) {
		fprintf(stderr, "uade: Can not create socketpair: %s\n",
			strerror(errno));
		return -1;
	}

	t = calloc(1, sizeof *t);
	if (t == NULL) {
		uade_atomic_close(fds[0]);
		uade_atomic_close(fds[1]);
		return -1;
	}

	snprintf(t->inarg, sizeof t->inarg, "%d", fds[1]);
	snprintf(t->outarg, sizeof t->outarg, "%d", fds[1]);
	t->argv[0] = "uadecore";
	t->argv[1] = "-i";
	t->argv[2] = t->inarg;
	t->argv[3] = "-o";
	t->argv[4] = t->outarg;
	t->argv[5] = NULL;

	g_core = t;
	if (pthread_create(&t->tid, NULL, uadecore_thread_main, t) != 0) {
		g_core = NULL;
		free(t);
		uade_atomic_close(fds[0]);
		uade_atomic_close(fds[1]);
		return -1;
	}

	/* Only a non-zero marker is needed; the thread is tracked via g_core. */
	*uadepid = (pid_t) 1;
	uade_set_peer(ipc, 1, fds[0], fds[0]);
	return 0;
}

void uade_arch_kill_and_wait_uadecore(struct uade_ipc *ipc, pid_t *uadepid)
{
	if (*uadepid == 0)
		return;

	if (ipc->in_fd >= 0) {
		uade_atomic_close(ipc->in_fd);
		if (ipc->out_fd == ipc->in_fd)
			ipc->out_fd = -1;
		ipc->in_fd = -1;
	}
	if (ipc->out_fd >= 0) {
		uade_atomic_close(ipc->out_fd);
		ipc->out_fd = -1;
	}

	if (g_core != NULL) {
		pthread_join(g_core->tid, NULL);
		free(g_core);
		g_core = NULL;
	}

	*uadepid = 0;
}

#endif

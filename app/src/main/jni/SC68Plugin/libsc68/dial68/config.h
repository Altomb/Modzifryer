/* config.h.  Hand-written for the Android/NDK build of libsc68 3.0.0.
 * Mirrors what libsc68/configure would generate on a POSIX (bionic) host.
 */
#ifndef SC68_ANDROID_CONFIG_H
#define SC68_ANDROID_CONFIG_H

/* debug facilities */
/* #undef DEBUG */

/* debug facilities for libsc68 */
/* #undef DEBUG_LIBSC68 */

/* Define to 1 if you have the <assert.h> header file. */
#define HAVE_ASSERT_H 1

/* Support __declspec() */
/* #undef HAVE_DECLSPEC */

/* Define to 1 if you have the <ctype.h> header file. */
#define HAVE_CTYPE_H 1

/* Define to 1 if you have the <errno.h> header file. */
#define HAVE_ERRNO_H 1

/* Define to 1 if you have the `basename' function. */
#define HAVE_BASENAME 1

/* Define to 1 if you have the `free' function. */
#define HAVE_FREE 1

/* Define to 1 if you have the `getenv' function. */
#define HAVE_GETENV 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to 1 if you have the <libgen.h> header file. */
#define HAVE_LIBGEN_H 1

/* Define to 1 if you have the <limits.h> header file. */
#define HAVE_LIMITS_H 1

/* Define to 1 if the system has the type `long long int'. */
#define HAVE_LONG_LONG_INT 1

/* Define to 1 if you have the `malloc' function. */
#define HAVE_MALLOC 1

/* Define to 1 if you have the <stdarg.h> header file. */
#define HAVE_STDARG_H 1

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdio.h> header file. */
#define HAVE_STDIO_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the `stpcpy' function. */
#define HAVE_STPCPY 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the `strtol' function. */
#define HAVE_STRTOL 1

/* Define to 1 if you have the `strtoul' function. */
#define HAVE_STRTOUL 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if the system has the type `unsigned long long int'. */
#define HAVE_UNSIGNED_LONG_LONG_INT 1

/* Define to 1 if you have the `vsnprintf' function. */
#define HAVE_VSNPRINTF 1

/* Define to 1 if you have the `vsprintf' function. */
#define HAVE_VSPRINTF 1

/* Define to 1 if assertions should be disabled. */
#define NDEBUG 1

/* suppress all debug facilities for libsc68 */
#define NDEBUG_LIBSC68 1

/* Name of package */
#define PACKAGE "libsc68"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT "benjihan -4t- sourceforge"

/* Package short description */
#define PACKAGE_DESC "sc68 engine library. It is part of the sc68 project. Visit <http://sc68.atari.org>"

/* Define to the full name of this package. */
#define PACKAGE_NAME "libsc68"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "libsc68 3.0.0"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "libsc68"

/* Define to the home page for this package. */
#define PACKAGE_URL "http://sc68.atari.org"

/* Version number */
#define PACKAGE_VERNUM 300

/* Define to the version of this package. */
#define PACKAGE_VERSION "3.0.0"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Using file68 library */
#define USE_FILE68 1

/* Enable extensions that the engine expects. */
#ifndef _GNU_SOURCE
# define _GNU_SOURCE 1
#endif

/* Version number of package */
#define VERSION "3.0.0"

/* set default YM engine */
/* #undef YM_ENGINE */

/* Define to the equivalent of the C99 'restrict' keyword. */
#ifndef restrict
# define restrict __restrict
#endif

#endif /* SC68_ANDROID_CONFIG_H */

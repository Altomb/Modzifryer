/* config.h.  Hand-written for the Android/NDK build of file68 3.0.0.
 * Mirrors what file68/configure would generate; ICE! + zlib enabled,
 * libao/cURL/registry disabled.
 */
#ifndef FILE68_ANDROID_CONFIG_H
#define FILE68_ANDROID_CONFIG_H

/* debug facilities */
/* #undef DEBUG */

/* debug facilities for file68 */
/* #undef DEBUG_FILE68 */

/* Defined if file68 supports audio (via libao) */
/* #undef FILE68_AO */

/* Defined if file68 supports remote files (via libcurl) */
/* #undef FILE68_CURL */

/* Defined if file68 supports !ice depacker (via unice68) */
#define FILE68_UNICE68 1

/* Defined if file68 supports deflate (via zlib) */
#define FILE68_Z 1

/* Define to 1 if you have the <assert.h> header file. */
#define HAVE_ASSERT_H 1

/* Define to 1 if you have the <ctype.h> header file. */
#define HAVE_CTYPE_H 1

/* Define to 1 if you have the <errno.h> header file. */
#define HAVE_ERRNO_H 1

/* Define to 1 if you have the `free' function. */
#define HAVE_FREE 1

/* Define to 1 if you have the <fcntl.h> header file. */
#define HAVE_FCNTL_H 1

/* Define to 1 if you have the `getenv' function. */
#define HAVE_GETENV 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to 1 if you have the <limits.h> header file. */
#define HAVE_LIMITS_H 1

/* Define to 1 if you have the `malloc' function. */
#define HAVE_MALLOC 1

/* Define to 1 if you have the `sleep' function. */
#define HAVE_SLEEP 1

/* Define to 1 if you have the <stdarg.h> header file. */
#define HAVE_STDARG_H 1

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdio.h> header file. */
#define HAVE_STDIO_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if you have the `usleep' function. */
#define HAVE_USLEEP 1

/* Define to 1 if you have the `vsnprintf' function. */
#define HAVE_VSNPRINTF 1

/* Define to 1 if you have the `vsprintf' function. */
#define HAVE_VSPRINTF 1

/* Disable file descriptor stream support */
/* #undef ISTREAM68_NO_FD */

/* Disable FILE* stream support */
/* #undef ISTREAM68_NO_FILE */

/* Disable memory stream support */
/* #undef ISTREAM68_NO_MEM */

/* Define to 1 if assertions should be disabled. */
#define NDEBUG 1

/* suppress all debug facilities for file68 */
#define NDEBUG_FILE68 1

/* Name of package */
#define PACKAGE "libfile68"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT "benjihan -4t- sourceforge"

/* Package short description */
#define PACKAGE_DESC "sc68 file and utility library. It is part of the sc68 project. Visit <http://sc68.atari.org>"

/* Define to the full name of this package. */
#define PACKAGE_NAME "file68"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "file68 3.0.0"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "libfile68"

/* Define to the home page for this package. */
#define PACKAGE_URL "http://sc68.atari.org"

/* Version number */
#define PACKAGE_VERNUM 300

/* Define to the version of this package. */
#define PACKAGE_VERSION "3.0.0"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Using Windows registry */
/* #undef USE_REGISTRY68 */

/* Enable GNU extensions. */
#ifndef _GNU_SOURCE
# define _GNU_SOURCE 1
#endif

/* Using unice68 library */
#define USE_UNICE68 1

/* Using z library */
#define USE_Z 1

/* Version number of package */
#define VERSION "3.0.0"

/* Define to the equivalent of the C99 'restrict' keyword. */
#ifndef restrict
# define restrict __restrict
#endif

#endif /* FILE68_ANDROID_CONFIG_H */

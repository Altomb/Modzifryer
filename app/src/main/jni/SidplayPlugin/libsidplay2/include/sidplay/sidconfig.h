#ifndef _sidconfig_h_
#define _sidconfig_h_

/* Define the sizeof of types in bytes */
#define SID_SIZEOF_CHAR      1
#define SID_SIZEOF_SHORT_INT 2
#define SID_SIZEOF_INT       4
#define SID_SIZEOF_LONG_INT  8

/* Android/Linux LP64 is little endian. */
#define SID_WORDS_LITTLEENDIAN

#define SID_HAVE_BOOL
#define SID_HAVE_STDBOOL_H

/* DLL building support on win32 hosts */
#ifndef SID_EXTERN
#   define SID_EXTERN
#endif

/* Namespace support */
#define SIDPLAY2_NAMESPACE __sidplay2__
#ifdef  SIDPLAY2_NAMESPACE
#   define SIDPLAY2_NAMESPACE_START \
    namespace SIDPLAY2_NAMESPACE    \
    {
#   define SIDPLAY2_NAMESPACE_STOP  \
    }
#else
#   define SIDPLAY2_NAMESPACE_START
#   define SIDPLAY2_NAMESPACE_STOP
#endif

#endif /* _sidconfig_h_ */

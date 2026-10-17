#include "archdep.h"
#include "joyport/joystick.h"
#include "info.h"
#include "machine.h"
#include <stdbool.h>

void archdep_network_shutdown(void) {}
void joystick_arch_init(void) {}
void joystick_ui_event(void *input, joystick_input_t type, int32_t value, bool major_change) {}

/* info.c is not compiled (needs generated infocontrib.h) */
vice_team_t core_team[] = { { NULL, NULL, NULL, NULL } };
vice_team_t ex_team[] = { { NULL, NULL, NULL, NULL } };

/* vice/main.c is not compiled (plugin has its own init sequence) */
bool default_settings_requested = false;
void main_exit(void)
{
    machine_shutdown();
}

/* vicii-clock-stretch.c references these (only used by c128) */
int maincpu_stretch = 0;
CLOCK c128cpu_memory_refresh_clk;

/* vice/main.c is not compiled; it owns these globals */
bool console_mode = true;
bool video_disabled_mode = true;

/* c64model.c model hooks for drive/iec/tape/userport hardware (unused in VSID) */
void c64_cia2_enable(int val) {}
void c64iec_enable(int val) {}
void tapeport_enable(int val) {}
void userport_enable(int val) {}
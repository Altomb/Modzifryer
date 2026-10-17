#!/usr/bin/env python3
import re

src = open("vice/maincpu.c").read()

out = src

# Replace the mainloop function header with psid_play
out = out.replace(
    "/* This doesn't return. The thread will directly exit when requested. */\n"
    "void maincpu_mainloop(void)\n",
    "short *psid_sound_buf;\n"
    "int psid_sound_idx;\n"
    "int psid_sound_max;\n"
    "\n"
    "void psid_play(short *buf, int n)\n"
    "{\n"
    "    psid_sound_buf = buf;\n"
    "    psid_sound_idx = 0;\n"
    "    psid_sound_max = n;\n",
)

# Make the register/file-scope vars persistent across psid_play() calls
out = out.replace("    uint8_t reg_a = 0;", "    static uint8_t reg_a = 0;")
out = out.replace("    uint8_t reg_x = 0;", "    static uint8_t reg_x = 0;")
out = out.replace("    uint8_t reg_y = 0;", "    static uint8_t reg_y = 0;")
out = out.replace("    int reg_a_read_idx = 0;", "    static int reg_a_read_idx = 0;")
out = out.replace("    int reg_a_write_idx = 0;", "    static int reg_a_write_idx = 0;")
out = out.replace("    int reg_x_idx = 2;", "    static int reg_x_idx = 2;")
out = out.replace("    int reg_y_idx = 1;", "    static int reg_y_idx = 1;")
out = out.replace("    uint8_t reg_p = 0;", "    static uint8_t reg_p = 0;")
out = out.replace("    uint8_t reg_sp = 0;", "    static uint8_t reg_sp = 0;")
out = out.replace("    uint8_t flag_n = 0;", "    static uint8_t flag_n = 0;")
out = out.replace("    uint8_t flag_z = 0;", "    static uint8_t flag_z = 0;")
out = out.replace("#ifndef NEED_REG_PC\n    unsigned int reg_pc;", "#ifndef NEED_REG_PC\n    static unsigned int reg_pc;")

# Never reset inside psid_play; the JNI wrapper resets on song change
out = out.replace(
    "    machine_trigger_reset(MACHINE_RESET_MODE_RESET_CPU);\n\n", "")

# Bound the loop by our sound buffer
out = out.replace("    while (1) {", "    while (psid_sound_idx != psid_sound_max) {")

# Drop drive / autostart subsystems (not used in PSID playback)
out = out.replace("        drive_cycle_hook();\n", "")
out = out.replace("        autostart_advance();\n", "")

open("overrides/maincpu.c", "w").write(out)
print("wrote overrides/maincpu.c")
#include "vice.h"
#include "video.h"
#include "lib.h"
#include "videoarch.h"
#include "viewport.h"

void video_render_setrawrgb(video_render_color_tables_t *color_tab, unsigned int index, uint32_t r, uint32_t g, uint32_t b) {}

struct video_canvas_s *video_canvas_init(void)
{
    video_canvas_t *canvas;

    canvas = lib_calloc(1, sizeof(video_canvas_t));
    if (canvas == NULL) {
        return NULL;
    }
    canvas->videoconfig = lib_calloc(1, sizeof(video_render_config_t));
    if (canvas->videoconfig == NULL) {
        lib_free(canvas);
        return NULL;
    }
    canvas->draw_buffer = lib_calloc(1, sizeof(draw_buffer_t));
    if (canvas->draw_buffer == NULL) {
        lib_free(canvas->videoconfig);
        lib_free(canvas);
        return NULL;
    }
    canvas->viewport = lib_calloc(1, sizeof(viewport_t));
    if (canvas->viewport == NULL) {
        lib_free(canvas->draw_buffer);
        lib_free(canvas->videoconfig);
        lib_free(canvas);
        return NULL;
    }
    canvas->geometry = lib_calloc(1, sizeof(geometry_t));
    if (canvas->geometry == NULL) {
        lib_free(canvas->viewport);
        lib_free(canvas->draw_buffer);
        lib_free(canvas->videoconfig);
        lib_free(canvas);
        return NULL;
    }
    video_arch_canvas_init(canvas);
    return canvas;
}

void video_canvas_create_set(struct video_canvas_s *canvas) {}
void video_canvas_map(struct video_canvas_s *canvas) {}
void video_canvas_unmap(struct video_canvas_s *canvas) {}
void video_canvas_refresh_all(struct video_canvas_s *canvas) {}
void video_canvas_redraw_size(struct video_canvas_s *canvas, unsigned int width, unsigned int height) {}

void video_canvas_refresh_all_tracked(void)
{
}

void video_color_palette_free(struct palette_s *palette)
{
    if (palette != NULL) {
        lib_free(palette);
    }
}
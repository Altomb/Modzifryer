package com.ssb.droidsound.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Window insets handling for edge-to-edge windows.
 *
 * From targetSdk 35 onwards Android draws every activity edge to edge, and the
 * legacy opt-out is no longer honoured, so system bars and display cutouts
 * overlap app content unless the app pads for them itself. Before the SDK bump
 * the framework did this automatically, which is why the pre-Android-15 layouts
 * have no inset handling at all.
 *
 * Padding is applied on top of whatever the layout already declared, so
 * layouts keep their own spacing.
 */
public final class SystemBars {

	private SystemBars() {
	}

	/**
	 * Pad {@code view} by the system bar and display cutout insets. Safe to
	 * call before the view is attached; insets are applied when they arrive.
	 */
	public static void padForSystemBars(final View view) {
		final int left = view.getPaddingLeft();
		final int top = view.getPaddingTop();
		final int right = view.getPaddingRight();
		final int bottom = view.getPaddingBottom();

		ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
			@Override
			public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat windowInsets) {
				Insets bars = windowInsets.getInsets(
						WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
				v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
				return windowInsets;
			}
		});

		ViewCompat.requestApplyInsets(view);
	}
}

package com.matburt.mobileorg.ng.ui.theme;

import android.graphics.Color;
import android.util.Log;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;

public class DefaultTheme {

	private static final String TAG = "Theme";
	public int c0Black = Color.rgb(0x00, 0x00, 0x00);
	public int c1Red = Color.rgb(0xd0, 0x00, 0x00);
	public int c2Green = Color.rgb(0x00, 0xa0, 0x00);
	public int c3Yellow = Color.rgb(0xc0, 0x80, 0x00);
	public int c4Blue = Color.rgb(0x22, 0x22, 0xf0);
	public int c5Purple = Color.rgb(0xa0, 0x00, 0xa0);
	public int c6Cyan = Color.rgb(0x00, 0x80, 0x80);
	public int c7White = Color.rgb(0xc0, 0xc0, 0xc0);

	public int c9LRed = Color.rgb(0xff, 0x77, 0x77);
	public int caLGreen = Color.rgb(0x77, 0xff, 0x77);
	public int cbLYellow = Color.rgb(0xff, 0xff, 0x00);
	public int ccLBlue = Color.rgb(0x88, 0x88, 0xff);
	public int cdLPurple = Color.rgb(0xff, 0x00, 0xff);
	public int ceLCyan = Color.rgb(0x00, 0xff, 0xff);
	public int cfLWhite = Color.rgb(0xff, 0xff, 0xff);

	public static DefaultTheme loadTheme() {
		String theme = App.getInstance().getStringPreference(R.string.theme,
				R.string.themeDefault);
		Log.i(TAG, "Use theme: " + theme);
		if ("white".equals(theme)) {
			return new WhiteTheme();
		}
		if ("mono".equals(theme)) {
			return new MonoTheme();
		}
		return new DefaultTheme();
	}
}

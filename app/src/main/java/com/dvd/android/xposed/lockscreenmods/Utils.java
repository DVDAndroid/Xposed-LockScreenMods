/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dvd.android.xposed.lockscreenmods;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static final String PREF_CAT_KEY_LOCKSCREEN_SHORTCUTS = "pref_cat_lockscreen_shortcuts";
    public static final List<String> PREF_KEY_LOCKSCREEN_SHORTCUT = new ArrayList<>(
            Arrays.asList("pref_lockscreen_shortcut0",
                    "pref_lockscreen_shortcut1", "pref_lockscreen_shortcut2",
                    "pref_lockscreen_shortcut3", "pref_lockscreen_shortcut4",
                    "pref_lockscreen_shortcut5"));
    public static final String ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED = "dvd.intent.action.LOCKSCREEN_SHORTCUT_SETTING_CHANGED";
    public static final String EXTRA_LS_SHORTCUT_SLOT = "lockscreenShortcutSlot";
    public static final String EXTRA_LS_SHORTCUT_VALUE = "lockscreenShortcutValue";
    public static final String EXTRA_LS_SAFE_LAUNCH = "lockscreenShortcutSafeLaunch";
    public static final String PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH = "pref_lockscreen_shortcuts_safe_launch";
    public static final String PREF_HIDE_ICON = "hide_icon";
    public static final String PREF_SIZE_ICON = "icon_size";
    public static final String PREF_ANIMATIONS_ENABLED = "animations_enabled";
    public static final String PREF_LONG_CLICK = "long_click";
    public static final String ACTION_SLEEP = "dvd.action.SLEEP";
    public static final String EXTRA_WAKE_ON = "dvd.action.WAKE_ON";
    public static final String PREF_D2TS = "double_tap_2_sleep";
    public static final String ACTION_LOCKSCREEN_SETTINGS_CHANGED = "dvd.intent.action.LOCKSCREEN_SETTINGS_CHANGED";

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null)
            return null;

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}

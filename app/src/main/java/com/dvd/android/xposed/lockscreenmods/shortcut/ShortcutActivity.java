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
package com.dvd.android.xposed.lockscreenmods.shortcut;

import android.content.Intent;

public class ShortcutActivity {

    public static final String ACTION_LAUNCH_ACTION = "dvd.intent.action.LAUNCH_ACTION";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ACTION_TYPE = "actionType";
    public static final String EXTRA_ALLOW_UNLOCK_ACTION = "allowUnlockAction";

    public static boolean isGbBroadcastShortcut(Intent intent) {
        return (intent != null && intent.getAction() != null
                && intent.getAction().equals(ShortcutActivity.ACTION_LAUNCH_ACTION)
                && intent.hasExtra(ShortcutActivity.EXTRA_ACTION_TYPE)
                && intent.getStringExtra(ShortcutActivity.EXTRA_ACTION_TYPE).equals("broadcast"));
    }
}

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_LOCKSCREEN_SETTINGS_CHANGED;
import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED;
import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SAFE_LAUNCH;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SHORTCUT_SLOT;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SHORTCUT_VALUE;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_WAKE_ON;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_ANIMATIONS_ENABLED;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_D2TS;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_LONG_CLICK;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_SIZE_ICON;

public class ModLockscreen {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_KGVIEW_MEDIATOR = "com.android.systemui.keyguard.KeyguardViewMediator";
    private static final String CLASS_NOTIF_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static LockscreenAppBar mAppBar;
    private static Context mContext;
    private static Context mGbContext;
    private static XSharedPreferences mPrefs;
    private static GestureDetector mGestureDetector;
    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED:
                    if (mAppBar != null) {
                        if (intent.hasExtra(EXTRA_LS_SHORTCUT_SLOT)) {
                            mAppBar.updateAppSlot(intent.getIntExtra(EXTRA_LS_SHORTCUT_SLOT, 0), intent.getStringExtra(EXTRA_LS_SHORTCUT_VALUE));
                        }
                        if (intent.hasExtra(EXTRA_LS_SAFE_LAUNCH)) {
                            mAppBar.setSafeLaunchEnabled(intent.getBooleanExtra(EXTRA_LS_SAFE_LAUNCH, false));
                        }
                        if (intent.hasExtra(PREF_SIZE_ICON)) {
                            mAppBar.setSize(intent.getIntExtra(PREF_SIZE_ICON, 40));
                        }
                        if (intent.hasExtra(PREF_ANIMATIONS_ENABLED)) {
                            mAppBar.setAnimations(intent.getBooleanExtra(PREF_ANIMATIONS_ENABLED, true));
                        }
                        if (intent.hasExtra(PREF_LONG_CLICK)) {
                            mAppBar.setLongClick(intent.getBooleanExtra(PREF_LONG_CLICK, false));
                        }
                    }
                    break;
                case ACTION_SLEEP:
                    sleep(intent.getBooleanExtra(EXTRA_WAKE_ON, true));
                    break;
                case ACTION_LOCKSCREEN_SETTINGS_CHANGED:
                    mPrefs.reload();
                    break;
            }
        }
    };

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            final Class<?> kgViewMediatorClass = XposedHelpers.findClass(CLASS_KGVIEW_MEDIATOR, classLoader);

            String setupMethodName = Build.VERSION.SDK_INT >= 22 ? "setupLocked" : "setup";
            XposedHelpers.findAndHookMethod(kgViewMediatorClass, setupMethodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(
                        final MethodHookParam param) throws Throwable {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mGbContext = mContext.createPackageContext(XposedMod.PACKAGE_NAME, 0);

                    prepareGestureDetector();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                    intentFilter.addAction(ACTION_LOCKSCREEN_SETTINGS_CHANGED);
                    intentFilter.addAction(ACTION_SLEEP);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUSBAR, classLoader, "makeStatusBarView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ViewGroup kgStatusView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mKeyguardStatusView");
                    int containerId = kgStatusView.getResources().getIdentifier("keyguard_clock_container", "id", PACKAGE_NAME);
                    if (containerId != 0) {
                        ViewGroup container = (ViewGroup) kgStatusView.findViewById(containerId);
                        if (container != null) {
                            mAppBar = new LockscreenAppBar(mContext, mGbContext, container, param.thisObject, prefs);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(CLASS_NOTIF_PANEL_VIEW, classLoader, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(PREF_D2TS, true) && mGestureDetector != null && (int) XposedHelpers.callMethod(
                            XposedHelpers.getObjectField(param.thisObject, "mStatusBar"), "getBarState") == 1) {
                        mGestureDetector.onTouchEvent((MotionEvent) param.args[0]);
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void prepareGestureDetector() {
        try {
            mGestureDetector = new GestureDetector(mContext,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            Intent intent = new Intent(ACTION_SLEEP);
                            intent.putExtra(EXTRA_WAKE_ON, false);
                            mContext.sendBroadcast(intent);

                            return true;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void sleep(boolean wake) {
        try {
            final PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

            XposedHelpers.callMethod(powerManager, "goToSleep", SystemClock.uptimeMillis());

            if (wake)
                XposedHelpers.callMethod(powerManager, "wakeUp", SystemClock.uptimeMillis());
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}
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
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.dvd.android.xposed.lockscreenmods.LockscreenSettings.ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED;
import static com.dvd.android.xposed.lockscreenmods.LockscreenSettings.EXTRA_LS_SAFE_LAUNCH;
import static com.dvd.android.xposed.lockscreenmods.LockscreenSettings.EXTRA_LS_SHORTCUT_SLOT;
import static com.dvd.android.xposed.lockscreenmods.LockscreenSettings.EXTRA_LS_SHORTCUT_VALUE;
import static com.dvd.android.xposed.lockscreenmods.LockscreenSettings.PREF_SIZE_ICON;

public class ModLockscreen {

    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final boolean DEBUG = false;
    private static final String TAG = "LockScreenShortcut:ModLockscreen";
    private static final String CLASS_KGVIEW_MEDIATOR = "com.android.systemui.keyguard.KeyguardViewMediator";
    private static LockscreenAppBar mAppBar;
    private static Context mContext;
    private static Context mGbContext;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED)) {
                if (mAppBar != null) {
                    if (intent.hasExtra(EXTRA_LS_SHORTCUT_SLOT)) {
                        mAppBar.updateAppSlot(
                                intent.getIntExtra(EXTRA_LS_SHORTCUT_SLOT, 0),
                                intent.getStringExtra(EXTRA_LS_SHORTCUT_VALUE));
                    }
                    if (intent.hasExtra(EXTRA_LS_SAFE_LAUNCH)) {
                        mAppBar.setSafeLaunchEnabled(intent.getBooleanExtra(
                                EXTRA_LS_SAFE_LAUNCH, false));
                    }
                    if (intent.hasExtra(PREF_SIZE_ICON)) {
                        mAppBar.setSize(intent.getIntExtra(PREF_SIZE_ICON, 40));
                    }
                }

            }
        }
    };

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void init(final XSharedPreferences prefs,
                            final ClassLoader classLoader) {
        try {
            final Class<?> kgViewMediatorClass = XposedHelpers.findClass(
                    CLASS_KGVIEW_MEDIATOR, classLoader);

            String setupMethodName = Build.VERSION.SDK_INT >= 22 ? "setupLocked"
                    : "setup";
            XposedHelpers.findAndHookMethod(kgViewMediatorClass,
                    setupMethodName, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                final MethodHookParam param) throws Throwable {
                            mContext = (Context) XposedHelpers.getObjectField(
                                    param.thisObject, "mContext");
                            mGbContext = mContext.createPackageContext(
                                    XposedMod.PACKAGE_NAME, 0);

                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter
                                    .addAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                            mContext.registerReceiver(mBroadcastReceiver,
                                    intentFilter);
                            if (DEBUG)
                                log("Keyguard mediator constructed");
                        }
                    });

            XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUSBAR, classLoader,
                    "makeStatusBarView", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(
                                final MethodHookParam param) throws Throwable {
                            ViewGroup kgStatusView = (ViewGroup) XposedHelpers
                                    .getObjectField(param.thisObject,
                                            "mKeyguardStatusView");
                            int containerId = kgStatusView.getResources()
                                    .getIdentifier("keyguard_clock_container",
                                            "id", PACKAGE_NAME);
                            if (containerId != 0) {
                                ViewGroup container = (ViewGroup) kgStatusView
                                        .findViewById(containerId);
                                if (container != null) {
                                    mAppBar = new LockscreenAppBar(mContext,
                                            mGbContext, container,
                                            param.thisObject, prefs);
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
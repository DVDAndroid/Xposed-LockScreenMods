/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dvd.android.xposed.lockscreenmods.preference.AppPickerPreference;
import com.dvd.android.xposed.lockscreenmods.shortcut.ShortcutActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class LockscreenAppBar {
    private static final String TAG = "LockScreenShortcut:LockscreenAppBar";
    private static final boolean DEBUG = false;

    public static final String EXTRA_FROM_LOCKSCREEN = "fromLockscreen";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private Context mContext;
    private Context mGbContext;
    private ViewGroup mContainer;
    private Object mStatusBar;
    private PackageManager mPm;
    private List<AppInfo> mAppSlots;
    private ViewGroup mRootView;
    private XSharedPreferences mPrefs;
    private boolean mSafeLaunchEnabled;
    private AppInfo mPendingAction;
    private Handler mHandler;

    public LockscreenAppBar(Context ctx, Context gbCtx, ViewGroup container,
            Object statusBar, XSharedPreferences prefs) {
        mContext = ctx;
        mGbContext = gbCtx;
        mContainer = container;
        mStatusBar = statusBar;
        mPrefs = prefs;
        mPm = mContext.getPackageManager();
        mHandler = new Handler();
        mSafeLaunchEnabled = prefs.getBoolean(
                LockscreenSettings.PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH, false);

        initAppSlots();
    }

    private void initAppSlots() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        mRootView = (ViewGroup) inflater.inflate(R.layout.lockscreen_app_bar, mContainer, false);
        mContainer.addView(mRootView);

        mAppSlots = new ArrayList<>(6);
        mAppSlots.add(new AppInfo(R.id.quickapp1));
        mAppSlots.add(new AppInfo(R.id.quickapp2));
        mAppSlots.add(new AppInfo(R.id.quickapp3));
        mAppSlots.add(new AppInfo(R.id.quickapp4));
        mAppSlots.add(new AppInfo(R.id.quickapp5));
        mAppSlots.add(new AppInfo(R.id.quickapp6));

        for (int i = 0; i < LockscreenSettings.PREF_KEY_LOCKSCREEN_SHORTCUT.size(); i++) {
            updateAppSlot(i, mPrefs.getString(LockscreenSettings.PREF_KEY_LOCKSCREEN_SHORTCUT.get(i),
                    null), false);
        }
        updateRootViewVisibility();
    }

    public void setSafeLaunchEnabled(boolean enabled) {
        mSafeLaunchEnabled = enabled;
    }

    public void updateAppSlot(int slot, String value) {
        updateAppSlot(slot, value, true);
    }

    private void updateAppSlot(int slot, String value, boolean updateRootViewVisibility) {
        mAppSlots.get(slot).initAppInfo(value);
        if (updateRootViewVisibility) {
            updateRootViewVisibility();
        }
    }

    private void updateRootViewVisibility() {
        boolean atLeastOneVisible = false;
        int childCount = mRootView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (mRootView.getChildAt(i).getVisibility() == View.VISIBLE) {
                atLeastOneVisible = true;
                break;
            }
        }
        mRootView.setVisibility(atLeastOneVisible ? View.VISIBLE : View.GONE);
    }

    private void startActivity(Intent intent) {
        // we are launching from lock screen
        intent.putExtra(EXTRA_FROM_LOCKSCREEN, true);
        // if intent is a GB action of broadcast type, handle it directly here
        if (ShortcutActivity.isGbBroadcastShortcut(intent)) {
            Intent newIntent = new Intent(intent.getStringExtra(ShortcutActivity.EXTRA_ACTION));
            newIntent.putExtras(intent);
            mContext.sendBroadcast(newIntent);
        // otherwise start activity dismissing keyguard
        } else {
            try {
                XposedHelpers.callMethod(mStatusBar, "postStartSettingsActivity", intent, 0);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    private Runnable pendingActionExpiredRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPendingAction != null) {
                mPendingAction.zoomOut();
                mPendingAction = null;
            }
        }
    };

    private final class AppInfo implements View.OnClickListener {
        private Intent mIntent;
        private Resources mResources;
        private ImageView mView;

        public AppInfo(int resId) {
            mResources = mGbContext.getResources();
            mView = (ImageView) mRootView.findViewById(resId);
            mView.setVisibility(View.GONE);
            mView.setOnClickListener(this);
        }

        private void reset() {
            mIntent = null;
            mView.setImageDrawable(null);
            mView.setVisibility(View.GONE);
        }

        public void initAppInfo(String value) {
            if (value == null) {
                reset();
                return;
            }

            try {
                Drawable icon = null;
                mIntent = Intent.parseUri(value, 0);
                if (!mIntent.hasExtra("mode")) {
                    reset();
                    return;
                }
                final int mode = mIntent.getIntExtra("mode", AppPickerPreference.MODE_APP);

                final int iconResId = mIntent.getStringExtra("iconResName") != null ?
                        mResources.getIdentifier(mIntent.getStringExtra("iconResName"),
                        "drawable", mGbContext.getPackageName()) : 0;
                if (iconResId != 0) {
                    icon = mResources.getDrawable(iconResId);
                } else {
                    final String appIconPath = mIntent.getStringExtra("icon");
                    if (appIconPath != null) {
                        File f = new File(appIconPath);
                        if (f.exists() && f.canRead()) {
                            icon = Drawable.createFromPath(f.getAbsolutePath());
                        }
                    }
                }

                if (icon == null) {
                    if (mode == AppPickerPreference.MODE_APP) {
                        ActivityInfo ai = mPm.getActivityInfo(mIntent.getComponent(), 0);
                        icon = ai.loadIcon(mPm);
                    } else {
                        icon = mResources.getDrawable(android.R.drawable.ic_menu_help);
                    }
                }
                mView.setImageDrawable(icon);
                mView.setVisibility(View.VISIBLE);
                if (DEBUG) log("AppInfo initialized for: " + mIntent);
            } catch (NameNotFoundException e) {
                log("App not found: " + mIntent);
                reset();
            } catch (Exception e) {
                log("Unexpected error: " + e.getMessage());
                reset();
            }
        }

        public void zoomIn() {
            if (mView != null) {
                mView.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .start();
            }
        }

        public void zoomOut() {
            if (mView != null) {
                mView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start();
            }
        }

        @Override
        public void onClick(View v) {
            if (mSafeLaunchEnabled) {
                mHandler.removeCallbacks(pendingActionExpiredRunnable);
    
                if (mPendingAction == this) {
                    pendingActionExpiredRunnable.run();
                    if (mIntent != null) {
                        startActivity(mIntent);
                    }
                } else {
                    pendingActionExpiredRunnable.run();
                    mPendingAction = this;
                    zoomIn();
                    mHandler.postDelayed(pendingActionExpiredRunnable, 1300);
                }
            } else {
                if (mIntent != null) {
                    startActivity(mIntent);
                }
            }
        }
    }
}

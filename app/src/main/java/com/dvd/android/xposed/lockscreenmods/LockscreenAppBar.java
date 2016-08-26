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
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dvd.android.xposed.lockscreenmods.preference.AppPickerPreference;
import com.dvd.android.xposed.lockscreenmods.shortcut.ShortcutActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_ANIMATIONS_ENABLED;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_KEY_LOCKSCREEN_SHORTCUT;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_LONG_CLICK;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_SIZE_ICON;

public class LockscreenAppBar {
    public static final String EXTRA_FROM_LOCKSCREEN = "fromLockscreen";
    private static final String TAG = "LockScreenShortcut:LockscreenAppBar";
    private static final boolean DEBUG = false;
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
    private int mSize;
    private boolean mAnimations;
    private boolean mLongClick;
    private Runnable pendingActionExpiredRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPendingAction != null) {
                mPendingAction.zoomOut();
                mPendingAction = null;
            }
        }
    };

    public LockscreenAppBar(Context ctx, Context gbCtx, ViewGroup container, Object statusBar, XSharedPreferences prefs) {
        mContext = ctx;
        mGbContext = gbCtx;
        mContainer = container;
        mStatusBar = statusBar;
        mPrefs = prefs;
        mPm = mContext.getPackageManager();
        mHandler = new Handler();
        mSafeLaunchEnabled = prefs
                .getBoolean(PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH, false);
        mSize = prefs.getInt(PREF_SIZE_ICON, 95);
        mAnimations = prefs.getBoolean(PREF_ANIMATIONS_ENABLED, true);
        mLongClick = prefs.getBoolean(PREF_LONG_CLICK, false);

        initAppSlots();
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
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

        for (int i = 0; i < PREF_KEY_LOCKSCREEN_SHORTCUT.size(); i++) {
            updateAppSlot(i, mPrefs.getString(PREF_KEY_LOCKSCREEN_SHORTCUT.get(i), null), false);
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
            String method = Build.VERSION.SDK_INT >= 23 ? "postStartActivityDismissingKeyguard" : "postStartSettingsActivity";

            try {
                XposedHelpers.callMethod(mStatusBar, method, intent, 0);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    public void setSize(int size) {
        mSize = size;
        refreshAppSlots();
    }

    public void refreshAppSlots() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                mSize, mSize);

        for (int i = 0; i < mRootView.getChildCount(); i++) {
            View app = mRootView.getChildAt(i);

            app.setLayoutParams(layoutParams);
        }
    }

    public void setAnimations(boolean enabled) {
        mAnimations = enabled;
    }

    public void setLongClick(boolean enabled) {
        mLongClick = enabled;
    }

    private final class AppInfo implements View.OnClickListener, View.OnLongClickListener {
        private Intent mIntent;
        private Resources mResources;
        private ImageView mView;

        public AppInfo(int resId) {
            mResources = mGbContext.getResources();
            mView = (ImageView) mRootView.findViewById(resId);
            mView.setVisibility(View.GONE);
            mView.setOnClickListener(this);
            mView.setOnLongClickListener(this);
        }

        private void reset() {
            mIntent = null;
            mView.setImageDrawable(null);
            mView.setVisibility(View.GONE);
        }

        @SuppressWarnings("deprecation")
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
                final int mode = mIntent.getIntExtra("mode",
                        AppPickerPreference.MODE_APP);

                final int iconResId = mIntent.getStringExtra("iconResName") != null ? mResources.getIdentifier(mIntent.getStringExtra("iconResName"), "drawable", mGbContext.getPackageName()) : 0;
                if (iconResId != 0) {
                    //noinspection ResourceType
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
                        ActivityInfo ai = mPm
                                .getActivityInfo(mIntent.getComponent(), 0);
                        icon = ai.loadIcon(mPm);
                    } else {
                        icon = mResources
                                .getDrawable(android.R.drawable.ic_menu_help);
                    }
                }
                mView.setImageDrawable(icon);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        mSize, mSize);
                mView.setLayoutParams(layoutParams);
                mView.setVisibility(View.VISIBLE);
                if (DEBUG)
                    log("AppInfo initialized for: " + mIntent);
            } catch (NameNotFoundException e) {
                log("App not found: " + mIntent);
                reset();
            } catch (Exception e) {
                log("Unexpected error: " + e.getMessage());
                reset();
            }
        }

        public void zoomIn() {
            if (mAnimations && mView != null) {
                mView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                        .start();
            }
        }

        public void zoomOut() {
            if (mAnimations && mView != null) {
                mView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
        }

        @Override
        public void onClick(View v) {
            if (mLongClick)
                return;
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

        @Override
        public boolean onLongClick(View v) {
            if (!mLongClick)
                return false;
            if (mIntent != null) {
                if (mAnimations)
                    zoomIn();
                startActivity(mIntent);
            }
            return false;
        }
    }
}

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.dvd.android.xposed.lockscreenmods.preference.AppPickerPreference;

import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_LOCKSCREEN_SETTINGS_CHANGED;
import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED;
import static com.dvd.android.xposed.lockscreenmods.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SAFE_LAUNCH;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SHORTCUT_SLOT;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_LS_SHORTCUT_VALUE;
import static com.dvd.android.xposed.lockscreenmods.Utils.EXTRA_WAKE_ON;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_ANIMATIONS_ENABLED;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_CAT_KEY_LOCKSCREEN_SHORTCUTS;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_D2TS;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_HIDE_ICON;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_KEY_LOCKSCREEN_SHORTCUT;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_LONG_CLICK;
import static com.dvd.android.xposed.lockscreenmods.Utils.PREF_SIZE_ICON;

public class LockscreenSettings extends Activity {

    private static SharedPreferences mPrefs;
    private static String mainClass;

    public static boolean isPackageInstalled(Context context, String packagename) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainClass = getClass().getCanonicalName() + "Alias";
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.about:
                AlertDialog d = new AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok, null)
                        .setTitle(item.getTitle()).setIcon(item.getIcon())
                        .setMessage(R.string.about_message).create();
                d.show();
                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case R.id.sleep:
                sendBroadcast(new Intent().setAction(ACTION_SLEEP).putExtra(EXTRA_WAKE_ON, true));
        }

        return super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private ShortcutHandler mShortcutHandler;
        private IconPickHandler mIconPickHandler;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // this is important because although the handler classes that read
            // these settings
            // are in the same package, they are executed in the context of the
            // hooked package
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.settings);

            mPrefs = getPreferenceScreen().getSharedPreferences();
            AppPickerPreference.sPrefsFragment = this;
            AppPickerPreference.cleanupAsync(getActivity());

            PreferenceCategory mPrefCatLsShortcuts = (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_SHORTCUTS);
            mPrefs = getPreferenceManager().getSharedPreferences();

            if (isPackageInstalled(getActivity(), "com.ceco.lollipop.gravitybox") && mPrefs.getBoolean("welcome", true)) {
                AlertDialog alert = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.warning)
                        .setMessage(R.string.gravitybox_installed)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null).create();
                alert.show();

                mPrefs.edit().putBoolean("welcome", false).apply();
            }

            for (int i = 0; i < PREF_KEY_LOCKSCREEN_SHORTCUT.size(); i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                appPref.setKey(PREF_KEY_LOCKSCREEN_SHORTCUT.get(i));
                appPref.setTitle(String.format(
                        getString(R.string.pref_app_launcher_slot_title),
                        i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getString(R.string.app_picker_none));
                appPref.setSummary(getString(R.string.app_picker_none));
                mPrefCatLsShortcuts.addPreference(appPref);
                if (mPrefs.getString(appPref.getKey(), null) == null) {
                    mPrefs.edit().putString(appPref.getKey(), null).apply();
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            Intent intent = new Intent();

            if (PREF_KEY_LOCKSCREEN_SHORTCUT.contains(key)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                intent.putExtra(EXTRA_LS_SHORTCUT_SLOT, PREF_KEY_LOCKSCREEN_SHORTCUT.indexOf(key));
                intent.putExtra(EXTRA_LS_SHORTCUT_VALUE, mPrefs.getString(key, null));
            } else if (key.equals(PREF_SIZE_ICON)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                intent.putExtra(PREF_SIZE_ICON, sharedPreferences.getInt(key, 40));
            } else if (key.equals(PREF_KEY_LOCKSCREEN_SHORTCUT_SAFE_LAUNCH)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                intent.putExtra(EXTRA_LS_SAFE_LAUNCH, mPrefs.getBoolean(key, false));
            } else if (key.equals(PREF_ANIMATIONS_ENABLED)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                intent.putExtra(PREF_ANIMATIONS_ENABLED, sharedPreferences.getBoolean(PREF_ANIMATIONS_ENABLED, true));
            } else if (key.equals(PREF_LONG_CLICK)) {
                intent.setAction(ACTION_PREF_LOCKSCREEN_SHORTCUT_SETTING_CHANGED);
                intent.putExtra(PREF_LONG_CLICK, sharedPreferences.getBoolean(PREF_LONG_CLICK, true));
            } else if (key.equals(PREF_D2TS)) {
                intent.setAction(ACTION_LOCKSCREEN_SETTINGS_CHANGED);
                intent.putExtra(PREF_D2TS, sharedPreferences.getBoolean(PREF_D2TS, true));
            } else if (key.equals(PREF_HIDE_ICON)) {
                int mode = sharedPreferences.getBoolean(key, false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), mainClass), mode, PackageManager.DONT_KILL_APP);
            }
            if (intent.getAction() != null) {
                mPrefs.edit().apply();
                getActivity().sendBroadcast(intent);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            super.onStop();
        }

        public void obtainShortcut(ShortcutHandler handler) {
            if (handler == null)
                return;

            mShortcutHandler = handler;
            startActivityForResult(mShortcutHandler.getCreateShortcutIntent(), 1028);
        }


        public interface ShortcutHandler {
            Intent getCreateShortcutIntent();

            void onHandleShortcut(Intent intent, String name, Bitmap icon);

            void onShortcutCancelled();
        }

        public interface IconPickHandler {
            void onIconPicked(Bitmap icon);

            void onIconPickCancelled();
        }

    }
}
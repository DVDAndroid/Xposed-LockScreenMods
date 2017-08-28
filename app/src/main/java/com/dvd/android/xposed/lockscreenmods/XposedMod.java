package com.dvd.android.xposed.lockscreenmods;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public static String PACKAGE_NAME = "com.dvd.android.xposed.lockscreenmods";
    private XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (Build.VERSION.SDK_INT > 23) { // for context.getSharedPreferences("", MODE_WORLD_READABLE) disabled from nougat.
            findAndHookMethod("android.app.ContextImpl", null, "checkMode", int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return null;
                }
            });
        }

        prefs = new XSharedPreferences(PACKAGE_NAME);
        prefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(ModLockscreen.PACKAGE_NAME)) {
            ModLockscreen.init(prefs, lpparam.classLoader);
        }
    }
}
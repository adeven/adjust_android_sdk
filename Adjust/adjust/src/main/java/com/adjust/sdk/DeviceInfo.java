package com.adjust.sdk;

import android.annotation.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;
import java.util.Map;

import static com.adjust.sdk.Constants.HIGH;
import static com.adjust.sdk.Constants.LARGE;
import static com.adjust.sdk.Constants.LONG;
import static com.adjust.sdk.Constants.LOW;
import static com.adjust.sdk.Constants.MEDIUM;
import static com.adjust.sdk.Constants.NORMAL;
import static com.adjust.sdk.Constants.SMALL;
import static com.adjust.sdk.Constants.XLARGE;

/**
 * Created by pfms on 06/11/14.
 */
class DeviceInfo {
    String macSha1;
    String macShortMd5;
    String androidId;
    String fbAttributionId;
    String clientSdk;
    String packageName;
    String appVersion;
    String deviceType;
    String deviceName;
    String deviceManufacturer;
    String osName;
    String osVersion;
    String apiLevel;
    String language;
    String country;
    String screenSize;
    String screenFormat;
    String screenDensity;
    String displayWidth;
    String displayHeight;
    String hardwareName;
    String abi;
    Map<String, String> pluginKeys;

    @SuppressWarnings("deprecation")
    private static Locale getSystemLocaleLegacy(final Configuration config) {
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Locale getSystemLocale(final Configuration config) {
        return config.getLocales().get(0);
    }

    DeviceInfo(final Context context, final String sdkPrefix) {
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        Locale locale;
        int screenLayout = configuration.screenLayout;
        boolean isGooglePlayServicesAvailable = Util.getPlayAdId(context) != null;
        String macAddress = getMacAddress(context, isGooglePlayServicesAvailable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = getSystemLocale(configuration);
        } else {
            locale = getSystemLocaleLegacy(configuration);
        }

        packageName = getPackageName(context);
        appVersion = getAppVersion(context);
        deviceType = getDeviceType(screenLayout);
        deviceName = getDeviceName();
        deviceManufacturer = getDeviceManufacturer();
        osName = getOsName();
        osVersion = getOsVersion();
        apiLevel = getApiLevel();
        language = getLanguage(locale);
        country = getCountry(locale);
        screenSize = getScreenSize(screenLayout);
        screenFormat = getScreenFormat(screenLayout);
        screenDensity = getScreenDensity(displayMetrics);
        displayWidth = getDisplayWidth(displayMetrics);
        displayHeight = getDisplayHeight(displayMetrics);
        clientSdk = getClientSdk(sdkPrefix);
        androidId = getAndroidId(context, isGooglePlayServicesAvailable);
        fbAttributionId = getFacebookAttributionId(context);
        pluginKeys = Util.getPluginKeys(context);
        macSha1 = getMacSha1(macAddress);
        macShortMd5 = getMacShortMd5(macAddress);
        hardwareName = getHardwareName();
        abi = getABI();
    }

    private String getMacAddress(final Context context,
                                 final boolean isGooglePlayServicesAvailable) {
        if (!isGooglePlayServicesAvailable) {
            if (!Util.checkPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE)) {
                AdjustFactory.getLogger().warn("Missing permission: ACCESS_WIFI_STATE");
            }
            return Util.getMacAddress(context);
        } else {
            return null;
        }
    }

    private String getPackageName(final Context context) {
        return context.getPackageName();
    }

    private String getAppVersion(final Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String name = context.getPackageName();
            PackageInfo info = packageManager.getPackageInfo(name, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getDeviceType(final int screenLayout) {
        int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return "phone";
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case 4:
                return "tablet";
            default:
                return null;
        }
    }

    private String getDeviceName() {
        return Build.MODEL;
    }

    private String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    private String getOsName() {
        return "android";
    }

    private String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    private String getApiLevel() {
        return "" + Build.VERSION.SDK_INT;
    }

    private String getLanguage(final Locale locale) {
        return locale.getLanguage();
    }

    private String getCountry(final Locale locale) {
        return locale.getCountry();
    }

    private String getHardwareName() {
        return Build.DISPLAY;
    }

    private String getScreenSize(final int screenLayout) {
        int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                return SMALL;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return NORMAL;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                return LARGE;
            case 4:
                return XLARGE;
            default:
                return null;
        }
    }

    private String getScreenFormat(final int screenLayout) {
        int screenFormat = screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;

        switch (screenFormat) {
            case Configuration.SCREENLAYOUT_LONG_YES:
                return LONG;
            case Configuration.SCREENLAYOUT_LONG_NO:
                return NORMAL;
            default:
                return null;
        }
    }

    private String getScreenDensity(final DisplayMetrics displayMetrics) {
        int density = displayMetrics.densityDpi;
        int low = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_LOW) / 2;
        int high = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_HIGH) / 2;

        if (density == 0) {
            return null;
        } else if (density < low) {
            return LOW;
        } else if (density > high) {
            return HIGH;
        }
        return MEDIUM;
    }

    private String getDisplayWidth(final DisplayMetrics displayMetrics) {
        return String.valueOf(displayMetrics.widthPixels);
    }

    private String getDisplayHeight(final DisplayMetrics displayMetrics) {
        return String.valueOf(displayMetrics.heightPixels);
    }

    private String getClientSdk(final String sdkPrefix) {
        if (sdkPrefix == null) {
            return Constants.CLIENT_SDK;
        } else {
            return String.format(Locale.US, "%s@%s", sdkPrefix, Constants.CLIENT_SDK);
        }
    }

    private String getMacSha1(final String macAddress) {
        if (macAddress == null) {
            return null;
        }

        return Util.sha1(macAddress);
    }

    private String getMacShortMd5(final String macAddress) {
        if (macAddress == null) {
            return null;
        }
        String macShort = macAddress.replaceAll(":", "");

        return Util.md5(macShort);
    }

    private String getAndroidId(final Context context,
                                final boolean isGooglePlayServicesAvailable) {
        if (!isGooglePlayServicesAvailable) {
            return Util.getAndroidId(context);
        } else {
            return null;
        }
    }

    private String getFacebookAttributionId(final Context context) {
        try {
            final ContentResolver contentResolver = context.getContentResolver();
            final Uri uri = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
            final String columnName = "aid";
            final String[] projection = {columnName};
            final Cursor cursor = contentResolver.query(uri, projection, null, null, null);

            if (cursor == null) {
                return null;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            final String attributionId = cursor.getString(cursor.getColumnIndex(columnName));
            cursor.close();
            return attributionId;
        } catch (Exception e) {
            return null;
        }
    }

    private String getABI() {
        String[] SupportedABIS = Util.getSupportedAbis();

        // SUPPORTED_ABIS is only supported in API level 21
        // get CPU_ABI instead
        if (SupportedABIS == null || SupportedABIS.length == 0) {
            return Util.getCpuAbi();
        }

        return SupportedABIS[0];
    }
}

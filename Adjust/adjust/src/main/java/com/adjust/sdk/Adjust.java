//
//  Adjust.java
//  Adjust
//
//  Created by Christian Wellenbrock on 2012-10-11.
//  Copyright (c) 2012-2014 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import android.content.Context;
import android.net.Uri;

/**
 * The main interface to Adjust.
 * Use the methods of this class to tell Adjust about the usage of your app.
 * See the README for details.
 */
public class Adjust {

    private static AdjustInstance defaultInstance;

    private Adjust() {
    }

    public static synchronized AdjustInstance getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new AdjustInstance();
        }
        return defaultInstance;
    }

    public static void onCreate(AdjustConfig adjustConfig) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.onCreate(adjustConfig);
    }

    public static void trackEvent(AdjustEvent event) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.trackEvent(event);
    }

    public static void onResume() {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.onResume();
    }

    public static void onPause() {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.onPause();
    }

    public static void setEnabled(boolean enabled) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.setEnabled(enabled);
    }

    public static boolean isEnabled() {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        return adjustInstance.isEnabled();
    }

    public static void appWillOpenUrl(Uri url) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.appWillOpenUrl(url);
    }

    public static void setReferrer(String referrer) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.sendReferrer(referrer);
    }

    public static void setOfflineMode(boolean enabled) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.setOfflineMode(enabled);
    }

    public static void getGoogleAdId(Context context, OnDeviceIdsRead onDeviceIdRead) {
        Util.getGoogleAdId(context, onDeviceIdRead);
    }

    public static void addSessionCallbackParameter(String key, String value) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.addSessionCallbackParameter(key, value);
    }

    public static void addSessionPartnerParameter(String key, String value) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.addSessionPartnerParameter(key, value);
    }

    // XXX TODO replace with remove, and reset session callback parameters
    public static void updateSessionCallbackParameters(SessionCallbackParametersUpdater sessionCallbackParametersUpdater) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.updateSessionCallbackParameters(sessionCallbackParametersUpdater);
    }

    // XXX TODO replace with remove, and reset session partner parameters
    public static void updateSessionPartnerParameters(SessionPartnerParametersUpdater sessionPartnerParametersUpdater) {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.updateSessionPartnerParameters(sessionPartnerParametersUpdater);
    }

    public static void sendFirstPackages() {
        AdjustInstance adjustInstance = Adjust.getDefaultInstance();
        adjustInstance.sendFirstPackages();
    }
}



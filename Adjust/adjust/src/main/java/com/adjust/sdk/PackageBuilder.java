//
//  PackageBuilder.java
//  Adjust
//
//  Created by Christian Wellenbrock on 2013-06-25.
//  Copyright (c) 2013 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import android.text.TextUtils;

import org.json.JSONObject;

import java.text.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.adjust.sdk.Constants.CALLBACK_PARAMETERS;
import static com.adjust.sdk.Constants.PARTNER_PARAMETERS;

class PackageBuilder {
    private AdjustConfig adjustConfig;
    private DeviceInfo deviceInfo;
    private ActivityStateCopy activityStateCopy;
    private long createdAt;

    // reattributions
    Map<String, String> extraParameters;
    AdjustAttribution attribution;
    String reftag;
    String referrer;
    String deeplink;
    String pushToken;
    long clickTime;

    private static ILogger logger = AdjustFactory.getLogger();

    private static class ActivityStateCopy {
        long lastInterval = -1;
        int eventCount = -1;
        String uuid = null;
        int sessionCount = -1;
        int subsessionCount = -1;
        long sessionLength = -1;
        long timeSpent = -1;

        ActivityStateCopy(final ActivityState activityState) {
            if (activityState == null) {
                return;
            }
            this.lastInterval = activityState.lastInterval;
            this.eventCount = activityState.eventCount;
            this.uuid = activityState.uuid;
            this.sessionCount = activityState.sessionCount;
            this.subsessionCount = activityState.subsessionCount;
            this.sessionLength = activityState.sessionLength;
            this.timeSpent = activityState.timeSpent;
        }
    }

    PackageBuilder(final AdjustConfig adjustConfig,
                   final DeviceInfo deviceInfo,
                   final ActivityState activityState,
                   final long createdAt) {
        this.adjustConfig = adjustConfig;
        this.deviceInfo = deviceInfo;
        this.activityStateCopy = new ActivityStateCopy(activityState);
        this.createdAt = createdAt;
    }

    public ActivityPackage buildSessionPackage(final SessionParameters sessionParameters,
                                               final boolean isInDelay) {
        Map<String, String> parameters = getDefaultParameters();
        PackageBuilder.addDuration(parameters, "last_interval", activityStateCopy.lastInterval);
        PackageBuilder.addString(parameters, "default_tracker", adjustConfig.defaultTracker);

        if (!isInDelay) {
            PackageBuilder.addMapJson(parameters, CALLBACK_PARAMETERS, sessionParameters.callbackParameters);
            PackageBuilder.addMapJson(parameters, PARTNER_PARAMETERS, sessionParameters.partnerParameters);
        }

        ActivityPackage sessionPackage = getDefaultActivityPackage(ActivityKind.SESSION);
        sessionPackage.setPath("/session");
        sessionPackage.setSuffix("");
        sessionPackage.setParameters(parameters);

        return sessionPackage;
    }

    public ActivityPackage buildEventPackage(final AdjustEvent event,
                                             final SessionParameters sessionParameters,
                                             final boolean isInDelay) {
        Map<String, String> parameters = getDefaultParameters();
        PackageBuilder.addInt(parameters, "event_count", activityStateCopy.eventCount);
        PackageBuilder.addString(parameters, "event_token", event.eventToken);
        PackageBuilder.addDouble(parameters, "revenue", event.revenue);
        PackageBuilder.addString(parameters, "currency", event.currency);

        if (!isInDelay) {
            PackageBuilder.addMapJson(parameters, CALLBACK_PARAMETERS,
                    Util.mergeParameters(sessionParameters.callbackParameters, event.callbackParameters, "Callback"));
            PackageBuilder.addMapJson(parameters, PARTNER_PARAMETERS,
                    Util.mergeParameters(sessionParameters.partnerParameters, event.partnerParameters, "Partner"));
        }
        ActivityPackage eventPackage = getDefaultActivityPackage(ActivityKind.EVENT);
        eventPackage.setPath("/event");
        eventPackage.setSuffix(getEventSuffix(event));
        eventPackage.setParameters(parameters);

        if (isInDelay) {
            eventPackage.setCallbackParameters(event.callbackParameters);
            eventPackage.setPartnerParameters(event.partnerParameters);
        }

        return eventPackage;
    }

    public ActivityPackage buildClickPackage(final String source) {
        Map<String, String> parameters = getIdsParameters();

        PackageBuilder.addString(parameters, "source", source);
        PackageBuilder.addDate(parameters, "click_time", clickTime);
        PackageBuilder.addString(parameters, "reftag", reftag);
        PackageBuilder.addMapJson(parameters, "params", extraParameters);
        PackageBuilder.addString(parameters, "referrer", referrer);
        PackageBuilder.addString(parameters, "deeplink", deeplink);
        PackageBuilder.addString(parameters, "push_token", pushToken);
        injectAttribution(parameters);

        ActivityPackage clickPackage = getDefaultActivityPackage(ActivityKind.CLICK);
        clickPackage.setPath("/sdk_click");
        clickPackage.setSuffix("");
        clickPackage.setParameters(parameters);

        return clickPackage;
    }

    public ActivityPackage buildAttributionPackage() {
        Map<String, String> parameters = getIdsParameters();

        ActivityPackage attributionPackage = getDefaultActivityPackage(ActivityKind.ATTRIBUTION);
        attributionPackage.setPath("attribution"); // does not contain '/' because of Uri.Builder.appendPath
        attributionPackage.setSuffix("");
        attributionPackage.setParameters(parameters);

        return attributionPackage;
    }

    private ActivityPackage getDefaultActivityPackage(final ActivityKind activityKind) {
        ActivityPackage activityPackage = new ActivityPackage(activityKind);
        activityPackage.setClientSdk(deviceInfo.clientSdk);
        return activityPackage;
    }

    private Map<String, String> getDefaultParameters() {
        Map<String, String> parameters = new HashMap<>();

        injectDeviceInfo(parameters);
        injectConfig(parameters);
        injectActivityState(parameters);
        injectCommonParameters(parameters);

        // general
        checkDeviceIds(parameters);

        return parameters;
    }

    private Map<String, String> getIdsParameters() {
        Map<String, String> parameters = new HashMap<>();

        injectDeviceInfoIds(parameters);
        injectConfig(parameters);
        injectCommonParameters(parameters);

        checkDeviceIds(parameters);

        return parameters;
    }

    private void injectDeviceInfo(final Map<String, String> parameters) {
        injectDeviceInfoIds(parameters);
        PackageBuilder.addString(parameters, "fb_id", deviceInfo.fbAttributionId);
        PackageBuilder.addString(parameters, "package_name", deviceInfo.packageName);
        PackageBuilder.addString(parameters, "app_version", deviceInfo.appVersion);
        PackageBuilder.addString(parameters, "device_type", deviceInfo.deviceType);
        PackageBuilder.addString(parameters, "device_name", deviceInfo.deviceName);
        PackageBuilder.addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer);
        PackageBuilder.addString(parameters, "os_name", deviceInfo.osName);
        PackageBuilder.addString(parameters, "os_version", deviceInfo.osVersion);
        PackageBuilder.addString(parameters, "api_level", deviceInfo.apiLevel);
        PackageBuilder.addString(parameters, "language", deviceInfo.language);
        PackageBuilder.addString(parameters, "country", deviceInfo.country);
        PackageBuilder.addString(parameters, "screen_size", deviceInfo.screenSize);
        PackageBuilder.addString(parameters, "screen_format", deviceInfo.screenFormat);
        PackageBuilder.addString(parameters, "screen_density", deviceInfo.screenDensity);
        PackageBuilder.addString(parameters, "display_width", deviceInfo.displayWidth);
        PackageBuilder.addString(parameters, "display_height", deviceInfo.displayHeight);
        PackageBuilder.addString(parameters, "hardware_name", deviceInfo.hardwareName);
        PackageBuilder.addString(parameters, "cpu_type", deviceInfo.abi);
        fillPluginKeys(parameters);
    }

    private void injectDeviceInfoIds(final Map<String, String> parameters) {
        PackageBuilder.addString(parameters, "mac_sha1", deviceInfo.macSha1);
        PackageBuilder.addString(parameters, "mac_md5", deviceInfo.macShortMd5);
        PackageBuilder.addString(parameters, "android_id", deviceInfo.androidId);
    }

    private void injectConfig(final Map<String, String> parameters) {
        PackageBuilder.addString(parameters, "app_token", adjustConfig.appToken);
        PackageBuilder.addString(parameters, "environment", adjustConfig.environment);
        PackageBuilder.addBoolean(parameters, "device_known", adjustConfig.deviceKnown);
        PackageBuilder.addBoolean(parameters, "needs_response_details", adjustConfig.hasListener());

        String playAdId = Util.getPlayAdId(adjustConfig.context);
        PackageBuilder.addString(parameters, "gps_adid", playAdId);
        Boolean isTrackingEnabled = Util.isPlayTrackingEnabled(adjustConfig.context);
        PackageBuilder.addBoolean(parameters, "tracking_enabled", isTrackingEnabled);
        PackageBuilder.addBoolean(parameters, "event_buffering_enabled", adjustConfig.eventBufferingEnabled);
    }

    private void injectActivityState(final Map<String, String> parameters) {
        PackageBuilder.addString(parameters, "android_uuid", activityStateCopy.uuid);
        PackageBuilder.addInt(parameters, "session_count", activityStateCopy.sessionCount);
        PackageBuilder.addInt(parameters, "subsession_count", activityStateCopy.subsessionCount);
        PackageBuilder.addDuration(parameters, "session_length", activityStateCopy.sessionLength);
        PackageBuilder.addDuration(parameters, "time_spent", activityStateCopy.timeSpent);
    }

    private void injectCommonParameters(final Map<String, String> parameters) {
        PackageBuilder.addDate(parameters, "created_at", createdAt);
        PackageBuilder.addBoolean(parameters, "attribution_deeplink", true);
    }

    private void injectAttribution(final Map<String, String> parameters) {
        if (attribution == null) {
            return;
        }
        PackageBuilder.addString(parameters, "tracker", attribution.trackerName);
        PackageBuilder.addString(parameters, "campaign", attribution.campaign);
        PackageBuilder.addString(parameters, "adgroup", attribution.adgroup);
        PackageBuilder.addString(parameters, "creative", attribution.creative);
    }

    private void checkDeviceIds(final Map<String, String> parameters) {
        if (!parameters.containsKey("mac_sha1")
                && !parameters.containsKey("mac_md5")
                && !parameters.containsKey("android_id")
                && !parameters.containsKey("gps_adid")) {
            logger.error("Missing device id's. Please check if Proguard is correctly set with Adjust SDK");
        }
    }

    private void fillPluginKeys(final Map<String, String> parameters) {
        if (deviceInfo.pluginKeys == null) {
            return;
        }

        for (Map.Entry<String, String> entry : deviceInfo.pluginKeys.entrySet()) {
            PackageBuilder.addString(parameters, entry.getKey(), entry.getValue());
        }
    }

    private String getEventSuffix(final AdjustEvent event) {
        if (event.revenue == null) {
            return String.format(Locale.US, "'%s'", event.eventToken);
        } else {
            return String.format(Locale.US, "(%.5f %s, '%s')", event.revenue, event.currency, event.eventToken);
        }
    }

    public static void addString(final Map<String, String> parameters,
                                 final String key,
                                 final String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        parameters.put(key, value);
    }

    public static void addInt(final Map<String, String> parameters,
                              final String key,
                              final long value) {
        if (value < 0) {
            return;
        }

        String valueString = Long.toString(value);
        PackageBuilder.addString(parameters, key, valueString);
    }

    public static void addDate(final Map<String, String> parameters,
                               final String key,
                               final long value) {
        if (value < 0) {
            return;
        }

        final DateFormat dateFormat = new SimpleDateFormat(Util.DATE_FORMAT);
        String dateString = dateFormat.format(value);
        PackageBuilder.addString(parameters, key, dateString);
    }

    public static void addDuration(final Map<String, String> parameters,
                                   final String key,
                                   final long durationInMilliSeconds) {
        if (durationInMilliSeconds < 0) {
            return;
        }

        long durationInSeconds = (durationInMilliSeconds + 500) / 1000;
        PackageBuilder.addInt(parameters, key, durationInSeconds);
    }

    public static void addMapJson(final Map<String, String> parameters,
                                  final String key,
                                  final Map<String, String> map) {
        if (map == null) {
            return;
        }

        if (map.size() == 0) {
            return;
        }

        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toString();

        PackageBuilder.addString(parameters, key, jsonString);
    }

    public static void addBoolean(final Map<String, String> parameters,
                                  final String key,
                                  final Boolean value) {
        if (value == null) {
            return;
        }

        int intValue = value ? 1 : 0;

        PackageBuilder.addInt(parameters, key, intValue);
    }

    public static void addDouble(final Map<String, String> parameters,
                                 final String key,
                                 final Double value) {
        if (value == null) return;

        String doubleString = String.format(Locale.US, "%.5f", value);

        PackageBuilder.addString(parameters, key, doubleString);
    }
}

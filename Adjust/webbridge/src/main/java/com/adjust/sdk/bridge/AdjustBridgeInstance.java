package com.adjust.sdk.bridge;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.app.Application;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustFactory;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;
import com.adjust.sdk.AdjustTestOptions;
import com.adjust.sdk.ILogger;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.adjust.sdk.OnDeeplinkResponseListener;
import com.adjust.sdk.OnDeviceIdsRead;
import com.adjust.sdk.OnEventTrackingFailedListener;
import com.adjust.sdk.OnEventTrackingSucceededListener;
import com.adjust.sdk.OnSessionTrackingFailedListener;
import com.adjust.sdk.OnSessionTrackingSucceededListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by uerceg on 22/07/16.
 */
public class AdjustBridgeInstance {
    private static final String LOG_LEVEL_VERBOSE               = "VERBOSE";
    private static final String LOG_LEVEL_DEBUG                 = "DEBUG";
    private static final String LOG_LEVEL_INFO                  = "INFO";
    private static final String LOG_LEVEL_WARN                  = "WARN";
    private static final String LOG_LEVEL_ERROR                 = "ERROR";
    private static final String LOG_LEVEL_ASSERT                = "ASSERT";
    private static final String LOG_LEVEL_SUPPRESS              = "SUPPRESS";

    private WebView webView;
    private Application application;

    private boolean isInitialized = false;
    private boolean shouldDeferredDeeplinkBeLaunched = true;

    // Automatically subscribe to Android lifecycle callbacks to properly handle session tracking.
    // This requires user to have minimal supported API level set to 14.
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static final class AdjustLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(Activity activity) {}
    }

    private boolean checkInit() {
        if (webView == null) {
            AdjustBridgeUtil.getLogger().error("Webview missing. Call AdjustBridge.setWebView before");
            return false;
        }

        if (application == null) {
            AdjustBridgeUtil.getLogger().error("Application context missing. Call AdjustBridge.setApplicationContext before");
            return false;
        }

        return true;
    }

    @JavascriptInterface
    public void onCreate(String adjustConfigString) {
        // Initialise SDK only if it's not already initialised.
        if (isInitialized) {
            AdjustBridgeUtil.getLogger().warn("Adjust bridge is already initialized. Ignoring further attempts");
            return;
        }

        if (!checkInit()) {
            return;
        }

        try {
            JSONObject jsonAdjustConfig = new JSONObject(adjustConfigString);

            Object appTokenField = jsonAdjustConfig.get("appToken");
            Object environmentField = jsonAdjustConfig.get("environment");
            Object allowSuppressLogLevelField = jsonAdjustConfig.get("allowSuppressLogLevel");
            Object eventBufferingEnabledField = jsonAdjustConfig.get("eventBufferingEnabled");
            Object sendInBackgroundField = jsonAdjustConfig.get("sendInBackground");
            Object logLevelField = jsonAdjustConfig.get("logLevel");
            Object sdkPrefixField = jsonAdjustConfig.get("sdkPrefix");
            Object processNameField = jsonAdjustConfig.get("processName");
            Object defaultTrackerField = jsonAdjustConfig.get("defaultTracker");
            Object attributionCallbackNameField = jsonAdjustConfig.get("attributionCallbackName");
            Object deviceKnownField = jsonAdjustConfig.get("deviceKnown");
            Object eventSuccessCallbackNameField = jsonAdjustConfig.get("eventSuccessCallbackName");
            Object eventFailureCallbackNameField = jsonAdjustConfig.get("eventFailureCallbackName");
            Object sessionSuccessCallbackNameField = jsonAdjustConfig.get("sessionSuccessCallbackName");
            Object sessionFailureCallbackNameField = jsonAdjustConfig.get("sessionFailureCallbackName");
            Object openDeferredDeeplinkField = jsonAdjustConfig.get("openDeferredDeeplink");
            Object deferredDeeplinkCallbackNameField = jsonAdjustConfig.get("deferredDeeplinkCallbackName");
            Object delayStartField = jsonAdjustConfig.get("delayStart");
            Object userAgentField = jsonAdjustConfig.get("userAgent");
            Object secretIdField = jsonAdjustConfig.get("secretId");
            Object info1Field = jsonAdjustConfig.get("info1");
            Object info2Field = jsonAdjustConfig.get("info2");
            Object info3Field = jsonAdjustConfig.get("info3");
            Object info4Field = jsonAdjustConfig.get("info4");
            Object readMobileEquipmentIdentityField = jsonAdjustConfig.get("readMobileEquipmentIdentity");

            String appToken = AdjustBridgeUtil.fieldToString(appTokenField);
            String environment = AdjustBridgeUtil.fieldToString(environmentField);
            Boolean allowSuppressLogLevel = AdjustBridgeUtil.fieldToBoolean(allowSuppressLogLevelField);

            AdjustConfig adjustConfig;
            if (allowSuppressLogLevel == null) {
                adjustConfig = new AdjustConfig(application.getApplicationContext(), appToken, environment);
            } else {
                adjustConfig = new AdjustConfig(application.getApplicationContext(), appToken, environment, allowSuppressLogLevel.booleanValue());
            }
            if (!adjustConfig.isValid()) {
                return;
            }

            // Event buffering
            Boolean eventBufferingEnabled = AdjustBridgeUtil.fieldToBoolean(eventBufferingEnabledField);
            if (eventBufferingEnabled != null) {
                adjustConfig.setEventBufferingEnabled(eventBufferingEnabled);
            }

            // Send in the background
            Boolean sendInBackground = AdjustBridgeUtil.fieldToBoolean(sendInBackgroundField);
            if (sendInBackground != null) {
                adjustConfig.setSendInBackground(sendInBackground);
            }

            // Log level
            String logLevelString = AdjustBridgeUtil.fieldToString(logLevelField);
            if (logLevelString != null) {
                if (logLevelString.equalsIgnoreCase(LOG_LEVEL_VERBOSE)) {
                    adjustConfig.setLogLevel(LogLevel.VERBOSE);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_DEBUG)) {
                    adjustConfig.setLogLevel(LogLevel.DEBUG);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_INFO)) {
                    adjustConfig.setLogLevel(LogLevel.INFO);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_WARN)) {
                    adjustConfig.setLogLevel(LogLevel.WARN);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_ERROR)) {
                    adjustConfig.setLogLevel(LogLevel.ERROR);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_ASSERT)) {
                    adjustConfig.setLogLevel(LogLevel.ASSERT);
                } else if (logLevelString.equalsIgnoreCase(LOG_LEVEL_SUPPRESS)) {
                    adjustConfig.setLogLevel(LogLevel.SUPRESS);
                }
            }

            // Sdk prefix
            String sdkPrefix = AdjustBridgeUtil.fieldToString(sdkPrefixField);
            if (sdkPrefix != null) {
                adjustConfig.setSdkPrefix(sdkPrefix);
            }

            // Main process name
            String processName = AdjustBridgeUtil.fieldToString(processNameField);
            if (processName != null) {
                adjustConfig.setProcessName(processName);
            }

            // Default tracker
            String defaultTracker = AdjustBridgeUtil.fieldToString(defaultTrackerField);
            if (defaultTracker != null) {
                adjustConfig.setDefaultTracker(defaultTracker);
            }

            // Attribution callback name
            final String attributionCallbackName = AdjustBridgeUtil.fieldToString(attributionCallbackNameField);
            if (attributionCallbackName != null) {
                adjustConfig.setOnAttributionChangedListener(new OnAttributionChangedListener() {
                    @Override
                    public void onAttributionChanged(AdjustAttribution attribution) {
                        AdjustBridgeUtil.execAttributionCallbackCommand(webView, attributionCallbackName, attribution);
                    }
                });
            }

            // Is device known
            Boolean deviceKnown = AdjustBridgeUtil.fieldToBoolean(deviceKnownField);
            if (deviceKnown != null) {
                adjustConfig.setDeviceKnown(deviceKnown);
            }

            // Event success callback
            final String eventSuccessCallbackName = AdjustBridgeUtil.fieldToString(eventSuccessCallbackNameField);
            if (eventSuccessCallbackName != null) {
                adjustConfig.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
                    public void onFinishedEventTrackingSucceeded(AdjustEventSuccess eventSuccessResponseData) {
                        AdjustBridgeUtil.execEventSuccessCallbackCommand(webView, eventSuccessCallbackName, eventSuccessResponseData);
                    }
                });
            }

            // Event failure callback
            final String eventFailureCallbackName = AdjustBridgeUtil.fieldToString(eventFailureCallbackNameField);
            if (eventFailureCallbackName != null) {
                adjustConfig.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
                    public void onFinishedEventTrackingFailed(AdjustEventFailure eventFailureResponseData) {
                        AdjustBridgeUtil.execEventFailureCallbackCommand(webView, eventFailureCallbackName, eventFailureResponseData);
                    }
                });
            }

            // Session success callback
            final String sessionSuccessCallbackName = AdjustBridgeUtil.fieldToString(sessionSuccessCallbackNameField);
            if (sessionSuccessCallbackName != null) {
                adjustConfig.setOnSessionTrackingSucceededListener(new OnSessionTrackingSucceededListener() {
                    @Override
                    public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess sessionSuccessResponseData) {
                        AdjustBridgeUtil.execSessionSuccessCallbackCommand(webView, sessionSuccessCallbackName, sessionSuccessResponseData);
                    }
                });
            }

            // Session failure callback
            final String sessionFailureCallbackName = AdjustBridgeUtil.fieldToString(sessionFailureCallbackNameField);
            if (sessionFailureCallbackName != null) {
                adjustConfig.setOnSessionTrackingFailedListener(new OnSessionTrackingFailedListener() {
                    @Override
                    public void onFinishedSessionTrackingFailed(AdjustSessionFailure failureResponseData) {
                        AdjustBridgeUtil.execSessionFailureCallbackCommand(webView, sessionFailureCallbackName, failureResponseData);
                    }
                });
            }

            // Should deferred deep link be opened?
            Boolean openDeferredDeeplink = AdjustBridgeUtil.fieldToBoolean(openDeferredDeeplinkField);
            if (openDeferredDeeplink != null) {
                shouldDeferredDeeplinkBeLaunched = openDeferredDeeplink;
            }

            // Deferred deeplink callback
            final String deferredDeeplinkCallbackName = AdjustBridgeUtil.fieldToString(deferredDeeplinkCallbackNameField);
            if (deferredDeeplinkCallbackName != null) {
                adjustConfig.setOnDeeplinkResponseListener(new OnDeeplinkResponseListener() {
                    @Override
                    public boolean launchReceivedDeeplink(Uri deeplink) {
                        AdjustBridgeUtil.execSingleValueCallback(webView, deferredDeeplinkCallbackName, deeplink.toString());
                        return shouldDeferredDeeplinkBeLaunched;
                    }
                });
            }

            // Delay start
            Double delayStart = AdjustBridgeUtil.fieldToDouble(delayStartField);
            if (delayStart != null) {
                adjustConfig.setDelayStart(delayStart);
            }

            // User agent
            String userAgent = AdjustBridgeUtil.fieldToString(userAgentField);
            if (userAgent != null) {
                adjustConfig.setUserAgent(userAgent);
            }

            // App secret
            Long secretId = AdjustBridgeUtil.fieldToLong(secretIdField);
            Long info1 = AdjustBridgeUtil.fieldToLong(info1Field);
            Long info2 = AdjustBridgeUtil.fieldToLong(info2Field);
            Long info3 = AdjustBridgeUtil.fieldToLong(info3Field);
            Long info4 = AdjustBridgeUtil.fieldToLong(info4Field);

            if (secretId != null && info1 != null && info2 != null
                    && info3 != null && info4 != null) {
                adjustConfig.setAppSecret(secretId, info1, info2, info3, info4);
            }

            // User agent
            Boolean readMobileEquipmentIdentity = AdjustBridgeUtil.fieldToBoolean(readMobileEquipmentIdentityField);
            if (userAgent != null) {
                adjustConfig.setReadMobileEquipmentIdentity(readMobileEquipmentIdentity);
            }

            // Manually call onResume() because web view initialisation will happen a bit delayed.
            // With this delay, it will miss lifecycle callback onResume() initial firing.
            Adjust.onCreate(adjustConfig);
            Adjust.onResume();

            isInitialized = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                application.registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void trackEvent(String adjustEventString) {
        if (!checkInit()) {
            return;
        }

        try {
            JSONObject jsonAdjustEvent = new JSONObject(adjustEventString);

            Object eventTokenField = jsonAdjustEvent.get("eventToken");
            Object revenueField = jsonAdjustEvent.get("revenue");
            Object currencyField = jsonAdjustEvent.get("currency");
            Object callbackParametersField = jsonAdjustEvent.get("callbackParameters");
            Object partnerParametersField = jsonAdjustEvent.get("partnerParameters");
            Object orderIdField = jsonAdjustEvent.get("orderId");

            String eventToken = AdjustBridgeUtil.fieldToString(eventTokenField);
            AdjustEvent adjustEvent = new AdjustEvent(eventToken);

            if (!adjustEvent.isValid()) {
                return;
            }

            Double revenue = AdjustBridgeUtil.fieldToDouble(revenueField);
            String currency = AdjustBridgeUtil.fieldToString(currencyField);
            if (revenue != null && currency != null) {
                adjustEvent.setRevenue(revenue, currency);
            }

            String[] callbackParameters = AdjustBridgeUtil.jsonArrayToArray((JSONArray)callbackParametersField);
            if (callbackParameters != null) {
                for (int i = 0; i < callbackParameters.length; i +=2) {
                    String key = callbackParameters[i];
                    String value = callbackParameters[i+1];

                    adjustEvent.addCallbackParameter(key, value);
                }
            }

            String[] partnerParameters = AdjustBridgeUtil.jsonArrayToArray((JSONArray)partnerParametersField);
            if (partnerParameters != null) {
                for (int i = 0; i < partnerParameters.length; i +=2) {
                    String key = partnerParameters[i];
                    String value = partnerParameters[i+1];

                    adjustEvent.addPartnerParameter(key, value);
                }
            }

            String orderId = AdjustBridgeUtil.fieldToString(orderIdField);
            if (orderId != null) {
                adjustEvent.setOrderId(orderId);
            }

            Adjust.trackEvent(adjustEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void onResume() {
        if (!checkInit()) {
            return;
        }
        Adjust.onResume();
    }

    @JavascriptInterface
    public void onPause() {
        if (!checkInit()) {
            return;
        }
        Adjust.onPause();
    }

    @JavascriptInterface
    public void setEnabled(String isEnabledString) {
        if (!checkInit()) {
            return;
        }

        Boolean isEnabled = AdjustBridgeUtil.fieldToBoolean(isEnabledString);
        if (isEnabled != null) {
            Adjust.setEnabled(isEnabled);
        }
    }

    @JavascriptInterface
    public void isEnabled(String callback) {
        if (!checkInit()) {
            return;
        }

        boolean isEnabled = Adjust.isEnabled();
        AdjustBridgeUtil.execSingleValueCallback(webView, callback, String.valueOf(isEnabled));
    }

    @JavascriptInterface
    public boolean isEnabled() {
        if (!checkInit()) {
            return false;
        }

        return Adjust.isEnabled();
    }

    @JavascriptInterface
    public void appWillOpenUrl(String deeplinkString) {
        if (!checkInit()) {
            return;
        }

        Uri deeplink = null;
        if (deeplinkString != null) {
            deeplink = Uri.parse(deeplinkString);
        }
        Adjust.appWillOpenUrl(deeplink);
    }

    @JavascriptInterface
    public void setReferrer(String referrer) {
        if (!checkInit()) {
            return;
        }

        Adjust.setReferrer(referrer, application.getApplicationContext());
    }

    @JavascriptInterface
    public void setOfflineMode(String isOfflineString) {
        if (!checkInit()) {
            return;
        }

        Boolean isOffline = AdjustBridgeUtil.fieldToBoolean(isOfflineString);

        if (isOffline != null) {
            Adjust.setOfflineMode(isOffline);
        }
    }

    @JavascriptInterface
    public void sendFirstPackages() {
        if (!checkInit()) {
            return;
        }

        Adjust.sendFirstPackages();
    }

    @JavascriptInterface
    public void addSessionCallbackParameter(String key, String value) {
        if (!checkInit()) {
            return;
        }

        Adjust.addSessionCallbackParameter(key, value);
    }

    @JavascriptInterface
    public void addSessionPartnerParameter(String key, String value) {
        if (!checkInit()) {
            return;
        }

        Adjust.addSessionPartnerParameter(key, value);
    }

    @JavascriptInterface
    public void removeSessionCallbackParameter(String key) {
        if (!checkInit()) {
            return;
        }

        Adjust.removeSessionCallbackParameter(key);
    }

    @JavascriptInterface
    public void removeSessionPartnerParameter(String key) {
        if (!checkInit()) {
            return;
        }

        Adjust.removeSessionPartnerParameter(key);
    }

    @JavascriptInterface
    public void resetSessionCallbackParameters() {
        if (!checkInit()) {
            return;
        }

        Adjust.resetSessionCallbackParameters();
    }

    @JavascriptInterface
    public void resetSessionPartnerParameters() {
        if (!checkInit()) {
            return;
        }

        Adjust.resetSessionPartnerParameters();
    }

    @JavascriptInterface
    public void setPushToken(String pushToken) {
        if (!checkInit()) {
            return;
        }

        Adjust.setPushToken(pushToken, application.getApplicationContext());
    }

    @JavascriptInterface
    public void getGoogleAdId(final String callback) {
        if (!checkInit()) {
            return;
        }

        Adjust.getGoogleAdId(application.getApplicationContext(), new OnDeviceIdsRead() {
            @Override
            public void onGoogleAdIdRead(String googleAdId) {
                AdjustBridgeUtil.execSingleValueCallback(webView, callback, googleAdId);
            }
        });
    }

    @JavascriptInterface
    public String getAmazonAdId() {
        if (!checkInit()) {
            return null;
        }

        return Adjust.getAmazonAdId(application.getApplicationContext());
    }

    @JavascriptInterface
    public String getAdid() {
        if (!checkInit()) {
            return null;
        }

        return Adjust.getAdid();
    }

    @JavascriptInterface
    public void getAttribution(final String callback) {
        if (!checkInit()) {
            return;
        }

        AdjustAttribution attribution = Adjust.getAttribution();
        AdjustBridgeUtil.execAttributionCallbackCommand(webView, callback, attribution);
    }

    @JavascriptInterface
    public void setTestOptions(final String testOptionsString) {
        if (!checkInit()) {
            return;
        }

        try {
            AdjustTestOptions adjustTestOptions = new AdjustTestOptions();
            JSONObject jsonAdjustEvent = new JSONObject(testOptionsString);

            Object baseUrlField = jsonAdjustEvent.get("baseUrl");
            Object basePathField = jsonAdjustEvent.get("basePath");
            Object useTestConnectionOptionsField = jsonAdjustEvent.get("useTestConnectionOptions");
            Object timerIntervalInMillisecondsField = jsonAdjustEvent.get("timerIntervalInMilliseconds");
            Object timerStartInMillisecondsField = jsonAdjustEvent.get("timerStartInMilliseconds");
            Object sessionIntervalInMillisecondsField = jsonAdjustEvent.get("sessionIntervalInMilliseconds");
            Object subsessionIntervalInMillisecondsField = jsonAdjustEvent.get("subsessionIntervalInMilliseconds");
            Object teardownField = jsonAdjustEvent.get("teardown");
            Object tryInstallReferrerField = jsonAdjustEvent.get("tryInstallReferrer");

            String baseUrl = AdjustBridgeUtil.fieldToString(baseUrlField);
            if (baseUrl != null) {
                adjustTestOptions.baseUrl = baseUrl;
            }

            String basePath = AdjustBridgeUtil.fieldToString(basePathField);
            if (baseUrl != null) {
                adjustTestOptions.basePath = basePath;
            }

            Boolean useTestConnectionOptions = AdjustBridgeUtil.fieldToBoolean(useTestConnectionOptionsField);
            if (baseUrl != null) {
                adjustTestOptions.useTestConnectionOptions = useTestConnectionOptions;
            }

            Long timerIntervalInMilliseconds = AdjustBridgeUtil.fieldToLong(timerIntervalInMillisecondsField);
            if (baseUrl != null) {
                adjustTestOptions.timerIntervalInMilliseconds = timerIntervalInMilliseconds;
            }

            Long timerStartInMilliseconds = AdjustBridgeUtil.fieldToLong(timerStartInMillisecondsField);
            if (baseUrl != null) {
                adjustTestOptions.timerStartInMilliseconds = timerStartInMilliseconds;
            }

            Long sessionIntervalInMilliseconds = AdjustBridgeUtil.fieldToLong(sessionIntervalInMillisecondsField);
            if (baseUrl != null) {
                adjustTestOptions.sessionIntervalInMilliseconds = sessionIntervalInMilliseconds;
            }

            Long subsessionIntervalInMilliseconds = AdjustBridgeUtil.fieldToLong(subsessionIntervalInMillisecondsField);
            if (baseUrl != null) {
                adjustTestOptions.subsessionIntervalInMilliseconds = subsessionIntervalInMilliseconds;
            }

            Boolean teardown = AdjustBridgeUtil.fieldToBoolean(teardownField);
            if (baseUrl != null) {
                adjustTestOptions.teardown = teardown;
            }

            Boolean tryInstallReferrer = AdjustBridgeUtil.fieldToBoolean(tryInstallReferrerField);
            if (baseUrl != null) {
                adjustTestOptions.tryInstallReferrer = tryInstallReferrer;
            }

            Adjust.setTestOptions(adjustTestOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public void setApplicationContext(Application application) {
        this.application = application;
    }
}
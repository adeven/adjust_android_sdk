package com.adjust.sdk;

import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * Created by pfms on 06/11/14.
 */
public class AdjustConfig {
    Context context;
    String appToken;
    String environment;
    String processName;
    LogLevel logLevel;
    String sdkPrefix;
    boolean eventBufferingEnabled;
    String defaultTracker;
    OnAttributionChangedListener onAttributionChangedListener;
    String referrer;
    long referrerClickTime;
    Boolean deviceKnown;
    Class deepLinkComponent;
    OnEventTrackingSucceededListener onEventTrackingSucceededListener;
    OnEventTrackingFailedListener onEventTrackingFailedListener;
    OnSessionTrackingSucceededListener onSessionTrackingSucceededListener;
    OnSessionTrackingFailedListener onSessionTrackingFailedListener;
    OnDeeplinkResponseListener onDeeplinkResponseListener;
    boolean sendInBackground;
    Double secondsDelayFirstPackages;
    List<Map.Entry<String, String>> sessionCallbackParameters;
    List<Map.Entry<String, String>> sessionPartnerParameters;

    public static final String ENVIRONMENT_SANDBOX = "sandbox";
    public static final String ENVIRONMENT_PRODUCTION = "production";

    private static ILogger getLogger() {
        return AdjustFactory.getLogger();
    }

    public AdjustConfig(Context context, String appToken, String environment) {
        if (!isValid(context, appToken, environment)) {
            return;
        }

        this.context = context.getApplicationContext();
        this.appToken = appToken;
        this.environment = environment;

        // default values
        this.logLevel = LogLevel.INFO;
        this.eventBufferingEnabled = false;
        this.sendInBackground = false;
    }

    public void setEventBufferingEnabled(Boolean eventBufferingEnabled) {
        if (eventBufferingEnabled == null) {
            this.eventBufferingEnabled = false;
            return;
        }
        this.eventBufferingEnabled = eventBufferingEnabled;
    }

    public void setSendInBackground(boolean sendInBackground) {
        this.sendInBackground = sendInBackground;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public void setSdkPrefix(String sdkPrefix) {
        this.sdkPrefix = sdkPrefix;
    }

    public void setProcessName(String processName) { this.processName = processName; }

    public void setDefaultTracker(String defaultTracker) {
        this.defaultTracker = defaultTracker;
    }

    public void setOnAttributionChangedListener(OnAttributionChangedListener onAttributionChangedListener) {
        this.onAttributionChangedListener = onAttributionChangedListener;
    }

    public void setDeviceKnown(boolean deviceKnown) {
        this.deviceKnown = deviceKnown;
    }

    public void setDeepLinkComponent(Class deepLinkComponent) {
        this.deepLinkComponent = deepLinkComponent;
    }

    public void setOnEventTrackingSucceededListener(OnEventTrackingSucceededListener onEventTrackingSucceededListener) {
        this.onEventTrackingSucceededListener = onEventTrackingSucceededListener;
    }

    public void setOnEventTrackingFailedListener(OnEventTrackingFailedListener onEventTrackingFailedListener) {
        this.onEventTrackingFailedListener = onEventTrackingFailedListener;
    }

    public void setOnSessionTrackingSucceededListener(OnSessionTrackingSucceededListener onSessionTrackingSucceededListener) {
        this.onSessionTrackingSucceededListener = onSessionTrackingSucceededListener;
    }

    public void setOnSessionTrackingFailedListener(OnSessionTrackingFailedListener onSessionTrackingFailedListener) {
        this.onSessionTrackingFailedListener = onSessionTrackingFailedListener;
    }

    public void setOnDeeplinkResponseListener(OnDeeplinkResponseListener onDeeplinkResponseListener) {
        this.onDeeplinkResponseListener = onDeeplinkResponseListener;
    }

    public void delayFirstPackages(double secondsDelayFirstPackages) {
        if (secondsDelayFirstPackages < 0) {
            getLogger().error("Delay time cannot be negative");
        }
        this.secondsDelayFirstPackages = secondsDelayFirstPackages;
    }

    public boolean hasAttributionChangedListener() {
        return onAttributionChangedListener != null;
    }

    public boolean hasListener() {
        return onAttributionChangedListener != null
                || onEventTrackingSucceededListener != null
                || onEventTrackingFailedListener != null
                || onSessionTrackingSucceededListener != null
                || onSessionTrackingFailedListener != null;
    }

    public boolean isValid() {
        return appToken != null;
    }

    private boolean isValid(Context context, String appToken, String environment) {
        if (!checkAppToken(appToken)) return false;
        if (!checkEnvironment(environment)) return false;
        if (!checkContext(context)) return false;

        return true;
    }

    private static boolean checkContext(Context context) {
        if (context == null) {
            getLogger().error("Missing context");
            return false;
        }

        if (!Util.checkPermission(context, android.Manifest.permission.INTERNET)) {
            getLogger().error("Missing permission: INTERNET");
            return false;
        }

        return true;
    }

    private static boolean checkAppToken(String appToken) {
        if (appToken == null) {
            getLogger().error("Missing App Token");
            return false;
        }

        if (appToken.length() != 12) {
            getLogger().error("Malformed App Token '%s'", appToken);
            return false;
        }

        return true;
    }

    private static boolean checkEnvironment(String environment) {
        ILogger logger = getLogger();
        if (environment == null) {
            logger.error("Missing environment");
            return false;
        }

        if (environment.equals(AdjustConfig.ENVIRONMENT_SANDBOX)) {
            logger.Assert("SANDBOX: Adjust is running in Sandbox mode. " +
                    "Use this setting for testing. " +
                    "Don't forget to set the environment to `production` before publishing!");
            return true;
        }
        if (environment.equals(AdjustConfig.ENVIRONMENT_PRODUCTION)) {
            logger.Assert(
                    "PRODUCTION: Adjust is running in Production mode. " +
                            "Use this setting only for the build that you want to publish. " +
                            "Set the environment to `sandbox` if you want to test your app!");
            return true;
        }

        logger.error("Unknown environment '%s'", environment);
        return false;
    }
}
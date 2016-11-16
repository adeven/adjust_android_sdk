package com.adjust.sdk;

import android.content.Context;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public final class AdjustFactory {
    private static IPackageHandler packageHandler = null;
    private static IRequestHandler requestHandler = null;
    private static IAttributionHandler attributionHandler = null;
    private static IActivityHandler activityHandler = null;
    private static ILogger logger = null;
    private static HttpsURLConnection httpsURLConnection = null;
    private static ISdkClickHandler sdkClickHandler = null;

    private static long timerInterval = -1;
    private static long timerStart = -1;
    private static long sessionInterval = -1;
    private static long subsessionInterval = -1;
    private static BackoffStrategy sdkClickBackoffStrategy = null;
    private static BackoffStrategy packageHandlerBackoffStrategy = null;
    private static long maxDelayStart = -1;

    private AdjustFactory() {

    }

    public static final class URLGetConnection {
        HttpsURLConnection httpsURLConnection;
        URL url;

        URLGetConnection(final HttpsURLConnection httpsURLConnection, final URL url) {
            this.httpsURLConnection = httpsURLConnection;
            this.url = url;
        }
    }

    public static IPackageHandler getPackageHandler(final ActivityHandler activityHandler,
                                                    final Context context,
                                                    final boolean startsSending) {
        if (packageHandler == null) {
            return new PackageHandler(activityHandler, context, startsSending);
        }
        packageHandler.init(activityHandler, context, startsSending);
        return packageHandler;
    }

    public static IRequestHandler getRequestHandler(final IPackageHandler packageHandler) {
        if (requestHandler == null) {
            return new RequestHandler(packageHandler);
        }
        requestHandler.init(packageHandler);
        return requestHandler;
    }

    public static ILogger getLogger() {
        if (logger == null) {
            // Logger needs to be "static" to retain the configuration throughout the app
            logger = new Logger();
        }
        return logger;
    }

    public static long getTimerInterval() {
        if (timerInterval == -1) {
            return Constants.ONE_MINUTE;
        }
        return timerInterval;
    }

    public static long getTimerStart() {
        if (timerStart == -1) {
            return Constants.ONE_MINUTE;
        }
        return timerStart;
    }

    public static long getSessionInterval() {
        if (sessionInterval == -1) {
            return Constants.THIRTY_MINUTES;
        }
        return sessionInterval;
    }

    public static long getSubsessionInterval() {
        if (subsessionInterval == -1) {
            return Constants.ONE_SECOND;
        }
        return subsessionInterval;
    }

    public static BackoffStrategy getSdkClickBackoffStrategy() {
        if (sdkClickBackoffStrategy == null) {
            return BackoffStrategy.SHORT_WAIT;
        }
        return sdkClickBackoffStrategy;
    }

    public static BackoffStrategy getPackageHandlerBackoffStrategy() {
        if (packageHandlerBackoffStrategy == null) {
            return BackoffStrategy.LONG_WAIT;
        }
        return packageHandlerBackoffStrategy;
    }

    public static IActivityHandler getActivityHandler(final AdjustConfig config) {
        if (activityHandler == null) {
            return ActivityHandler.getInstance(config);
        }
        activityHandler.init(config);
        return activityHandler;
    }

    public static IAttributionHandler getAttributionHandler(final IActivityHandler activityHandler,
                                                            final ActivityPackage attributionPackage,
                                                            final boolean startsSending,
                                                            final boolean hasListener) {
        if (attributionHandler == null) {
            return new AttributionHandler(activityHandler, attributionPackage, startsSending, hasListener);
        }
        attributionHandler.init(activityHandler, attributionPackage, startsSending, hasListener);
        return attributionHandler;
    }

    public static HttpsURLConnection getHttpsURLConnection(final URL url) throws IOException {
        if (AdjustFactory.httpsURLConnection == null) {
            return (HttpsURLConnection) url.openConnection();
        }

        return AdjustFactory.httpsURLConnection;
    }

    public static URLGetConnection getHttpsURLGetConnection(final URL url) throws IOException {
        if (AdjustFactory.httpsURLConnection == null) {
            return new URLGetConnection((HttpsURLConnection) url.openConnection(), url);
        }

        return new URLGetConnection(AdjustFactory.httpsURLConnection, url);
    }

    public static ISdkClickHandler getSdkClickHandler(final boolean startsSending) {
        if (sdkClickHandler == null) {
            return new SdkClickHandler(startsSending);
        }

        sdkClickHandler.init(startsSending);
        return sdkClickHandler;
    }

    public static long getMaxDelayStart() {
        if (maxDelayStart == -1) {
            return Constants.ONE_SECOND * 10; // 10 seconds
        }
        return maxDelayStart;
    }

    public static void setMaxDelayStart(final long maxDelayStart) {
        AdjustFactory.maxDelayStart = maxDelayStart;
    }

    public static void setPackageHandler(final IPackageHandler packageHandler) {
        AdjustFactory.packageHandler = packageHandler;
    }

    public static void setRequestHandler(final IRequestHandler requestHandler) {
        AdjustFactory.requestHandler = requestHandler;
    }

    public static void setLogger(final ILogger logger) {
        AdjustFactory.logger = logger;
    }

    public static void setTimerInterval(final long timerInterval) {
        AdjustFactory.timerInterval = timerInterval;
    }

    public static void setTimerStart(final long timerStart) {
        AdjustFactory.timerStart = timerStart;
    }

    public static void setSessionInterval(final long sessionInterval) {
        AdjustFactory.sessionInterval = sessionInterval;
    }

    public static void setSubsessionInterval(final long subsessionInterval) {
        AdjustFactory.subsessionInterval = subsessionInterval;
    }

    public static void setSdkClickBackoffStrategy(final BackoffStrategy sdkClickBackoffStrategy) {
        AdjustFactory.sdkClickBackoffStrategy = sdkClickBackoffStrategy;
    }

    public static void setPackageHandlerBackoffStrategy(final BackoffStrategy packageHandlerBackoffStrategy) {
        AdjustFactory.packageHandlerBackoffStrategy = packageHandlerBackoffStrategy;
    }

    public static void setActivityHandler(final IActivityHandler activityHandler) {
        AdjustFactory.activityHandler = activityHandler;
    }

    public static void setAttributionHandler(final IAttributionHandler attributionHandler) {
        AdjustFactory.attributionHandler = attributionHandler;
    }

    public static void setHttpsURLConnection(final HttpsURLConnection httpsURLConnection) {
        AdjustFactory.httpsURLConnection = httpsURLConnection;
    }

    public static void setSdkClickHandler(final ISdkClickHandler sdkClickHandler) {
        AdjustFactory.sdkClickHandler = sdkClickHandler;
    }
}

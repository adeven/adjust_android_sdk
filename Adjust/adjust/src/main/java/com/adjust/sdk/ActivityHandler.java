//
//  ActivityHandler.java
//  Adjust
//
//  Created by Christian Wellenbrock on 2013-06-25.
//  Copyright (c) 2013 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.adjust.sdk.Constants.ACTIVITY_STATE_FILENAME;
import static com.adjust.sdk.Constants.ATTRIBUTION_FILENAME;
import static com.adjust.sdk.Constants.CALLBACK_PARAMETERS_FILENAME;
import static com.adjust.sdk.Constants.LOGTAG;
import static com.adjust.sdk.Constants.PARTNER_PARAMETERS_FILENAME;

public class ActivityHandler extends HandlerThread implements IActivityHandler {

    private static long FOREGROUND_TIMER_INTERVAL;
    private static long FOREGROUND_TIMER_START;
    private static long BACKGROUND_TIMER_INTERVAL;
    private static long SESSION_INTERVAL;
    private static long SUBSESSION_INTERVAL;
    private static final String TIME_TRAVEL = "Time travel!";
    private static final String ADJUST_PREFIX = "adjust_";
    private static final String ACTIVITY_STATE_NAME = "Activity state";
    private static final String ATTRIBUTION_NAME = "Attribution";
    private static final String FOREGROUND_TIMER_NAME = "Foreground timer";
    private static final String BACKGROUND_TIMER_NAME = "Background timer";
    private static final String FIRST_SEND_TIMER_NAME = "First send timer";
    private static final String CALLBACK_PARAMETERS_NAME = "Callback parameters";
    private static final String PARTNER_PARAMETERS_NAME = "Partner parameters";

    private Handler internalHandler;
    private IPackageHandler packageHandler;
    private ActivityState activityState;
    private ILogger logger;
    private TimerCycle foregroundTimer;
    private ScheduledExecutorService scheduler;
    private TimerOnce backgroundTimer;
    private InternalState internalState;
    private TimerOnce firstSendTimer;

    private DeviceInfo deviceInfo;
    private AdjustConfig adjustConfig; // always valid after construction
    private AdjustAttribution attribution;
    private IAttributionHandler attributionHandler;
    private ISdkClickHandler sdkClickHandler;

    Map<String, String> sessionCallbackParameters;
    Map<String, String> sessionPartnerParameters;

    public class InternalState {
        boolean enabled;
        boolean offline;
        boolean background;

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isDisabled() {
            return !enabled;
        }

        public boolean isOffline() {
            return offline;
        }

        public boolean isOnline() {
            return !offline;
        }

        public boolean isBackground() {
            return background;
        }

        public boolean isForeground() {
            return !background;
        }
    }

    private ActivityHandler(AdjustConfig adjustConfig) {
        super(LOGTAG, MIN_PRIORITY);
        setDaemon(true);
        start();

        logger = AdjustFactory.getLogger();
        this.internalHandler = new Handler(getLooper());
        internalState = new InternalState();

        // read files to have sync values available
        readAttribution(adjustConfig.context);
        readActivityState(adjustConfig.context);
        readSessionCallbackParameters(adjustConfig.context);
        readSessionPartnerParameters(adjustConfig.context);

        // enabled by default
        if (activityState == null) {
            internalState.enabled = true;
        } else {
            internalState.enabled = activityState.enabled;
        }
        // online by default
        internalState.offline = false;
        // in the background by default
        internalState.background = true;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        // if it's the first launch
        if (activityState == null &&
                // and inital delay is configured
                adjustConfig.secondsDelayFirstPackages != null)
        {
            logger.info("Delay of first session configured");
            firstSendTimer = new TimerOnce(scheduler, new Runnable() {
                @Override
                public void run() {
                    sendFirstPackages();
                }
            }, FIRST_SEND_TIMER_NAME);
        }

        init(adjustConfig);

        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                initInternal();
            }
        });
    }

    @Override
    public void init(AdjustConfig adjustConfig) {
        this.adjustConfig = adjustConfig;
    }

    public static ActivityHandler getInstance(AdjustConfig adjustConfig) {
        if (adjustConfig == null) {
            AdjustFactory.getLogger().error("AdjustConfig missing");
            return null;
        }

        if (!adjustConfig.isValid()) {
            AdjustFactory.getLogger().error("AdjustConfig not initialized correctly");
            return null;
        }

        if (adjustConfig.processName != null) {
            int currentPid = android.os.Process.myPid();
            ActivityManager manager = (ActivityManager) adjustConfig.context.getSystemService(Context.ACTIVITY_SERVICE);

            if (manager == null) {
                return null;
            }

            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (processInfo.pid == currentPid) {
                    if (!processInfo.processName.equalsIgnoreCase(adjustConfig.processName)) {
                        AdjustFactory.getLogger().info("Skipping initialization in background process (%s)", processInfo.processName);
                        return null;
                    }
                    break;
                }
            }
        }

        ActivityHandler activityHandler = new ActivityHandler(adjustConfig);
        return activityHandler;
    }

    @Override
    public void onResume() {
        internalState.background = false;

        checkStartDelay();

        stopBackgroundTimer();

        startForegroundTimer();

        trackSubsessionStart();
    }

    private void checkStartDelay() {
        // first session has already been created
        if (activityState != null) {
            return;
        }

        // it's not configured to start delayed or already finished
        if (firstSendTimer == null) {
            return;
        }

        // the delay has already started
        if (firstSendTimer.getFireIn() > 0) {
            return;
        }

        double secondsDelay = adjustConfig.secondsDelayFirstPackages;
        long milisecondsDelay = (long)secondsDelay * 1000;
        firstSendTimer.startIn(milisecondsDelay);
        logger.info("Waiting %s seconds before starting first session", secondsDelay);
    }

    public void trackSubsessionStart() {
        logger.verbose("Subsession start");
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                startInternal();
            }
        });
    }

    @Override
    public void onPause() {
        internalState.background = true;

        stopForegroundTimer();

        startBackgroundTimer();

        trackSubsessionEnd();
    }

    public void trackSubsessionEnd() {
        logger.verbose("Subsession end");
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                endInternal();
            }
        });
    }

    @Override
    public void trackEvent(final AdjustEvent event) {
        if (activityState == null) {
            logger.warn("Event triggered before first application launch.\n" +
                    "This will trigger the SDK start and an install without user interaction.\n" +
                    "Please check https://github.com/adjust/android_sdk#can-i-trigger-an-event-at-application-launch for more information.");
            trackSubsessionStart();
        }

        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                trackEventInternal(event);
            }
        });
    }

    @Override
    public void finishedTrackingActivity(ResponseData responseData) {
        // redirect session responses to attribution handler to check for attribution information
        if (responseData instanceof SessionResponseData) {
            attributionHandler.checkSessionResponse((SessionResponseData)responseData);
            return;
        }
        // check if it's an event response
        if (responseData instanceof EventResponseData) {
            launchEventResponseTasks((EventResponseData)responseData);
            return;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        // compare with the saved or internal state
        if (!hasChangedState(this.isEnabled(), enabled,
                "Adjust already enabled", "Adjust already disabled")) {
            return;
        }

        internalState.enabled = enabled;

        if (activityState == null) {
            updateStatus(!enabled,
                    "Package handler and attribution handler will start as paused due to the SDK being disabled",
                    "Package and attribution handler will still start as paused due to the SDK being offline",
                    "Package handler and attribution handler will start as active due to the SDK being enabled");
            return;
        }

        activityState.enabled = enabled;
        writeActivityState();

        updateStatus(!enabled,
                "Pausing package handler and attribution handler due to SDK being disabled",
                "Package and attribution handler remain paused due to SDK being offline",
                "Resuming package handler and attribution handler due to SDK being enabled");
    }

    private void updateStatus(boolean pausingState, String pausingMessage,
                              String remainsPausedMessage, String unPausingMessage)
    {
        // it is changing from an active state to a pause state
        if (pausingState) {
            logger.info(pausingMessage);
            updateHandlersStatusAndSend();
            return;
        }

        // it is remaining in a pause state
        if (paused()) {
            logger.info(remainsPausedMessage);
        // it is changing from a pause state to an active state
        } else {
            logger.info(unPausingMessage);
            updateHandlersStatusAndSend();
        }
    }

    private boolean hasChangedState(boolean previousState, boolean newState,
                                    String trueMessage, String falseMessage)
    {
        if (previousState != newState) {
            return true;
        }

        if (previousState) {
            logger.debug(trueMessage);
        } else {
            logger.debug(falseMessage);
        }

        return false;
    }

    @Override
    public void setOfflineMode(boolean offline) {
        // compare with the internal state
        if (!hasChangedState(internalState.isOffline(), offline,
                "Adjust already in offline mode",
                "Adjust already in online mode")) {
            return;
        }

        internalState.offline = offline;

        if (activityState == null) {
            updateStatus(offline,
                    "Package handler and attribution handler will start paused due to SDK being offline",
                    "Package and attribution handler will still start as paused due to SDK being disabled",
                    "Package handler and attribution handler will start as active due to SDK being online");
            return;
        }

        updateStatus(offline,
                "Pausing package and attribution handler to put SDK offline mode",
                "Package and attribution handler remain paused due to SDK being disabled",
                "Resuming package handler and attribution handler to put SDK in online mode");
    }

    @Override
    public boolean isEnabled() {
        if (activityState != null) {
            return activityState.enabled;
        } else {
            return internalState.isEnabled();
        }
    }

    @Override
    public void readOpenUrl(final Uri url, final long clickTime) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                readOpenUrlInternal(url, clickTime);
            }
        });
    }

    @Override
    public boolean updateAttribution(AdjustAttribution attribution) {
        if (attribution == null) {
            return false;
        }

        if (attribution.equals(this.attribution)) {
            return false;
        }

        saveAttribution(attribution);
        return true;
    }

    private void saveAttribution(AdjustAttribution attribution) {
        this.attribution = attribution;
        writeAttribution();
    }

    @Override
    public void setAskingAttribution(boolean askingAttribution) {
        activityState.askingAttribution = askingAttribution;
        writeActivityState();
    }

    @Override
    public void sendReferrer(final String referrer, final long clickTime) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                sendReferrerInternal(referrer, clickTime);
            }
        });
    }

    @Override
    public void launchEventResponseTasks(final EventResponseData eventResponseData) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                launchEventResponseTasksInternal(eventResponseData);
            }
        });
    }

    @Override
    public void launchSessionResponseTasks(final SessionResponseData sessionResponseData) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                launchSessionResponseTasksInternal(sessionResponseData);
            }
        });
    }

    @Override
    public void launchAttributionResponseTasks(final AttributionResponseData attributionResponseData) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                launchAttributionResponseTasksInternal(attributionResponseData);
            }
        });
    }

    @Override
    public void addSessionCallbackParameter(final String key, final String value) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                addSessionCallbackParameterInternal(key, value);
            }
        });
    }

    @Override
    public void addSessionPartnerParameter(final String key, final String value) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                addSessionPartnerParameterInternal(key, value);
            }
        });
    }

    // XXX TODO replace with remove, and reset session callback parameters
    @Override
    public void updateSessionCallbackParameters(final SessionCallbackParametersUpdater sessionCallbackParametersUpdater) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                updateSessionCallbackParametersInternal(sessionCallbackParametersUpdater);
            }
        });
    }

    // XXX TODO replace with remove, and reset session partner parameters
    @Override
    public void updateSessionPartnerParameters(final SessionPartnerParametersUpdater sessionPartnerParametersUpdater) {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                updateSessionPartnerParametersInternal(sessionPartnerParametersUpdater);
            }
        });
    }

    @Override
    public void sendFirstPackages() {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                sendFirstPackagesInternal();
            }
        });
    }

    public ActivityPackage getAttributionPackage() {
        long now = System.currentTimeMillis();
        PackageBuilder attributionBuilder = new PackageBuilder(adjustConfig,
                deviceInfo,
                activityState,
                now);
        return attributionBuilder.buildAttributionPackage();
    }

    public InternalState getInternalState() {
        return internalState;
    }

    private void updateHandlersStatusAndSend() {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                updateHandlersStatusInternal();
                if (!adjustConfig.eventBufferingEnabled) {
                    packageHandler.sendFirstPackage();
                }
            }
        });
    }

    private void foregroundTimerFired() {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                foregroundTimerFiredInternal();
            }
        });
    }

    private void backgroundTimerFired() {
        internalHandler.post(new Runnable() {
            @Override
            public void run() {
                backgroundTimerFiredInternal();
            }
        });
    }

    private void initInternal() {
        FOREGROUND_TIMER_INTERVAL = AdjustFactory.getTimerInterval();
        FOREGROUND_TIMER_START = AdjustFactory.getTimerStart();
        BACKGROUND_TIMER_INTERVAL = AdjustFactory.getTimerInterval();
        SESSION_INTERVAL = AdjustFactory.getSessionInterval();
        SUBSESSION_INTERVAL = AdjustFactory.getSubsessionInterval();

        deviceInfo = new DeviceInfo(adjustConfig.context, adjustConfig.sdkPrefix);

        if (AdjustConfig.ENVIRONMENT_PRODUCTION.equals(adjustConfig.environment)) {
            logger.setLogLevel(LogLevel.ASSERT);
        } else {
            logger.setLogLevel(adjustConfig.logLevel);
        }

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Event buffering is enabled");
        }

        String playAdId = Util.getPlayAdId(adjustConfig.context);
        if (playAdId == null) {
            logger.warn("Unable to get Google Play Services Advertising ID at start time");
        } else {
            logger.debug("Google Play Services Advertising ID read correctly at start time");
        }

        if (adjustConfig.defaultTracker != null) {
            logger.info("Default tracker: '%s'", adjustConfig.defaultTracker);
        }

        if (adjustConfig.referrer != null) {
            sendReferrer(adjustConfig.referrer, adjustConfig.referrerClickTime); // send to background queue to make sure that activityState is valid
        }

        packageHandler = AdjustFactory.getPackageHandler(this, adjustConfig.context, toSend());

        ActivityPackage attributionPackage = getAttributionPackage();
        attributionHandler = AdjustFactory.getAttributionHandler(this,
                attributionPackage,
                toSend(),
                adjustConfig.hasAttributionChangedListener());

        sdkClickHandler = AdjustFactory.getSdkClickHandler(toSend());

        foregroundTimer = new TimerCycle(new Runnable() {
            @Override
            public void run() {
                foregroundTimerFired();
            }
        }, FOREGROUND_TIMER_START, FOREGROUND_TIMER_INTERVAL, FOREGROUND_TIMER_NAME);

        backgroundTimer = new TimerOnce(scheduler, new Runnable() {
            @Override
            public void run() {
                backgroundTimerFired();
            }
        }, BACKGROUND_TIMER_NAME);

        if (adjustConfig.sessionCallbackParameters != null) {
            for (Map.Entry<String, String> entry : adjustConfig.sessionCallbackParameters) {
                addSessionCallbackParameterInternal(entry.getKey(), entry.getValue());
            }
            // release unnecessary structure
            adjustConfig.sessionCallbackParameters = null;
        }

        if (adjustConfig.sessionPartnerParameters != null) {
            for (Map.Entry<String, String> entry : adjustConfig.sessionPartnerParameters) {
                addSessionPartnerParameterInternal(entry.getKey(), entry.getValue());
            }
            // release unnecessary structure
            adjustConfig.sessionCallbackParameters = null;
        }
    }

    private void startInternal() {
        // it shouldn't start if it was disabled after a first session
        if (activityState != null
                && !activityState.enabled) {
            return;
        }

        updateHandlersStatusInternal();

        processSession();

        checkAttributionState();
    }

    private void processSession() {
        long now = System.currentTimeMillis();

        // very first session
        if (activityState == null) {
            activityState = new ActivityState();
            activityState.sessionCount = 1; // this is the first session

            transferSessionPackage(now);
            activityState.resetSessionAttributes(now);
            activityState.enabled = internalState.isEnabled();
            writeActivityState();
            return;
        }

        long lastInterval = now - activityState.lastActivity;

        if (lastInterval < 0) {
            logger.error(TIME_TRAVEL);
            activityState.lastActivity = now;
            writeActivityState();
            return;
        }

        // new session
        if (lastInterval > SESSION_INTERVAL) {
            activityState.sessionCount++;
            activityState.lastInterval = lastInterval;

            transferSessionPackage(now);
            activityState.resetSessionAttributes(now);
            writeActivityState();
            return;
        }

        // new subsession
        if (lastInterval > SUBSESSION_INTERVAL) {
            activityState.subsessionCount++;
            activityState.sessionLength += lastInterval;
            activityState.lastActivity = now;
            logger.verbose("Started subsession %d of session %d",
                    activityState.subsessionCount,
                    activityState.sessionCount);
            writeActivityState();
            return;
        }

        logger.verbose("Time span since last activity too short for a new subsession");
    }

    private void checkAttributionState() {
        if (!checkActivityState(activityState)) { return; }

        // if it's a new session
        if (activityState.subsessionCount <= 1) {
            return;
        }

        // if there is already an attribution saved and there was no attribution being asked
        if (attribution != null && !activityState.askingAttribution) {
            return;
        }

        attributionHandler.getAttribution();
    }

    private void endInternal() {
        // pause sending if it's not allowed to send
        if (!toSend()) {
            pauseSending();
        }

        if (updateActivityState(System.currentTimeMillis())) {
            writeActivityState();
        }
    }

    private void trackEventInternal(AdjustEvent event) {
        if (!checkActivityState(activityState)) return;
        if (!this.isEnabled()) return;
        if (!checkEvent(event)) return;

        long now = System.currentTimeMillis();

        activityState.eventCount++;
        updateActivityState(now);

        PackageBuilder eventBuilder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);

        // XXX no need to be copies
        eventBuilder.sessionCallbackParametersCopy = getSessionCallbackParametersCopy();
        eventBuilder.sessionPartnerParametersCopy = getSessionPartnerParametersCopy();
        ActivityPackage eventPackage = eventBuilder.buildEventPackage(event, this.firstSendTimer != null);
        packageHandler.addPackage(eventPackage);

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Buffered event %s", eventPackage.getSuffix());
        } else {
            packageHandler.sendFirstPackage();
        }

        // if it is in the background and it can send, start the background timer
        if (adjustConfig.sendInBackground && internalState.isBackground()) {
            startBackgroundTimer();
        }

        writeActivityState();
    }

    private void launchEventResponseTasksInternal(final EventResponseData eventResponseData) {
        Handler handler = new Handler(adjustConfig.context.getMainLooper());

        // success callback
        if (eventResponseData.success && adjustConfig.onEventTrackingSucceededListener != null) {
            logger.debug("Launching success event tracking listener");
            // add it to the handler queue
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    adjustConfig.onEventTrackingSucceededListener.onFinishedEventTrackingSucceeded(eventResponseData.getSuccessResponseData());
                }
            };
            handler.post(runnable);

            return;
        }
        // failure callback
        if (!eventResponseData.success && adjustConfig.onEventTrackingFailedListener != null) {
            logger.debug("Launching failed event tracking listener");
            // add it to the handler queue
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    adjustConfig.onEventTrackingFailedListener.onFinishedEventTrackingFailed(eventResponseData.getFailureResponseData());
                }
            };
            handler.post(runnable);

            return;
        }
    }

    private void launchSessionResponseTasksInternal(SessionResponseData sessionResponseData) {
        // use the same handler to ensure that all tasks are executed sequentially
        Handler handler = new Handler(adjustConfig.context.getMainLooper());

        // try to update the attribution
        boolean attributionUpdated = updateAttribution(sessionResponseData.attribution);

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListener(handler);
        }

        // launch Session tracking listener if available
        launchSessionResponseListener(sessionResponseData, handler);

        // if there is any, try to launch the deeplink
        prepareDeeplink(sessionResponseData, handler);
    }

    private void launchSessionResponseListener(final SessionResponseData sessionResponseData, Handler handler) {
        // success callback
        if (sessionResponseData.success && adjustConfig.onSessionTrackingSucceededListener != null) {
            logger.debug("Launching success session tracking listener");
            // add it to the handler queue
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    adjustConfig.onSessionTrackingSucceededListener.onFinishedSessionTrackingSucceeded(sessionResponseData.getSuccessResponseData());
                }
            };
            handler.post(runnable);

            return;
        }
        // failure callback
        if (!sessionResponseData.success && adjustConfig.onSessionTrackingFailedListener != null) {
            logger.debug("Launching failed session tracking listener");
            // add it to the handler queue
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    adjustConfig.onSessionTrackingFailedListener.onFinishedSessionTrackingFailed(sessionResponseData.getFailureResponseData());
                }
            };
            handler.post(runnable);

            return;
        }
    }

    private void launchAttributionResponseTasksInternal(AttributionResponseData responseData) {
        Handler handler = new Handler(adjustConfig.context.getMainLooper());

        // try to update the attribution
        boolean attributionUpdated = updateAttribution(responseData.attribution);

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListener(handler);
        }
    }

    private void launchAttributionListener(Handler handler) {
        if (adjustConfig.onAttributionChangedListener == null) {
            return;
        }
        // add it to the handler queue
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adjustConfig.onAttributionChangedListener.onAttributionChanged(attribution);
            }
        };
        handler.post(runnable);
    }

    private void prepareDeeplink(ResponseData responseData, final Handler handler) {
        if (responseData.jsonResponse == null) {
            return;
        }

        final String deeplink = responseData.jsonResponse.optString("deeplink", null);

        if (deeplink == null) {
            return;
        }

        final Uri location = Uri.parse(deeplink);

        // there is no validation to be made by the user
        if (adjustConfig.onDeeplinkResponseListener == null) {
            launchDeeplink(location, handler, deeplink);
            return;
        }

        // launch deeplink validation by user
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean toLaunchDeeplink = adjustConfig.onDeeplinkResponseListener.launchReceivedDeeplink(location);
                if (toLaunchDeeplink) {
                    launchDeeplink(location, handler, deeplink);
                }
            }
        };
        handler.post(runnable);
    }

    private void launchDeeplink(final Uri location, Handler handler, final String deeplink) {
        final Intent mapIntent;
        if (adjustConfig.deepLinkComponent == null) {
            mapIntent = new Intent(Intent.ACTION_VIEW, location);
        } else {
            mapIntent = new Intent(Intent.ACTION_VIEW, location, adjustConfig.context, adjustConfig.deepLinkComponent);
        }
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mapIntent.setPackage(adjustConfig.context.getPackageName());

        // Verify it resolves
        PackageManager packageManager = adjustConfig.context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(mapIntent, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (!isIntentSafe) {
            logger.error("Unable to open deep link (%s)", deeplink);
            return;
        }

        // add it to the handler queue
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.info("Open deep link (%s)", deeplink);
                adjustConfig.context.startActivity(mapIntent);
            }
        };
        handler.post(runnable);
    }

    private void sendReferrerInternal(String referrer, long clickTime) {
        if (referrer == null || referrer.length() == 0 ) {
            return;
        }
        PackageBuilder clickPackageBuilder = queryStringClickPackageBuilder(referrer);

        if (clickPackageBuilder == null) {
            return;
        }

        clickPackageBuilder.referrer = referrer;
        ActivityPackage clickPackage = clickPackageBuilder.buildClickPackage(Constants.REFTAG, clickTime);

        sdkClickHandler.sendSdkClick(clickPackage);
    }

    private void readOpenUrlInternal(Uri url, long clickTime) {
        if (url == null) {
            return;
        }

        String queryString = url.getQuery();

        if (queryString == null && url.toString().length() > 0) {
            queryString = "";
        }

        PackageBuilder clickPackageBuilder = queryStringClickPackageBuilder(queryString);
        if (clickPackageBuilder == null) {
            return;
        }

        clickPackageBuilder.deeplink = url.toString();
        ActivityPackage clickPackage = clickPackageBuilder.buildClickPackage(Constants.DEEPLINK, clickTime);

        sdkClickHandler.sendSdkClick(clickPackage);
    }

    private PackageBuilder queryStringClickPackageBuilder(String queryString) {
        if (queryString == null) {
            return null;
        }

        Map<String, String> queryStringParameters = new LinkedHashMap<String, String>();
        AdjustAttribution queryStringAttribution = new AdjustAttribution();

        logger.verbose("Reading query string (%s)", queryString);

        String[] queryPairs = queryString.split("&");

        for (String pair : queryPairs) {
            readQueryString(pair, queryStringParameters, queryStringAttribution);
        }

        String reftag = queryStringParameters.remove(Constants.REFTAG);

        long now = System.currentTimeMillis();
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        builder.extraParameters = queryStringParameters;
        builder.attribution = queryStringAttribution;
        builder.reftag = reftag;

        return builder;
    }

    private boolean readQueryString(String queryString,
                                    Map<String, String> extraParameters,
                                    AdjustAttribution queryStringAttribution) {
        String[] pairComponents = queryString.split("=");
        if (pairComponents.length != 2) return false;

        String key = pairComponents[0];
        if (!key.startsWith(ADJUST_PREFIX)) return false;

        String value = pairComponents[1];
        if (value.length() == 0) return false;

        String keyWOutPrefix = key.substring(ADJUST_PREFIX.length());
        if (keyWOutPrefix.length() == 0) return false;

        if (!trySetAttribution(queryStringAttribution, keyWOutPrefix, value)) {
            extraParameters.put(keyWOutPrefix, value);
        }

        return true;
    }

    private boolean trySetAttribution(AdjustAttribution queryStringAttribution,
                                      String key,
                                      String value) {
        if (key.equals("tracker")) {
            queryStringAttribution.trackerName = value;
            return true;
        }

        if (key.equals("campaign")) {
            queryStringAttribution.campaign = value;
            return true;
        }

        if (key.equals("adgroup")) {
            queryStringAttribution.adgroup = value;
            return true;
        }

        if (key.equals("creative")) {
            queryStringAttribution.creative = value;
            return true;
        }

        return false;
    }

    private void updateHandlersStatusInternal() {
        if (toSend()) {
            resumeSending();
        } else {
            pauseSending();
        }
    }

    private void pauseSending() {
        attributionHandler.pauseSending();
        packageHandler.pauseSending();
        sdkClickHandler.pauseSending();
    }

    private void resumeSending() {
        attributionHandler.resumeSending();
        packageHandler.resumeSending();
        sdkClickHandler.resumeSending();
    }

    private boolean updateActivityState(long now) {
        if (!checkActivityState(activityState)) { return false; }

        long lastInterval = now - activityState.lastActivity;
        // ignore late updates
        if (lastInterval > SESSION_INTERVAL) {
            return false;
        }
        activityState.lastActivity = now;

        if (lastInterval < 0) {
            logger.error(TIME_TRAVEL);
        } else {
            activityState.sessionLength += lastInterval;
            activityState.timeSpent += lastInterval;
        }
        return true;
    }

    public static boolean deleteActivityState(Context context) {
        return Util.deleteFile(context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME);
    }

    public static boolean deleteAttribution(Context context) {
        return Util.deleteFile(context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME);
    }

    private void transferSessionPackage(long now) {
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        // XXX no need to be copies
        builder.sessionCallbackParametersCopy = getSessionCallbackParametersCopy();
        builder.sessionPartnerParametersCopy = getSessionPartnerParametersCopy();
        ActivityPackage sessionPackage = builder.buildSessionPackage();
        packageHandler.addPackage(sessionPackage);
        packageHandler.sendFirstPackage();
    }

    private void startForegroundTimer() {
        // don't start the timer if it's disabled, offline or delayed
        if (paused()) {
            return;
        }

        foregroundTimer.start();
    }

    private void stopForegroundTimer() {
        foregroundTimer.suspend();
    }

    private void foregroundTimerFiredInternal() {
        if (paused()) {
            // stop the timer cycle if it's disabled, offline or delayed
            stopForegroundTimer();
            return;
        }

        packageHandler.sendFirstPackage();

        if (updateActivityState(System.currentTimeMillis())) {
            writeActivityState();
        }
    }

    private void startBackgroundTimer() {
        // check if it can send in the background
        if (!toSend()) {
            return;
        }

        // background timer already started
        if (backgroundTimer.getFireIn() > 0) {
            return;
        }

        backgroundTimer.startIn(BACKGROUND_TIMER_INTERVAL);
    }

    private void stopBackgroundTimer() {
        backgroundTimer.cancel();
    }

    private void backgroundTimerFiredInternal() {
        packageHandler.sendFirstPackage();
    }

    private void readActivityState(Context context) {
        try {
            activityState = Util.readObject(context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME, ActivityState.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", ACTIVITY_STATE_NAME, e.getMessage());
            activityState = null;
        }
    }

    private void readAttribution(Context context) {
        try {
            attribution = Util.readObject(context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME, AdjustAttribution.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", ATTRIBUTION_NAME, e.getMessage());
            attribution = null;
        }
    }

    private void readSessionCallbackParameters(Context context) {
        try {
            sessionCallbackParameters = Util.readObject(context,
                    CALLBACK_PARAMETERS_FILENAME,
                    CALLBACK_PARAMETERS_NAME,
                    (Class<Map<String, String>>)((Class)Map.class));
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", CALLBACK_PARAMETERS_NAME, e.getMessage());
            sessionCallbackParameters = null;
        }
    }

    private void readSessionPartnerParameters(Context context) {
        try {
            sessionPartnerParameters = Util.readObject(context,
                    PARTNER_PARAMETERS_FILENAME,
                    PARTNER_PARAMETERS_NAME,
                    (Class<Map<String, String>>)((Class)Map.class));
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", PARTNER_PARAMETERS_NAME, e.getMessage());
            sessionPartnerParameters = null;
        }
    }

    private synchronized void writeActivityState() {
        Util.writeObject(activityState, adjustConfig.context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME);
    }

    private void writeAttribution() {
        Util.writeObject(attribution, adjustConfig.context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME);
    }

    private void writeSessionCallbackParameters() {
        Util.writeObject(sessionCallbackParameters, adjustConfig.context, CALLBACK_PARAMETERS_FILENAME, CALLBACK_PARAMETERS_NAME);
    }

    private void writeSessionPartnerParameters() {
        Util.writeObject(sessionPartnerParameters, adjustConfig.context, PARTNER_PARAMETERS_FILENAME, PARTNER_PARAMETERS_NAME);
    }

    private boolean checkEvent(AdjustEvent event) {
        if (event == null) {
            logger.error("Event missing");
            return false;
        }

        if (!event.isValid()) {
            logger.error("Event not initialized correctly");
            return false;
        }

        return true;
    }

    private boolean checkActivityState(ActivityState activityState) {
        if (activityState == null) {
            logger.error("Missing activity state.");
            return false;
        }
        return true;
    }

    private boolean paused() {
        // is paused if it's offline
        return internalState.isOffline() ||
                // or disabled (activity state first, internal state second)
                !this.isEnabled() ||
                // or in delayed state
                this.firstSendTimer != null;
    }

    private boolean toSend() {
        // if it's offline, disabled, delayed -> don't send
        if (paused()) {
            return false;
        }

        // has the option to send in the background -> is to send
        if (adjustConfig.sendInBackground) {
            return true;
        }

        // doesn't have the option -> depends on being on the background/foreground
        return internalState.isForeground();
    }

    private void addSessionCallbackParameterInternal(String key, String value) {
        if (!Util.isValidParameter(key, "key", "Session callback")) return;
        if (!Util.isValidParameter(value, "value", "Session callback")) return;

        if (sessionCallbackParameters == null) {
            sessionCallbackParameters = new LinkedHashMap<String, String>();
        }

        String previousValue = sessionCallbackParameters.put(key, value);

        if (previousValue != null) {
            logger.warn("key %s was overwritten", key);
        }
        writeSessionCallbackParameters();
    }

    private void addSessionPartnerParameterInternal(String key, String value) {
        if (!Util.isValidParameter(key, "key", "Session partner")) return;
        if (!Util.isValidParameter(value, "value", "Session partner")) return;

        if (sessionPartnerParameters == null) {
            sessionPartnerParameters = new LinkedHashMap<String, String>();
        }

        String previousValue = sessionPartnerParameters.put(key, value);

        if (previousValue != null) {
            logger.warn("key %s was overwritten", key);
        }
        writeSessionPartnerParameters();
    }

    private void updateSessionCallbackParametersInternal(SessionCallbackParametersUpdater sessionCallbackParametersUpdater) {
        if (sessionCallbackParametersUpdater == null) {
            logger.error("Session callback parameters updater is null");
            return;
        }

        sessionCallbackParameters = sessionCallbackParametersUpdater.updateSessionCallbackParameters(sessionCallbackParameters);

        writeSessionCallbackParameters();
    }

    private void updateSessionPartnerParametersInternal(SessionPartnerParametersUpdater sessionPartnerParametersUpdater) {
        if (sessionPartnerParametersUpdater == null) {
            logger.error("Session partner parameters updater is null");
            return;
        }

        sessionPartnerParameters = sessionPartnerParametersUpdater.updateSessionPartnerParameters(sessionPartnerParameters);

        writeSessionPartnerParameters();
    }

    private void sendFirstPackagesInternal() {
        if (firstSendTimer == null) {
            logger.info("Initial delay expired or never configured");
            return;
        }
        // cancel possible still running timer if it was called by user
        firstSendTimer.cancel();
        // no longer delayed
        firstSendTimer = null;
        // update possible
        packageHandler.updateQueue(sessionCallbackParameters, sessionPartnerParameters);
        // update the status and try to send first package
        updateHandlersStatusAndSend();
    }

    private Map<String, String> getSessionCallbackParametersCopy() {
        if (sessionCallbackParameters == null) {
            return null;
        }

        return new HashMap<String, String>(sessionCallbackParameters);
    }

    private Map<String, String> getSessionPartnerParametersCopy() {
        if (sessionPartnerParameters == null) {
            return null;
        }

        return new HashMap<String, String>(sessionPartnerParameters);
    }
}

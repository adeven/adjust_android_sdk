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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.adjust.sdk.Constants.ACTIVITY_STATE_FILENAME;
import static com.adjust.sdk.Constants.ATTRIBUTION_FILENAME;
import static com.adjust.sdk.Constants.SESSION_CALLBACK_PARAMETERS_FILENAME;
import static com.adjust.sdk.Constants.SESSION_PARTNER_PARAMETERS_FILENAME;

public final class ActivityHandler implements IActivityHandler {
    private static long BACKGROUND_TIMER_INTERVAL;
    private static long SESSION_INTERVAL;
    private static long SUBSESSION_INTERVAL;
    private static final String TIME_TRAVEL = "Time travel!";
    private static final String ADJUST_PREFIX = "adjust_";
    private static final String ACTIVITY_STATE_NAME = "Activity state";
    private static final String ATTRIBUTION_NAME = "Attribution";
    private static final String FOREGROUND_TIMER_NAME = "Foreground timer";
    private static final String BACKGROUND_TIMER_NAME = "Background timer";
    private static final String DELAY_START_TIMER_NAME = "Delay Start timer";
    private static final String SESSION_CALLBACK_PARAMETERS_NAME = "Session Callback parameters";
    private static final String SESSION_PARTNER_PARAMETERS_NAME = "Session Partner parameters";

    private CustomScheduledExecutor scheduledExecutor;
    private IPackageHandler packageHandler;
    private ActivityState activityState;
    private ILogger logger;
    private TimerCycle foregroundTimer;
    private TimerOnce backgroundTimer;
    private TimerOnce delayStartTimer;
    private InternalState internalState;

    private DeviceInfo deviceInfo;
    private AdjustConfig adjustConfig; // always valid after construction
    private AdjustAttribution attribution;
    private IAttributionHandler attributionHandler;
    private ISdkClickHandler sdkClickHandler;
    private SessionParameters sessionParameters;

    @Override
    public void teardown(final boolean deleteState) {
        if (backgroundTimer != null) {
            backgroundTimer.teardown();
        }
        if (foregroundTimer != null) {
            foregroundTimer.teardown();
        }
        if (delayStartTimer != null) {
            delayStartTimer.teardown();
        }
        if (scheduledExecutor != null) {
            try {
                scheduledExecutor.shutdownNow();
            } catch (SecurityException se) {

            }
        }
        if (packageHandler != null) {
            packageHandler.teardown(deleteState);
        }
        if (attributionHandler != null) {
            attributionHandler.teardown();
        }
        if (sdkClickHandler != null) {
            sdkClickHandler.teardown();
        }
        if (sessionParameters != null) {
            if (sessionParameters.callbackParameters != null) {
                sessionParameters.callbackParameters.clear();
            }
            if (sessionParameters.partnerParameters != null) {
                sessionParameters.partnerParameters.clear();
            }
        }

        teardownActivityStateS(deleteState);
        teardownAttributionS(deleteState);
        teardownAllSessionParametersS(deleteState);

        packageHandler = null;
        logger = null;
        foregroundTimer = null;
        scheduledExecutor = null;
        backgroundTimer = null;
        delayStartTimer = null;
        internalState = null;
        deviceInfo = null;
        adjustConfig = null;
        attributionHandler = null;
        sdkClickHandler = null;
        sessionParameters = null;
    }

    public static class InternalState {
        private boolean enabled;
        private boolean offline;
        private boolean background;
        boolean delayStart;
        boolean updatePackages;

        public final boolean isEnabled() {
            return enabled;
        }

        public final boolean isDisabled() {
            return !enabled;
        }

        public final boolean isOffline() {
            return offline;
        }

        public final boolean isOnline() {
            return !offline;
        }

        public final boolean isBackground() {
            return background;
        }

        public final boolean isForeground() {
            return !background;
        }

        public final boolean isDelayStart() {
            return delayStart;
        }

        public final boolean isToStartNow() {
            return !delayStart;
        }

        public final boolean isToUpdatePackages() {
            return updatePackages;
        }
    }

    private ActivityHandler(final AdjustConfig adjustConfig) {
        init(adjustConfig);

        // init logger to be available everywhere
        logger = AdjustFactory.getLogger();

        logger.lockLogLevel();

        scheduledExecutor = new CustomScheduledExecutor("ActivityHandler");
        internalState = new InternalState();

        // enabled by default
        internalState.enabled = true;
        // online by default
        internalState.offline = false;
        // in the background by default
        internalState.background = true;
        // delay start not configured by default
        internalState.delayStart = false;
        // does not need to update packages by default
        internalState.updatePackages = false;

        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                initI();
            }
        });
    }

    @Override
    public final void init(final AdjustConfig adjustConfig) {
        this.adjustConfig = adjustConfig;
    }

    public static ActivityHandler getInstance(final AdjustConfig adjustConfig) {
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

        return new ActivityHandler(adjustConfig);
    }

    @Override
    public final void onResume() {
        internalState.background = false;

        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                delayStartI();

                stopBackgroundTimerI();

                startForegroundTimerI();

                logger.verbose("Subsession start");

                startI();
            }
        });
    }

    @Override
    public final void onPause() {
        internalState.background = true;

        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                stopForegroundTimerI();

                startBackgroundTimerI();

                logger.verbose("Subsession end");

                endI();
            }
        });
    }

    @Override
    public final void trackEvent(final AdjustEvent event) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (activityState == null) {
                    logger.warn("Event tracked before first activity resumed.\n" +
                            "If it was triggered in the Application class, it might timestamp or even send an install long before the user opens the app.\n" +
                            "Please check https://github.com/adjust/android_sdk#can-i-trigger-an-event-at-application-launch for more information.");
                    startI();
                }
                trackEventI(event);
            }
        });
    }

    @Override
    public final void finishedTrackingActivity(final ResponseData responseData) {
        // redirect session responses to attribution handler to check for attribution information
        if (responseData instanceof SessionResponseData) {
            attributionHandler.checkSessionResponse((SessionResponseData) responseData);
            return;
        }

        // check if it's an event response
        if (responseData instanceof EventResponseData) {
            launchEventResponseTasksI((EventResponseData) responseData);
        }
    }

    @Override
    public final void setEnabled(final boolean enabled) {
        // compare with the saved or internal state
        if (!hasChangedState(this.isEnabled(), enabled,
                "Adjust already enabled", "Adjust already disabled")) {
            return;
        }

        // save new enabled state in internal state
        internalState.enabled = enabled;

        if (activityState == null) {
            updateStatus(!enabled,
                    "Handlers will start as paused due to the SDK being disabled",
                    "Handlers will still start as paused",
                    "Handlers will start as active due to the SDK being enabled");
            return;
        }

        // save new enabled state in activity state
        Runnable saveEnabled = new Runnable() {
            @Override
            public void run() {
                activityState.enabled = enabled;
            }
        };
        writeActivityStateS(saveEnabled);

        updateStatus(!enabled,
                "Pausing handlers due to SDK being disabled",
                "Handlers remain paused",
                "Resuming handlers due to SDK being enabled");
    }

    private void updateStatus(final boolean pausingState,
                              final String pausingMessage,
                              final String remainsPausedMessage,
                              final String unPausingMessage) {
        // it is changing from an active state to a pause state
        if (pausingState) {
            logger.info(pausingMessage);
        }

        // check if it's remaining in a pause state
        else if (pausedI(false)) {                      // safe to use internal version of paused (read only), can suffer from phantom read but not an issue
            // including the sdk click handler
            if (pausedI(true)) {
                logger.info(remainsPausedMessage);
            } else {
                logger.info(remainsPausedMessage + ", except the Sdk Click Handler");
            }
        } else {
            // it is changing from a pause state to an active state
            logger.info(unPausingMessage);
        }

        updateHandlersStatusAndSend();
    }

    private boolean hasChangedState(final boolean previousState,
                                    final boolean newState,
                                    final String trueMessage,
                                    final String falseMessage) {
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
    public final void setOfflineMode(final boolean offline) {
        // compare with the internal state
        if (!hasChangedState(internalState.isOffline(), offline,
                "Adjust already in offline mode",
                "Adjust already in online mode")) {
            return;
        }

        internalState.offline = offline;

        if (activityState == null) {
            updateStatus(offline,
                    "Handlers will start paused due to SDK being offline",
                    "Handlers will still start as paused",
                    "Handlers will start as active due to SDK being online");
            return;
        }

        updateStatus(offline,
                "Pausing handlers to put SDK offline mode",
                "Handlers remain paused",
                "Resuming handlers to put SDK in online mode");
    }

    @Override
    public final boolean isEnabled() {
        return isEnabledI();
    }

    private boolean isEnabledI() {
        if (activityState != null) {
            return activityState.enabled;
        } else {
            return internalState.isEnabled();
        }
    }

    @Override
    public final void readOpenUrl(final Uri url,
                                  final long clickTime) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                readOpenUrlI(url, clickTime);
            }
        });
    }

    @Override
    public final boolean updateAttributionI(final AdjustAttribution attribution) {
        if (attribution == null) {
            return false;
        }

        if (attribution.equals(this.attribution)) {
            return false;
        }

        this.attribution = attribution;
        writeAttributionI();
        return true;
    }

    @Override
    public final void setAskingAttribution(final boolean askingAttribution) {
        Runnable saveAskingAttribution = new Runnable() {
            @Override
            public void run() {
                activityState.askingAttribution = askingAttribution;
            }
        };

        writeActivityStateS(saveAskingAttribution);
    }

    @Override
    public final void sendReferrer(final String referrer, final long clickTime) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                sendReferrerI(referrer, clickTime);
            }
        });
    }

    @Override
    public final void launchEventResponseTasks(final EventResponseData eventResponseData) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                launchEventResponseTasksI(eventResponseData);
            }
        });
    }

    @Override
    public final void launchSessionResponseTasks(final SessionResponseData sessionResponseData) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                launchSessionResponseTasksI(sessionResponseData);
            }
        });
    }

    @Override
    public final void launchAttributionResponseTasks(final AttributionResponseData attributionResponseData) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                launchAttributionResponseTasksI(attributionResponseData);
            }
        });
    }

    @Override
    public final void sendFirstPackages() {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                sendFirstPackagesI();
            }
        });
    }

    @Override
    public final void addSessionCallbackParameter(final String key, final String value) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                addSessionCallbackParameterI(key, value);
            }
        });
    }

    @Override
    public final void addSessionPartnerParameter(final String key, final String value) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                addSessionPartnerParameterI(key, value);
            }
        });
    }

    @Override
    public final void removeSessionCallbackParameter(final String key) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                removeSessionCallbackParameterI(key);
            }
        });
    }

    @Override
    public final void removeSessionPartnerParameter(final String key) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                removeSessionPartnerParameterI(key);
            }
        });
    }

    @Override
    public final void resetSessionCallbackParameters() {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                resetSessionCallbackParametersI();
            }
        });
    }

    @Override
    public final void resetSessionPartnerParameters() {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                resetSessionPartnerParametersI();
            }
        });
    }

    @Override
    public final void setPushToken(final String token) {
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                setPushTokenI(token);
            }
        });
    }

    public ActivityPackage getAttributionPackageI() {
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
        scheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                updateHandlersStatusAndSendI();
            }
        });
    }

    private void initI() {
        SESSION_INTERVAL = AdjustFactory.getSessionInterval();
        SUBSESSION_INTERVAL = AdjustFactory.getSubsessionInterval();
        // get timer values
        long timerInterval = AdjustFactory.getTimerInterval();
        long timerStart = AdjustFactory.getTimerStart();
        BACKGROUND_TIMER_INTERVAL = AdjustFactory.getTimerInterval();

        // has to be read in the background
        readAttributionI(adjustConfig.context);
        readActivityStateI(adjustConfig.context);

        sessionParameters = new SessionParameters();
        readSessionCallbackParametersI(adjustConfig.context);
        readSessionPartnerParametersI(adjustConfig.context);

        if (activityState != null) {
            internalState.enabled = activityState.enabled;
            internalState.updatePackages = activityState.updatePackages;
        }

        deviceInfo = new DeviceInfo(adjustConfig.context, adjustConfig.sdkPrefix);

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Event buffering is enabled");
        }

        String playAdId = Util.getPlayAdId(adjustConfig.context);
        if (playAdId == null) {
            logger.warn("Unable to get Google Play Services Advertising ID at start time");
            if (deviceInfo.macSha1 == null &&
                    deviceInfo.macShortMd5 == null &&
                    deviceInfo.androidId == null) {
                logger.error("Unable to get any device id's. Please check if Proguard is correctly set with Adjust SDK");
            }
        } else {
            logger.info("Google Play Services Advertising ID read correctly at start time");
        }

        if (adjustConfig.defaultTracker != null) {
            logger.info("Default tracker: '%s'", adjustConfig.defaultTracker);
        }

        foregroundTimer = new TimerCycle(scheduledExecutor,
                new Runnable() {
                    @Override
                    public void run() {
                        foregroundTimerFiredI();
                    }
                }, timerStart, timerInterval, FOREGROUND_TIMER_NAME);

        // create background timer
        if (adjustConfig.sendInBackground) {
            logger.info("Send in background configured");

            backgroundTimer = new TimerOnce(scheduledExecutor, new Runnable() {
                @Override
                public void run() {
                    backgroundTimerFiredI();
                }
            }, BACKGROUND_TIMER_NAME);
        }

        // configure delay start timer
        if (activityState == null &&
                adjustConfig.delayStart != null &&
                adjustConfig.delayStart > 0.0) {
            logger.info("Delay start configured");
            internalState.delayStart = true;
            delayStartTimer = new TimerOnce(scheduledExecutor, new Runnable() {
                @Override
                public void run() {
                    sendFirstPackagesI();
                }
            }, DELAY_START_TIMER_NAME);
        }

        Util.setUserAgent(adjustConfig.userAgent);

        packageHandler = AdjustFactory.getPackageHandler(this, adjustConfig.context, toSendI(false));

        ActivityPackage attributionPackage = getAttributionPackageI();

        attributionHandler = AdjustFactory.getAttributionHandler(this,
                attributionPackage,
                toSendI(false),
                adjustConfig.hasAttributionChangedListener());

        sdkClickHandler = AdjustFactory.getSdkClickHandler(toSendI(true));

        if (isToUpdatePackagesI()) {
            updatePackagesI();
        }

        if (adjustConfig.referrer != null) {
            sendReferrerI(adjustConfig.referrer, adjustConfig.referrerClickTime); // send to background queue to make sure that activityState is valid
        }

        sessionParametersActionsI(adjustConfig.sessionParametersActionsArray);
    }

    private void sessionParametersActionsI(final List<IRunActivityHandler> sessionParametersActionsArray) {
        if (sessionParametersActionsArray == null) {
            return;
        }

        for (IRunActivityHandler sessionParametersAction : sessionParametersActionsArray) {
            sessionParametersAction.run(this);
        }
    }

    private void startI() {
        // it shouldn't start if it was disabled after a first session
        if (activityState != null
                && !activityState.enabled) {
            return;
        }

        updateHandlersStatusAndSendI();

        processSessionI();

        checkAttributionStateI();
    }

    private void processSessionI() {
        long now = System.currentTimeMillis();

        // very first session
        if (activityState == null) {
            activityState = new ActivityState();
            activityState.sessionCount = 1; // this is the first session

            transferSessionPackageI(now);
            activityState.resetSessionAttributes(now);
            activityState.enabled = internalState.isEnabled();
            activityState.updatePackages = internalState.isToUpdatePackages();
            writeActivityStateI();
            return;
        }

        long lastInterval = now - activityState.lastActivity;

        if (lastInterval < 0) {
            logger.error(TIME_TRAVEL);
            activityState.lastActivity = now;
            writeActivityStateI();
            return;
        }

        // new session
        if (lastInterval > SESSION_INTERVAL) {
            activityState.sessionCount++;
            activityState.lastInterval = lastInterval;

            transferSessionPackageI(now);
            activityState.resetSessionAttributes(now);
            writeActivityStateI();
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
            writeActivityStateI();
            return;
        }

        logger.verbose("Time span since last activity too short for a new subsession");
    }

    private void checkAttributionStateI() {
        if (!checkActivityStateI(activityState)) {
            return;
        }

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

    private void endI() {
        // pause sending if it's not allowed to send
        if (!toSendI()) {
            pauseSendingI();
        }

        if (updateActivityStateI(System.currentTimeMillis())) {
            writeActivityStateI();
        }
    }

    private void trackEventI(final AdjustEvent event) {
        if (!checkActivityStateI(activityState)) return;
        if (!isEnabledI()) return;
        if (!checkEventI(event)) return;
        if (!checkOrderIdI(event.orderId)) return;

        long now = System.currentTimeMillis();

        activityState.eventCount++;
        updateActivityStateI(now);

        PackageBuilder eventBuilder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        ActivityPackage eventPackage = eventBuilder.buildEventPackage(event, sessionParameters, internalState.isDelayStart());
        packageHandler.addPackage(eventPackage);

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Buffered event %s", eventPackage.getSuffix());
        } else {
            packageHandler.sendFirstPackage();
        }

        // if it is in the background and it can send, start the background timer
        if (adjustConfig.sendInBackground && internalState.isBackground()) {
            startBackgroundTimerI();
        }

        writeActivityStateI();
    }

    private void launchEventResponseTasksI(final EventResponseData eventResponseData) {
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

    private void launchSessionResponseTasksI(final SessionResponseData sessionResponseData) {
        // use the same handler to ensure that all tasks are executed sequentially
        Handler handler = new Handler(adjustConfig.context.getMainLooper());

        // try to update the attribution
        boolean attributionUpdated = updateAttributionI(sessionResponseData.attribution);

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListenerI(handler);
        }

        // launch Session tracking listener if available
        launchSessionResponseListenerI(sessionResponseData, handler);
    }

    private void launchSessionResponseListenerI(final SessionResponseData sessionResponseData,
                                                final Handler handler) {
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

    private void launchAttributionResponseTasksI(final AttributionResponseData attributionResponseData) {
        Handler handler = new Handler(adjustConfig.context.getMainLooper());

        // try to update the attribution
        boolean attributionUpdated = updateAttributionI(attributionResponseData.attribution);

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListenerI(handler);
        }

        // if there is any, try to launch the deeplink
        prepareDeeplinkI(attributionResponseData.deeplink, handler);
    }

    private void launchAttributionListenerI(final Handler handler) {
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

    private void prepareDeeplinkI(final Uri deeplink, final Handler handler) {
        if (deeplink == null) {
            return;
        }

        logger.info("Deferred deeplink received (%s)", deeplink);

        final Intent deeplinkIntent = createDeeplinkIntentI(deeplink);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean toLaunchDeeplink = true;
                if (adjustConfig.onDeeplinkResponseListener != null) {
                    toLaunchDeeplink = adjustConfig.onDeeplinkResponseListener.launchReceivedDeeplink(deeplink);
                }
                if (toLaunchDeeplink) {
                    launchDeeplinkMain(deeplinkIntent, deeplink);
                }
            }
        };
        handler.post(runnable);
    }

    private Intent createDeeplinkIntentI(final Uri deeplink) {
        Intent mapIntent;
        if (adjustConfig.deepLinkComponent == null) {
            mapIntent = new Intent(Intent.ACTION_VIEW, deeplink);
        } else {
            mapIntent = new Intent(Intent.ACTION_VIEW, deeplink, adjustConfig.context, adjustConfig.deepLinkComponent);
        }
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mapIntent.setPackage(adjustConfig.context.getPackageName());

        return mapIntent;
    }

    private void launchDeeplinkMain(final Intent deeplinkIntent, final Uri deeplink) {
        // Verify it resolves
        PackageManager packageManager = adjustConfig.context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(deeplinkIntent, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (!isIntentSafe) {
            logger.error("Unable to open deferred deep link (%s)", deeplink);
            return;
        }

        // add it to the handler queue
        logger.info("Open deferred deep link (%s)", deeplink);
        adjustConfig.context.startActivity(deeplinkIntent);
    }

    private void sendReferrerI(final String referrer, final long clickTime) {
        if (referrer == null || referrer.length() == 0) {
            return;
        }
        PackageBuilder clickPackageBuilder = queryStringClickPackageBuilderI(referrer);

        if (clickPackageBuilder == null) {
            return;
        }

        clickPackageBuilder.referrer = referrer;
        clickPackageBuilder.clickTime = clickTime;
        ActivityPackage clickPackage = clickPackageBuilder.buildClickPackage(Constants.REFTAG);

        sdkClickHandler.sendSdkClick(clickPackage);
    }

    private void readOpenUrlI(final Uri url, final long clickTime) {
        if (url == null) {
            return;
        }

        String queryString = url.getQuery();

        if (queryString == null && url.toString().length() > 0) {
            queryString = "";
        }

        PackageBuilder clickPackageBuilder = queryStringClickPackageBuilderI(queryString);
        if (clickPackageBuilder == null) {
            return;
        }

        clickPackageBuilder.deeplink = url.toString();
        clickPackageBuilder.clickTime = clickTime;
        ActivityPackage clickPackage = clickPackageBuilder.buildClickPackage(Constants.DEEPLINK);

        sdkClickHandler.sendSdkClick(clickPackage);
    }

    private PackageBuilder queryStringClickPackageBuilderI(final String queryString) {
        if (queryString == null) {
            return null;
        }

        Map<String, String> queryStringParameters = new LinkedHashMap<String, String>();
        AdjustAttribution queryStringAttribution = new AdjustAttribution();

        logger.verbose("Reading query string (%s)", queryString);

        String[] queryPairs = queryString.split("&");

        for (String pair : queryPairs) {
            readQueryStringI(pair, queryStringParameters, queryStringAttribution);
        }

        String reftag = queryStringParameters.remove(Constants.REFTAG);

        long now = System.currentTimeMillis();
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        builder.extraParameters = queryStringParameters;
        builder.attribution = queryStringAttribution;
        builder.reftag = reftag;

        return builder;
    }

    private boolean readQueryStringI(final String queryString,
                                     final Map<String, String> extraParameters,
                                     final AdjustAttribution queryStringAttribution) {
        String[] pairComponents = queryString.split("=");
        if (pairComponents.length != 2) return false;

        String key = pairComponents[0];
        if (!key.startsWith(ADJUST_PREFIX)) return false;

        String value = pairComponents[1];
        if (value.length() == 0) return false;

        String keyWOutPrefix = key.substring(ADJUST_PREFIX.length());
        if (keyWOutPrefix.length() == 0) return false;

        if (!trySetAttributionI(queryStringAttribution, keyWOutPrefix, value)) {
            extraParameters.put(keyWOutPrefix, value);
        }

        return true;
    }

    private boolean trySetAttributionI(final AdjustAttribution queryStringAttribution,
                                       final String key,
                                       final String value) {
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

    private void updateHandlersStatusAndSendI() {
        // check if it should stop sending
        if (!toSendI()) {
            pauseSendingI();
            return;
        }

        resumeSendingI();

        // try to send
        if (!adjustConfig.eventBufferingEnabled) {
            packageHandler.sendFirstPackage();
        }
    }

    private void pauseSendingI() {
        attributionHandler.pauseSending();
        packageHandler.pauseSending();
        // the conditions to pause the sdk click handler are less restrictive
        // it's possible for the sdk click handler to be active while others are paused
        if (!toSendI(true)) {
            sdkClickHandler.pauseSending();
        } else {
            sdkClickHandler.resumeSending();
        }
    }

    private void resumeSendingI() {
        attributionHandler.resumeSending();
        packageHandler.resumeSending();
        sdkClickHandler.resumeSending();
    }

    private boolean updateActivityStateI(final long now) {
        if (!checkActivityStateI(activityState)) {
            return false;
        }

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

    public static boolean deleteActivityState(final Context context) {
        return context.deleteFile(ACTIVITY_STATE_FILENAME);
    }

    public static boolean deleteAttribution(final Context context) {
        return context.deleteFile(ATTRIBUTION_FILENAME);
    }

    public static boolean deleteSessionCallbackParameters(final Context context) {
        return context.deleteFile(SESSION_CALLBACK_PARAMETERS_FILENAME);
    }

    public static boolean deleteSessionPartnerParameters(final Context context) {
        return context.deleteFile(SESSION_PARTNER_PARAMETERS_FILENAME);
    }

    private void transferSessionPackageI(final long now) {
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        ActivityPackage sessionPackage = builder.buildSessionPackage(sessionParameters, internalState.isDelayStart());
        packageHandler.addPackage(sessionPackage);
        packageHandler.sendFirstPackage();
    }

    private void startForegroundTimerI() {
        // don't start the timer if it's disabled
        if (!isEnabledI()) {
            return;
        }

        foregroundTimer.start();
    }

    private void stopForegroundTimerI() {
        foregroundTimer.suspend();
    }

    private void foregroundTimerFiredI() {
        // stop the timer cycle if it's disabled
        if (!isEnabledI()) {
            stopForegroundTimerI();
            return;
        }

        if (toSendI()) {
            packageHandler.sendFirstPackage();
        }

        if (updateActivityStateI(System.currentTimeMillis())) {
            writeActivityStateI();
        }
    }

    private void startBackgroundTimerI() {
        if (backgroundTimer == null) {
            return;
        }

        // check if it can send in the background
        if (!toSendI()) {
            return;
        }

        // background timer already started
        if (backgroundTimer.getFireIn() > 0) {
            return;
        }

        backgroundTimer.startIn(BACKGROUND_TIMER_INTERVAL);
    }

    private void stopBackgroundTimerI() {
        if (backgroundTimer == null) {
            return;
        }

        backgroundTimer.cancel();
    }

    private void backgroundTimerFiredI() {
        if (toSendI()) {
            packageHandler.sendFirstPackage();
        }
    }

    private void delayStartI() {
        // it's not configured to start delayed or already finished
        if (internalState.isToStartNow()) {
            return;
        }

        // the delay has already started
        if (isToUpdatePackagesI()) {
            return;
        }

        // check against max start delay
        double delayStartSeconds = adjustConfig.delayStart != null ? adjustConfig.delayStart : 0.0;
        long maxDelayStartMilli = AdjustFactory.getMaxDelayStart();

        long delayStartMilli = (long) (delayStartSeconds * (double) 1000);
        if (delayStartMilli > maxDelayStartMilli) {
            double maxDelayStartSeconds = maxDelayStartMilli / (double) 1000;
            String delayStartFormatted = Util.SecondsDisplayFormat.format(delayStartSeconds);
            String maxDelayStartFormatted = Util.SecondsDisplayFormat.format(maxDelayStartSeconds);

            logger.warn("Delay start of %s seconds bigger than max allowed value of %s seconds", delayStartFormatted, maxDelayStartFormatted);
            delayStartMilli = maxDelayStartMilli;
            delayStartSeconds = maxDelayStartSeconds;
        }

        String delayStartFormatted = Util.SecondsDisplayFormat.format(delayStartSeconds);
        logger.info("Waiting %s seconds before starting first session", delayStartFormatted);

        delayStartTimer.startIn(delayStartMilli);

        internalState.updatePackages = true;

        if (activityState != null) {
            activityState.updatePackages = true;
            writeActivityStateI();
        }
    }

    private void sendFirstPackagesI() {
        if (internalState.isToStartNow()) {
            logger.info("Start delay expired or never configured");
            return;
        }

        // update packages in queue
        updatePackagesI();
        // no longer is in delay start
        internalState.delayStart = false;
        // cancel possible still running timer if it was called by user
        delayStartTimer.cancel();
        // and release timer
        delayStartTimer = null;
        // update the status and try to send first package
        updateHandlersStatusAndSendI();
    }

    private void updatePackagesI() {
        // update activity packages
        packageHandler.updatePackages(sessionParameters);
        // no longer needs to update packages
        internalState.updatePackages = false;
        if (activityState != null) {
            activityState.updatePackages = false;
            writeActivityStateI();
        }
    }

    private boolean isToUpdatePackagesI() {
        if (activityState != null) {
            return activityState.updatePackages;
        } else {
            return internalState.isToUpdatePackages();
        }
    }

    public void addSessionCallbackParameterI(final String key, final String value) {
        if (!Util.isValidParameter(key, "key", "Session Callback")) return;
        if (!Util.isValidParameter(value, "value", "Session Callback")) return;

        if (sessionParameters.callbackParameters == null) {
            sessionParameters.callbackParameters = new LinkedHashMap<>();
        }

        String oldValue = sessionParameters.callbackParameters.get(key);

        if (value.equals(oldValue)) {
            logger.verbose("Key %s already present with the same value", key);
            return;
        }

        if (oldValue != null) {
            logger.warn("Key %s will be overwritten", key);
        }

        sessionParameters.callbackParameters.put(key, value);

        writeSessionCallbackParametersI();
    }

    public void addSessionPartnerParameterI(final String key, final String value) {
        if (!Util.isValidParameter(key, "key", "Session Partner")) return;
        if (!Util.isValidParameter(value, "value", "Session Partner")) return;

        if (sessionParameters.partnerParameters == null) {
            sessionParameters.partnerParameters = new LinkedHashMap<>();
        }

        String oldValue = sessionParameters.partnerParameters.get(key);

        if (value.equals(oldValue)) {
            logger.verbose("Key %s already present with the same value", key);
            return;
        }

        if (oldValue != null) {
            logger.warn("Key %s will be overwritten", key);
        }

        sessionParameters.partnerParameters.put(key, value);

        writeSessionPartnerParametersI();
    }

    public void removeSessionCallbackParameterI(final String key) {
        if (!Util.isValidParameter(key, "key", "Session Callback")) return;

        if (sessionParameters.callbackParameters == null) {
            logger.warn("Session Callback parameters are not set");
            return;
        }

        String oldValue = sessionParameters.callbackParameters.remove(key);

        if (oldValue == null) {
            logger.warn("Key %s does not exist", key);
            return;
        }

        logger.debug("Key %s will be removed", key);

        writeSessionCallbackParametersI();
    }

    public void removeSessionPartnerParameterI(final String key) {
        if (!Util.isValidParameter(key, "key", "Session Partner")) return;

        if (sessionParameters.partnerParameters == null) {
            logger.warn("Session Partner parameters are not set");
            return;
        }

        String oldValue = sessionParameters.partnerParameters.remove(key);

        if (oldValue == null) {
            logger.warn("Key %s does not exist", key);
            return;
        }

        logger.debug("Key %s will be removed", key);

        writeSessionPartnerParametersI();
    }

    public void resetSessionCallbackParametersI() {
        if (sessionParameters.callbackParameters == null) {
            logger.warn("Session Callback parameters are not set");
        }

        sessionParameters.callbackParameters = null;

        writeSessionCallbackParametersI();
    }

    public void resetSessionPartnerParametersI() {
        if (sessionParameters.partnerParameters == null) {
            logger.warn("Session Partner parameters are not set");
        }

        sessionParameters.partnerParameters = null;

        writeSessionPartnerParametersI();
    }

    private void setPushTokenI(final String token) {
        if (token == null) {
            return;
        }

        if (token.equals(activityState.pushToken)) {
            return;
        }

        long now = System.currentTimeMillis();
        PackageBuilder clickPackageBuilder = new PackageBuilder(adjustConfig, deviceInfo, activityState, now);
        clickPackageBuilder.pushToken = token;

        ActivityPackage clickPackage = clickPackageBuilder.buildClickPackage(Constants.PUSH);
        sdkClickHandler.sendSdkClick(clickPackage);

        // save new push token
        activityState.pushToken = token;
        writeActivityStateI();
    }

    private void readActivityStateI(final Context context) {
        try {
            activityState = Util.readObject(context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME, ActivityState.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", ACTIVITY_STATE_NAME, e.getMessage());
            activityState = null;
        }
    }

    private void readAttributionI(final Context context) {
        try {
            attribution = Util.readObject(context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME, AdjustAttribution.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", ATTRIBUTION_NAME, e.getMessage());
            attribution = null;
        }
    }

    private void readSessionCallbackParametersI(final Context context) {
        try {
            sessionParameters.callbackParameters = Util.readObject(context,
                    SESSION_CALLBACK_PARAMETERS_FILENAME,
                    SESSION_CALLBACK_PARAMETERS_NAME,
                    (Class<Map<String, String>>) (Class) Map.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", SESSION_CALLBACK_PARAMETERS_NAME, e.getMessage());
            sessionParameters.callbackParameters = null;
        }
    }

    private void readSessionPartnerParametersI(final Context context) {
        try {
            sessionParameters.partnerParameters = Util.readObject(context,
                    SESSION_PARTNER_PARAMETERS_FILENAME,
                    SESSION_PARTNER_PARAMETERS_NAME,
                    (Class<Map<String, String>>) (Class) Map.class);
        } catch (Exception e) {
            logger.error("Failed to read %s file (%s)", SESSION_PARTNER_PARAMETERS_NAME, e.getMessage());
            sessionParameters.partnerParameters = null;
        }
    }

    private void writeActivityStateI() {
        writeActivityStateS(null);
    }

    private void writeActivityStateS(final Runnable changeActivityState) {
        synchronized (ActivityState.class) {
            if (activityState == null) {
                return;
            }
            if (changeActivityState != null) {
                changeActivityState.run();
            }
            Util.writeObject(activityState, adjustConfig.context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME);
        }
    }

    private void teardownActivityStateS(final boolean toDelete) {
        synchronized (ActivityState.class) {
            if (activityState == null) {
                return;
            }
            if (toDelete && adjustConfig != null && adjustConfig.context != null) {
                deleteActivityState(adjustConfig.context);
            }
            activityState = null;
        }
    }

    private void writeAttributionI() {
        synchronized (AdjustAttribution.class) {
            if (attribution == null) {
                return;
            }
            Util.writeObject(attribution, adjustConfig.context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME);
        }
    }

    private void teardownAttributionS(final boolean toDelete) {
        synchronized (AdjustAttribution.class) {
            if (attribution == null) {
                return;
            }
            if (toDelete && adjustConfig != null && adjustConfig.context != null) {
                deleteAttribution(adjustConfig.context);
            }
            attribution = null;
        }
    }

    private void writeSessionCallbackParametersI() {
        synchronized (SessionParameters.class) {
            if (sessionParameters == null) {
                return;
            }
            Util.writeObject(sessionParameters.callbackParameters, adjustConfig.context, SESSION_CALLBACK_PARAMETERS_FILENAME, SESSION_CALLBACK_PARAMETERS_NAME);
        }
    }

    private void writeSessionPartnerParametersI() {
        synchronized (SessionParameters.class) {
            if (sessionParameters == null) {
                return;
            }
            Util.writeObject(sessionParameters.partnerParameters, adjustConfig.context, SESSION_PARTNER_PARAMETERS_FILENAME, SESSION_PARTNER_PARAMETERS_NAME);
        }
    }

    private void teardownAllSessionParametersS(final boolean toDelete) {
        synchronized (SessionParameters.class) {
            if (sessionParameters == null) {
                return;
            }
            if (toDelete && adjustConfig != null && adjustConfig.context != null) {
                deleteSessionCallbackParameters(adjustConfig.context);
                deleteSessionPartnerParameters(adjustConfig.context);
            }
            sessionParameters = null;
        }
    }

    private boolean checkEventI(final AdjustEvent event) {
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

    private boolean checkOrderIdI(final String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return true;  // no order ID given
        }

        if (activityState.findOrderId(orderId)) {
            logger.info("Skipping duplicated order ID '%s'", orderId);
            return false; // order ID found -> used already
        }

        activityState.addOrderId(orderId);
        logger.verbose("Added order ID '%s'", orderId);
        // activity state will get written by caller
        return true;
    }

    private boolean checkActivityStateI(final ActivityState activityState) {
        if (activityState == null) {
            logger.error("Missing activity state");
            return false;
        }
        return true;
    }

    private boolean pausedI(final boolean sdkClickHandlerOnly) {
        if (sdkClickHandlerOnly) {
            // sdk click handler is paused if either:
            return internalState.isOffline() ||     // it's offline
                    !isEnabledI();                  // is disabled
        }
        // other handlers are paused if either:
        return internalState.isOffline() ||      // it's offline
                !isEnabledI() ||      // is disabled
                internalState.isDelayStart();       // is in delayed start
    }

    private boolean toSendI() {
        return toSendI(false);
    }

    private boolean toSendI(final boolean sdkClickHandlerOnly) {
        // don't send when it's paused
        if (pausedI(sdkClickHandlerOnly)) {
            return false;
        }

        // has the option to send in the background -> is to send
        if (adjustConfig.sendInBackground) {
            return true;
        }

        // doesn't have the option -> depends on being on the background/foreground
        return internalState.isForeground();
    }
}

//
//  Constants.java
//  Adjust
//
//  Created by keyboardsurfer on 2013-11-08.
//  Copyright (c) 2012-2014 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import java.util.Arrays;
import java.util.List;

/**
 * @author keyboardsurfer
 * @since 8.11.13
 */
public interface Constants {
    int ONE_SECOND = 1000;
    int ONE_MINUTE = 60 * ONE_SECOND;
    int THIRTY_MINUTES = 30 * ONE_MINUTE;
    int ONE_HOUR = 2 * THIRTY_MINUTES;

    int CONNECTION_TIMEOUT = Constants.ONE_MINUTE;
    int SOCKET_TIMEOUT = Constants.ONE_MINUTE;
    int MAX_WAIT_INTERVAL = Constants.ONE_MINUTE;

    String BASE_URL = "https://app.adjust.com";
    String GDPR_URL = "https://gdpr.adjust.com";
    String SUBSCRIPTION_URL = "https://subscription.adjust.com";
    String BASE_URL_IN = "https://app.adjust.net.in";
    String GDPR_URL_IN = "https://gdpr.adjust.net.in";
    String SUBSCRIPTION_URL_IN = "https://subscription.adjust.net.in";
    String BASE_URL_CN = "https://app.adjust.world";
    String GDPR_URL_CN = "https://gdpr.adjust.world";
    String SUBSCRIPTION_URL_CN = "https://subscription.adjust.world";

    String[] FALLBACK_BASE_URLS = {BASE_URL_IN, BASE_URL_CN};
    String[] FALLBACK_GDPR_URLS = {GDPR_URL_IN, GDPR_URL_CN};
    String[] FALLBACK_SUBSCRIPTION_URLS = {SUBSCRIPTION_URL_IN, SUBSCRIPTION_URL_CN};
    String[] FALLBACK_BASE_URLS_IN = {BASE_URL};
    String[] FALLBACK_GDPR_URLS_IN = {GDPR_URL};
    String[] FALLBACK_SUBSCRIPTION_URLS_IN = {SUBSCRIPTION_URL};
    String[] FALLBACK_BASE_URLS_CN = {BASE_URL};
    String[] FALLBACK_GDPR_URLS_CN = {GDPR_URL};
    String[] FALLBACK_SUBSCRIPTION_URLS_CN = {SUBSCRIPTION_URL};

    String[] FALLBACK_IPS_IN = {
            "185.151.204.6",
            "185.151.204.7",
            "185.151.204.8",
            "185.151.204.9",
            "185.151.204.10",
            "185.151.204.11",
            "185.151.204.12",
            "185.151.204.13",
            "185.151.204.14",
            "185.151.204.15" };

    String[] FALLBACK_IPS_CN = {
            "185.151.204.40",
            "185.151.204.41",
            "185.151.204.42",
            "185.151.204.43"
    };

    String SCHEME = "https";
    String AUTHORITY = "app.adjust.com";
    String CLIENT_SDK = "android4.23.0";
    String LOGTAG = "Adjust";
    String REFTAG = "reftag";
    String INSTALL_REFERRER = "install_referrer";
    String REFERRER_API_GOOGLE = "google";
    String REFERRER_API_HUAWEI = "huawei";
    String DEEPLINK = "deeplink";
    String PUSH = "push";
    String THREAD_PREFIX = "Adjust-";

    String ACTIVITY_STATE_FILENAME = "AdjustIoActivityState";
    String ATTRIBUTION_FILENAME = "AdjustAttribution";
    String SESSION_CALLBACK_PARAMETERS_FILENAME = "AdjustSessionCallbackParameters";
    String SESSION_PARTNER_PARAMETERS_FILENAME = "AdjustSessionPartnerParameters";

    String MALFORMED = "malformed";
    String SMALL = "small";
    String NORMAL = "normal";
    String LONG = "long";
    String LARGE = "large";
    String XLARGE = "xlarge";
    String LOW = "low";
    String MEDIUM = "medium";
    String HIGH = "high";
    String REFERRER = "referrer";

    String ENCODING = "UTF-8";
    String MD5 = "MD5";
    String SHA1 = "SHA-1";
    String SHA256 = "SHA-256";

    String CALLBACK_PARAMETERS = "callback_params";
    String PARTNER_PARAMETERS = "partner_params";

    int MAX_INSTALL_REFERRER_RETRIES = 2;

    String FB_AUTH_REGEX = "^(fb|vk)[0-9]{5,}[^:]*://authorize.*access_token=.*";

    String PREINSTALL = "preinstall";
    String SYSTEM_PROPERTIES = "system_properties";
    String SYSTEM_PROPERTIES_REFLECTION = "system_properties_reflection";
    String SYSTEM_PROPERTIES_PATH = "system_properties_path";
    String SYSTEM_PROPERTIES_PATH_REFLECTION = "system_properties_path_reflection";
    String CONTENT_PROVIDER = "content_provider";
    String CONTENT_PROVIDER_INTENT_ACTION = "content_provider_intent_action";
    String FILE_SYSTEM = "file_system";

    String ADJUST_PREINSTALL_SYSTEM_PROPERTY_PREFIX = "adjust.preinstall.";
    String ADJUST_PREINSTALL_SYSTEM_PROPERTY_PATH = "adjust.preinstall.path";
    String ADJUST_PREINSTALL_CONTENT_URI_AUTHORITY = "com.adjust.preinstall";
    String ADJUST_PREINSTALL_CONTENT_URI_PATH = "trackers";
    String ADJUST_PREINSTALL_CONTENT_PROVIDER_INTENT_ACTION = "com.attribution.REFERRAL_PROVIDER";
    String ADJUST_PREINSTALL_FILE_SYSTEM_PATH = "/data/local/tmp/adjust.preinstall";
}

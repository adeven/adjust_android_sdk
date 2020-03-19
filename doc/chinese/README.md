## 摘要

这是Adjust™的安卓SDK包。您可以访问[adjust.com]了解更多有关Adjust™的信息。

Read this in other languages: [English][en-readme], [中文][zh-readme], [日本語][ja-readme], [한국어][ko-readme].

## 目录

### 快速入门

  * [应用示例](#qs-example-apps)
  * [启用设置](#qs-getting-started)
     * [添加SDK至您的项目](#qs-add-sdk)
     * [添加Google Play服务](#qs-gps)
     * [添加权限](#qs-permissions)
     * [Proguard设置](#qs-proguard)
     * [Install referrer](#qs-install-referrer)
        * [Google Play Referrer API](#qs-gpr-api)
        * [Google Play Store intent](#qs-gps-intent)
     * [集成SDK至您的应用](#qs-integrate-sdk)
     * [基本设置](#qs-basic-setup)
         * [原生应用SDK](#qs-basic-setup-native)
         * [Web Views SDK](#qs-basic-setup-web)
     * [会话跟踪](#qs-session-tracking)
        * [API level 14及以上版本](#qs-session-tracking-api-14)
        * [API level 9-13版本](#qs-session-tracking-api-9)
     * [SDK签名](#qs-sdk-signature)
     * [Adjust日志](#qs-adjust-logging)
     * [构建您的应用](#qs-build-the-app)

### 深度链接

   * [深度链接概述](#dl)
   * [标准深度链接场景](#dl-standard)
   * [延迟深度链接场景](#dl-deferred)
   * [通过深度链接的再归因](#dl-reattribution)

### 事件跟踪

   * [事件跟踪](#et-tracking)
   * [收入跟踪](#et-revenue)
   * [收入重复数据删除](#et-revenue-deduplication)
   * [应用收入验证](#et-purchase-verification)

### 自定义参数

   * [自定义参数概述](#cp)
   * [事件参数](#cp-event-parameters)
      * [事件回调参数](#cp-event-callback-parameters)
      * [事件合作伙伴参数](#cp-event-partner-parameters)
      * [事件回调ID](#cp-event-callback-id)
   * [会话参数](#cp-session-parameters)
      * [会话回调参数](#cp-session-callback-parameters)
      * [会话合作伙伴参数](#cp-session-partner-parameters)
      * [延迟启动](#cp-delay-start)
 
 ### 附加功能 
 
   * [推送标签（Push token）—— 卸载跟踪](#af-push-token)
   * [归因回传](#af-attribution-callback)
   * [广告收入跟踪](#af-ad-revenue)
   * [会话和事件回传](#af-session-event-callbacks)
   * [用户归因](#af-user-attribution)
   * [设备ID](#af-device-ids)
      * [Google Play服务广告ID](#af-gps-adid)
      * [Amazon广告ID](#af-amazon-adid)
      * [Adjust设备ID](#af-adid)
   * [预安装跟踪码](#af-pre-installed-trackers)
   * [离线模式](#af-offline-mode)
   * [禁用跟踪](#af-disable-tracking)
   * [事件缓冲](#af-event-buffering)
   * [后台跟踪](#af-background-tracking)
   * [GDPR 的被遗忘权](#af-gdpr-forget-me)

### 测试及故障排查

   * [显示 "Session failed (Ignoring too frequent session. ...)" 出错信息](#tt-session-failed)
   * [我的广播接收器是否能成功获取install referrer?](#tt-broadcast-receiver)
   * [我是否可以在应用激活时触发事件？](#tt-event-at-launch)

### 许可

## 快速入门

### <a id="qs-example-apps"></a>应用示例

[`example-app-java`][example-java] 及 [`example-app-kotlin`][example-kotlin] 目录内有原生安卓应用示例，您可查看[`example-webbridge`目录][example-webbridge]了解使用web view的原生应用示例，以及[`example-tv` 目录][example-tv]了解安卓 TV 应用示例。您还可打开安卓项目查看如何集成Adjust SDK的示例。

### <a id="qs-getting-started"></a>基本集成

我们将向您介绍把Adjust SDK集成到安卓项目的最基本步骤。我们假定您将Android Studio用于安卓开发。集成Adjust SDK所需最低的Android API级别为**9（Gingerbread）**。

### <a id="qs-add-sdk"></a>添加SDK至您的项目

如果您正在使用Maven，请添加下行至您的`build.gradle`文件：

```gradle
implementation 'com.adjust.sdk:adjust-android:4.21.0'
implementation 'com.android.installreferrer:installreferrer:1.0'
```

如果您希望在应用的web view中使用Adjust SDK，请同时包含以下附加依赖项：

```gradle
implementation 'com.adjust.sdk:adjust-android-webbridge:4.21.0'
```

您还可以将Adjust SDK和web view扩展名作为JAR文件来添加，这可从我们的[发布页面][releases]中下载。

### <a id="qs-gps"></a>添加Google Play服务

自2014年8月1日起，在Google Play商店中的应用必须使用[Google广告ID][google_ad_id]以唯一标识每个设备。为了让Adjust SDK能够使用Google广告ID,您必须集成[Google Play服务][google_play_services]。如果您还未完成该集成，请将以下依赖项添加到应用`build.gradle`文件的`dependencies`块中，以便添加对Google Play服务库的依赖：

```gradle
implementation 'com.google.android.gms:play-services-analytics:16.0.4'
```

**注意**:Adjust SDK未与Google Play服务库中`play-services-analytics`的任何特定版本绑定，因此您可自由选择使用最新版本（或您需要的任何版本）。

### <a id="qs-permissions"></a>添加权限

如果您还未在您的`AndroidManifest.xml`文件中添加AdjustSDK所需的以下权限，请进行添加：

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

如果您的**发布目标非Google Play商店**, 请同时添加以下权限：

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
```

### <a id="qs-proguard"></a>Proguard设置

如果您正在使用Proguard,请添加如下代码行至您的Proguard文件:

```
-keep class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }
```

如果您的**发布目标非Google Play商店**,请使用以下 `com.adjust.sdk` Package 规则。

```
-keep public class com.adjust.sdk.** { *; }
```

### <a id="qs-install-referrer"></a>Install referrer

为了将应用安装正确地归因到其来源，Adjust需要关于**install referrer（安装引荐来源）** 的信息。这可以通过 **Google Play Referrer API** 或使用广播接收器（broadcast receiver)捕捉 **Google Play Store intent** 来获得。

**重要**: Google推出Google Play Referrer API旨在提供更加可靠和安全地获取install referrer信息的方法，并帮助归因提供商更有效地对抗点击劫持（click injection)。我们**强烈建议**在您的应用中支持它。相比之下，通过Google Play Store intent获取install referrer的方法则安全性较低。目前该方式与新的Google Play Referrer API并行可用，但未来将被弃用。

#### <a id="qs-gpr-api"></a>Google Play Referrer API

为了让您的应用支持Google Play Referrer API，请确保已经遵循[添加SDK至您的项目](#qs-add-sdk)章节进行了正确设置，并在`build.gradle`文件中添加了如下代码行：

```
implementation 'com.android.installreferrer:installreferrer:1.0'
```

请仔细遵循[Proguard设置](#qs-proguard)说明。请确保您已经添加了说明中所提及的全部规则，尤其是本功能必需的规则：

```
-keep public class com.android.installreferrer.** { *; }
```

**Adjust SDK 4.12.0或以上版本** 已支持该功能。

#### <a id="qs-gps-intent"></a>Google Play Store intent

Google Play Store `INSTALL_REFERRER` intent应该由广播接收器（broadcast receiver）来接收。如果您**未使用自己的广播接收器**来接收 `INSTALL_REFERRER` intent，请在`AndroidManifest.xml`的`application`标签中添加如下`receiver`标签：

```xml
<receiver
    android:name="com.adjust.sdk.AdjustReferrerReceiver"
    android:permission="android.permission.INSTALL_PACKAGES"
    android:exported="true" >
    <intent-filter>
        <action android:name="com.android.vending.INSTALL_REFERRER" />
    </intent-filter>
</receiver>
```

我们使用这个广播接收器来检索install referrer，并将其传送给后端。

如果您已经为`INSTALL_REFERRER`intent使用了不同的广播接收器，请遵循[此说明][referrer]来添加Adjust广播接收器。

### <a id="qs-integrate-sdk"></a>集成SDK至您的应用

我们从设置基本会话跟踪开始。

### <a id="qs-basic-setup"></a>基本设置

如果您希望将SDK集成到原生应用中，请遵照[原生应用SDK](#qs-basic-setup-native)的说明进行操作。如果您需要将SDK集成到使用Web view的应用中，请按照以下[Web Views SDK](#qs-basic-setup-web)说明进行操作。

#### <a id="qs-basic-setup-native"></a>原生应用SDK

我们推荐使用全局安卓[应用程序][android_application]类来初始化SDK。如果您的应用中还没有此类，请按照以下步骤设置：

- 创建一个扩展`Application`的类。
- 打开应用中的`AndroidManifest.xml`文件并找到 `<application>` 元素。
- 添加`android:name`属性，将其设置为您的新应用程序类的名称。

    在此应用示例中，我们将`Application`类命名为`GlobalApplication`，因此manifest文件被设置为：

    ```xml
     <application
       android:name=".GlobalApplication"
       <!-- ...-->
 
    </application>
    ```

- 在您的`Application`类中找到或者创建`onCreate`方法，并添加如下代码来初始化Adjust SDK:

    ```java
    import com.adjust.sdk.Adjust;
    import com.adjust.sdk.AdjustConfig;

    public class GlobalApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();

            String appToken = "{YourAppToken}";
            String environment = AdjustConfig.ENVIRONMENT_SANDBOX;
            AdjustConfig config = new AdjustConfig(this, appToken, environment);
            Adjust.onCreate(config);
        }
    }
    ```

    将`{YourAppToken}`替换为您的应用识别码（app token）。您可以在[控制面板][dashboard]中找到该应用识别码。

    然后，您必须将`environment`（环境模式）设置为Sandbox 或者生产模式：

    ```java
    String environment = AdjustConfig.ENVIRONMENT_SANDBOX;
    String environment = AdjustConfig.ENVIRONMENT_PRODUCTION;
    ```

**重要:** 仅当您或其他人测试您的应用时，该值应设为 `AdjustConfig.ENVIRONMENT_SANDBOX`。在您发布应用之前，请确保将环境改设为 `AdjustConfig.ENVIRONMENT_PRODUCTION`。再次研发和测试时，请将其设回为 `AdjustConfig.ENVIRONMENT_SANDBOX`。

我们按照设置的环境来区分真实流量和来自测试设备的测试流量。非常重要的是，您必须始终根据您的目前状态更新环境！

#### <a id="qs-basic-setup-web"></a>Web Views SDK

在您获取WebView对象的引用后：

- 调用`webView.getSettings().setJavaScriptEnabled(true)`, 以便在web view中启用Javascript。
- 调用`AdjustBridge.registerAndGetInstance(getApplication(), webview)`来启用`AdjustBridgeInstance`的默认实例。
- 这也将Adjust bridge注册为web view的Javascript接口。

完成以上步骤后，您的activity应该如下所示：

```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        AdjustBridge.registerAndGetInstance(getApplication(), webview);
        try {
            webView.loadUrl("file:///android_asset/AdjustExample-WebView.html");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

完成以上步骤后，您已成功地将Adjust bridge添加到您的应用中。Javascript bridge现在可以在Adjust原生安卓SDK和您的页面之间进行通信，它将被加载到Web view中。

在您的HTML文件中，导入位于assets文件夹根目录中的Adjust javascript文件。如果您的HTML文件也存在，请按如下方式导入：

```html
<script type="text/javascript" src="adjust.js"></script>
<script type="text/javascript" src="adjust_event.js"></script>
<script type="text/javascript" src="adjust_config.js"></script>
```

一旦您添加对Javascript文件的引用后，在HTML文件中使用它们来初始化Adjust SDK：

```js
let yourAppToken = '{YourAppToken}';
let environment = AdjustConfig.EnvironmentSandbox;
let adjustConfig = new AdjustConfig(yourAppToken, environment);

Adjust.onCreate(adjustConfig);
```

将`{YourAppToken}`替换为您的应用识别码（app token）。您可以在[控制面板][dashboard]中找到该应用识别码。
 
然后，取决于您处于测试或是生产模式，您必须将`environment`（环境模式）设为以下对应值：

```js
let environment = AdjustConfig.EnvironmentSandbox;
let environment = AdjustConfig.EnvironmentProduction;
```

重要: 仅当您或其他人测试您的应用时，该值应设为`AdjustConfig.EnvironmentSandbox`。在您发布应用之前，请确保将环境改设为`AdjustConfig.EnvironmentProduction`。再次研发和测试时，请将其设回为`AdjustConfig.EnvironmentSandbox`。

我们按照设置的环境来区分真实流量和来自测试设备的测试流量。非常重要的是，您必须始终根据您的目前状态更新环境！

### <a id="qs-session-tracking"></a>会话跟踪

**注意**:此步骤**非常重要**，请**确保您在应用中正确设置它**。设置之后，Adjust SDK将对您的应用进行会话跟踪。

#### <a id="qs-session-tracking-api-14"></a>API level 14及以上版本

- 添加一个私有类(private class)以实现`ActivityLifecycleCallbacks`接口。如果您不能访问该接口，则表示您的应用仅支持安卓API level 14以下版本。在此种情况下，请按照此[说明](#qs-session-tracking-api-9)手动更新每项Activity。如果您在之前已经对应用的每个Activity调用了`Adjust.onResume`和`Adjust.onPause`，请将其全部删除。
- 编辑`onActivityResumed(Activity activity)`方法，添加对`Adjust.onResume()`的调用。编辑`onActivityPaused(Activity activity)`方法，添加对`Adjust.onPause()`的调用。
- 在设置Adjust SDK的位置添加`onCreate()` 方法，并添加调用 `registerActivityLifecycleCallbacks`以及被创建的`ActivityLifecycleCallbacks`类实例。

    ```java
    import com.adjust.sdk.Adjust;
    import com.adjust.sdk.AdjustConfig;

    public class GlobalApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();

            String appToken = "{YourAppToken}";
            String environment = AdjustConfig.ENVIRONMENT_SANDBOX;
            AdjustConfig config = new AdjustConfig(this, appToken, environment);
            Adjust.onCreate(config);

            registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());

            //...
        }

         private static final class AdjustLifecycleCallbacks implements ActivityLifecycleCallbacks {
             @Override
             public void onActivityResumed(Activity activity) {
                 Adjust.onResume();
             }

             @Override
             public void onActivityPaused(Activity activity) {
                 Adjust.onPause();
             }

             //...
         }
      }
    ```

#### <a id="qs-session-tracking-api-9"></a>API level 9-13版本

如果您的应用gradle中的`minSdkVersion`是在`9`至`13`版本之间，您应当考虑至少升级至版本14以简化集成流程。请咨询官方安卓[控制面板][android-dashboard]了解目前市场上广泛使用的主要版本。

为了进行准确的会话跟踪，每当任一Activity重新开始或者暂停时都需要调用某个Adjust SDK方法。否则SDK可能会错过一个会话开始或者会话结束。请遵循以下步骤对您的应用中的**每个Activity**进行正确设置：

- 在Activity的`onResume`方法中调用 `Adjust.onResume`。必要时创建该方法。
- 在Activity的`onPause`方法中调用`Adjust.onPause`。必要时创建该方法。

完成以上步骤后，您的Activity应如下：

```java
import com.adjust.sdk.Adjust;
// ...
public class YourActivity extends Activity {
    protected void onResume() {
        super.onResume();
        Adjust.onResume();
    }
    protected void onPause() {
        super.onPause();
        Adjust.onPause();
    }
    // ...
}
```

对您的应用中的**每个**Activity重复以上步骤。如果您在之后创建新的Activity，也请按照以上步骤设置。取决于您的编码方式，您也可通过设置所有Activitiy的通用超类来实现它。

### <a id="qs-sdk-signature"></a>SDK签名

账户管理员必须启用SDK签名。如果您希望使用该功能，请联系Adjust技术支持(support@adjust.com)。

如果您已经在账户中启用了SDK签名，并可访问Adjust控制面板的应用密钥，请使用以下方法来集成SDK签名到您的应用。

在您的config实例中调用`setAppSecret`来设置应用密钥:

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustConfig config = new AdjustConfig(this, appToken, environment);
config.setAppSecret(secretId, info1, info2, info3, info4);
Adjust.onCreate(config);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustConfig = new AdjustConfig(yourAppToken, environment);
adjustConfig.setAppSecret(secretId, info1, info2, info3, info4);
Adjust.onCreate(adjustConfig);
```
</td>
</tr>
</table>

### <a id="qs-adjust-logging"></a>Adjust日志

您可以增加或减少在测试中看到的日志数量，方法是用以下参数之一来调用config实例上的`setLogLevel`：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
config.setLogLevel(LogLevel.VERBOSE); // enable all logs
config.setLogLevel(LogLevel.DEBUG); // disable verbose logs
config.setLogLevel(LogLevel.INFO); // disable debug logs (default)
config.setLogLevel(LogLevel.WARN); // disable info logs
config.setLogLevel(LogLevel.ERROR); // disable warning logs
config.setLogLevel(LogLevel.ASSERT); // disable error logs
config.setLogLevel(LogLevel.SUPRESS); // disable all logs
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
adjustConfig.setLogLevel(AdjustConfig.LogLevelVerbose); // enable all logs
adjustConfig.setLogLevel(AdjustConfig.LogLevelDebug); // disable verbose logs
adjustConfig.setLogLevel(AdjustConfig.LogLevelInfo); // disable debug logs (default)
adjustConfig.setLogLevel(AdjustConfig.LogLevelWarn); // disable info logs
adjustConfig.setLogLevel(AdjustConfig.LogLevelError); // disable warning logs
adjustConfig.setLogLevel(AdjustConfig.LogLevelAssert); // disable error logs
adjustConfig.setLogLevel(AdjustConfig.LogLevelSuppress); // disable all logs
```
</td>
</tr>
</table>

如果您希望禁用所有日志输出，除了将日志级别设置为抑制以外，您还应该对配置对象使用构建函数，它将获取boolean参数来显示是否应该支持抑制日志级别：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustConfig config = new AdjustConfig(this, appToken, environment, true);
config.setLogLevel(LogLevel.SUPRESS);
Adjust.onCreate(config);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustConfig = new AdjustConfig(yourAppToken, environment, true);
adjustConfig.setLogLevel(AdjustConfig.LogLevelSuppress);
Adjust.onCreate(adjustConfig);
```
</td>
</tr>
</table>

### <a id="qs-build-the-app"></a>构建您的应用

构建并运行您的安卓应用。在`LogCat`查看工具中，您可以设置筛选`tag:Adjust`来隐藏所有其他日志。应用启动后，您可看到以下Adjust日志：`Install tracked`（安装已跟踪）。

## 深度链接

### <a id="dl"></a>深度链接概述

如果您使用的是已启用深度链接的Adjust跟踪链接，则可以接收有关深度链接URL及其内容的相关信息。不论用户的设备上已经安装了应用（标准深度链接场景），或者尚未安装应用（延迟深度链接场景），用户都可与URL交互。在标准深度链接场景中，安卓平台原生支持您获取关于深度链接内容的信息。但是，安卓平台不提供对延迟深度链接场景的支持。在此情况下，Adjust SDK可以帮助您获取有关深度链接内容的信息。

### <a id="dl-standard"></a>标准深度链接场景

如果用户已经安装了您的应用，您希望在用户点击带有`deep_link`（深度链接）参数的Adjust跟踪链接后打开应用，您必须在应用中启用深度链接。请定义**唯一方案名称（unique scheme name）**，并将其分配至您希望在用户点击链接后应用打开时启动的Activity中。这可以通过设置在深度链接被点击应用被打开后您希望启动的Activity类的某个属性来实现。您可在`AndroidManifest.xml`中设置它。请在manifest文件中添加`intent-filter`至您指定的Activity定义，并分配指定的方案名至`android:scheme`属性值：

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|keyboardHidden"
    android:label="@string/app_name"
    android:screenOrientation="portrait">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="adjustExample" />
    </intent-filter>
</activity>
```

如果您希望在跟踪链接被点击后打开应用，请在Adjust跟踪链接的`deep_link`参数中使用指定的Scheme名称。未添加任何深度链接信息的跟踪链接将如下所示：

```
https://app.adjust.com/abc123?deep_link=adjustExample%3A%2F%2F
```

请记住，在URL中的`deep_link`参数值**必须采用URL编码形式**。

如上所述完成应用设置，当用户点击跟踪链接时，您的应用将连带`MainActivity intent`打开。在`MainActivity`类中，您将自动获取关于`deep_link`参数的内容信息。虽然该内容在URL中已编码，但是它在发送给您后**不会被编码**。

`AndroidManifest.xml`文件中的`android:launchMode`Activity设置将决定Activity文件中`deep_link`参数内容的传递位置。请查看[官方安卓文档][android-launch-modes]了解有关`android:launchMode`属性值的更多信息。

通过`Intent`对象发送至您指定的Activity的深度链接内容信息将可能被传递至两个位置—— Activity的`onCreate`或者`onNewIntent`方式。一旦应用被打开，方式被触发后，您将获得在点击URL中被传递至`deep_link`参数中的实际深度链接。您可以使用这些信息为应用增加一些附加逻辑。

您可以按以下两种方式提取深度链接内容：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Intent intent = getIntent();
    Uri data = intent.getData();
    // data.toString() -> This is your deep_link parameter value.
}
```

```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    Uri data = intent.getData();
    // data.toString() -> This is your deep_link parameter value.
}
```

### <a id="dl-deferred"></a>延迟深度链接场景

延迟深度链接场景发生在当用户点击了带有`deep_link`参数的Adjust跟踪链接，但用户在点击时还未在其设备中安装应用。点击链接后，用户将被重定向至Google Play商店下载和安装您的应用。用户首次打开该应用后，`deep_link`参数将被发送至应用。

Adjust SDK 默认自动打开延迟深度链接，无须额外设置。

#### 延迟深度链接回传

如果您希望控制Adjust SDK是否打开延迟深度链接，您可以通过在配置对象中设置回传的方式实现。

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustConfig config = new AdjustConfig(this, appToken, environment);

// Evaluate the deeplink to be launched.
config.setOnDeeplinkResponseListener(new OnDeeplinkResponseListener() {
    @Override
    public boolean launchReceivedDeeplink(Uri deeplink) {
        // ...
        if (shouldAdjustSdkLaunchTheDeeplink(deeplink)) {
            return true;
        } else {
            return false;
        }
    }
});

Adjust.onCreate(config);
```

Adjust SDK从后台接收到关于深度链接内容的信息后，将在监听器内向您传递相关内容信息，并等待您的`boolean`返回值。该返回值决定是否由Adjust SDK启动您从深度链接已分配方案名称的activity（如标准深度链接场景一样）。

如果您的返回值为`true`，我们将启动该activity, 触发在[标准深度链接场景](#dl-standard) 章节中所描述的场景。如果您不希望SDK启动Activity，您可以从监听器返回`false`值，并根据深度链接内容自行决定下一步动作。

</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustConfig = new AdjustConfig(yourAppToken, environment);
adjustConfig.setDeferredDeeplinkCallback(function (deeplink) {});

Adjust.onCreate(adjustConfig);
```

在延迟深度链接场景中，您可以对配置对象作一个额外设置。一旦Adjust SDK获取延迟深度链接信息，您可以选择是否由我们的SDK打开URL。这可通过在配置对象上调用`setOpenDeferredDeeplink`方式来设置：

```js
// ...

function deferredDeeplinkCallback(deeplink) {}

let adjustConfig = new AdjustConfig(yourAppToken, environment);
adjustConfig.setOpenDeferredDeeplink(true);
adjustConfig.setDeferredDeeplinkCallback(deferredDeeplinkCallback);

Adjust.start(adjustConfig);

```

请记住，如果您不设置回传，**在默认情况下Adjust SDK将始终尝试启动URL**。
</td>
</tr>
</table>


### <a id="dl-reattribution"></a>通过深度链接的再归因

Adjust支持您使用深度链接运行再参与推广活动。您可查看我们的[官方文档][reattribution-with-deeplinks]，了解更多相关信息。

如果您正在使用该功能，为了准确地再归因您的用户，您需要在应用中作一个额外回传至Adjust SDK。

一旦您已经在应用中收到深度链接内容信息，请添加回传至`Adjust.appWillOpenUrl(Uri, Context)`方式。添加该回传后，Adjust SDK将尝试查找在深度链接中是否有任何新的归因信息，一旦找到，该信息将被发送至Adjust后台。如果您的用户因为点击带有深度链接内容的Adjust跟踪链接而应该被再归因，您将会看到应用中的[归因回传](#af-attribution-callback)被该用户的新归因信息触发。

请如下示添加至`Adjust.appWillOpenUrl(Uri, Context)`的回传：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Intent intent = getIntent();
    Uri data = intent.getData();
    Adjust.appWillOpenUrl(data, getApplicationContext());
}
```

```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    Uri data = intent.getData();
    Adjust.appWillOpenUrl(data, getApplicationContext());
}
```

**注意:** `Adjust.appWillOpenUrl(Uri)`方法从Android SDK v4.14.0 起已被标记为 **deprecated** (弃用)，请使用`Adjust.appWillOpenUrl(Uri, Context)`方法。

**Web view特别提示：** 您还可如下所示在web view中使用Javascript中的`Adjust.appWillOpenUrl`函数进行调用：

```js
Adjust.appWillOpenUrl(deeplinkUrl);
```

## 事件跟踪

### <a id="et-tracking"></a>事件跟踪

您可以使用Adjust来跟踪应用中的任何事件。假设您想要跟踪具体按钮的每次点击，您必须在[控制面板][dashboard]中创建一个新的事件识别码（Event Token）。例如事件识别码是`abc123`，在按钮的`onClick`方法中，您可以添加以下代码行来跟踪点击：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
</table>

### <a id="et-revenue"></a>收入跟踪

如果您的用户可以通过点击广告或应用内购为您带来收入，您可以按照事件来跟踪这些收入。假设一次点击值一欧分，那么您可以这样来跟踪收入事件：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
adjustEvent.setRevenue(0.01, "EUR");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
adjustEvent.setRevenue(0.01, 'EUR');
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
</table>

当然，这也可以和回调参数相结合。

您设置货币类型后，Adjust将自动把收入转换为您选择的货币类型。阅读这里了解有关[货币转换][currency-conversion]的更多信息。

如果您希望跟踪应用内购，请确保仅在购买完成且商品被购买完后调用`trackEvent`。这一点对于避免跟踪实际上未产生的收入非常重要。

您可以在[事件跟踪指南][event-tracking]中了解更多有关收入和事件跟踪的内容。

### <a id="et-revenue-deduplication"></a>收入重复数据删除

您也可以选择输入交易ID，以避免跟踪重复收入。输入后，最近的十个交易ID将被记录下来，重复交易ID的收入事件将被跳过。这对于应用内购跟踪尤其有用。参见以下例子。

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
adjustEvent.setRevenue(0.01, "EUR");
adjustEvent.setOrderId("{OrderId}");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
adjustEvent.setRevenue(0.01, 'EUR');
adjustEvent.setOrderId('{OrderId}');
Adjust.trackEvent(event);
```
</td>
</tr>
</table>

### <a id="et-purchase-verification"></a>应用收入验证

如果您想要验证应用内购，您可以使用[Adjust的收入验证产品][android-purchase-verification]——我们的服务器端收据验证工具。点击链接了解更多信息。

## 自定义参数

### <a id="cp"></a>自定义参数概述

除了Adjust SDK默认收集的数据点之外，您还可以使用Adjust SDK跟踪并根据需要添加任意数量的自定义值（用户ID、产品ID等）到事件或会话。自定义参数仅作为原始数据提供，且**不会**显示在Adjust控制面板中。

您可对内部使用收集的值使用**回调参数**，与外部合作伙伴共享的值使用**合作伙伴参数**。如果值（如产品ID）被同时用于内部和外部合作伙伴，我们建议您同时使用回传和合作伙伴参数来跟踪该值。

### <a id="cp-event-parameters"></a> 事件参数

### <a id="cp-event-callback-parameters"></a>事件回调参数

您可以在[控制面板][dashboard]中为您的事件登记回调URL。跟踪到事件时，我们会向该URL发送GET请求。您可以在跟踪事件之前调用事件实例的`addCallbackParameter`，向该事件添加回调参数。然后我们会将这些参数添加至您的回调URL。

假设您已经登记URL为`http://www.example.com/callback`，然后如下行跟踪事件：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
adjustEvent.addCallbackParameter("key", "value");
adjustEvent.addCallbackParameter("foo", "bar");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
adjustEvent.addCallbackParameter('key', 'value');
adjustEvent.addCallbackParameter('foo', 'bar');
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
</table>

在这种情况下，我们会跟踪该事件并发送请求至：

```
http://www.example.com/callback?key=value&foo=bar
```

Adjust支持多种可以用作参数值的占位符，例如 `{gps_adid}`。在接下来的回调中，该占位符将被当前设备的Google Play服务ID所替代。同时请注意，我们不保存您的任何定制参数，而只是将它们添加到您的回调中。如果您没有为事件输入回调地址，这些参数甚至不会被读取。

您可以在我们的[回调指南][callbacks-guide]中了解到有关使用URL回调的更多信息，包括可用值的完整列表。

### <a id="cp-event-partner-parameters"></a> 事件合作伙伴参数

您还可以针对您已在Adjust控制面板中激活的渠道合作伙伴添加被发送至合作伙伴的参数。

方式和上述提及的回调参数类似，可以通过调用您的事件实例上的`addPartnerParameter`方法来添加。


<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
adjustEvent.addPartnerParameter("key", "value");
adjustEvent.addPartnerParameter("foo", "bar");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
adjustEvent.addPartnerParameter('key', 'value');
adjustEvent.addPartnerParameter('foo', 'bar');
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
</table>

您可在我们的[特殊合作伙伴指南][special-partners]中了解到有关特殊合作伙伴和集成的更多信息。

### <a id="cp-event-callback-id"></a>事件回调ID

您还可为想要跟踪的每个事件添加自定义字符串ID。此ID将在之后的事件成功和/或事件失败回调中被报告，以便您了解哪些事件跟踪成功或者失败。您可通过调用事件实例上的`setCallbackId`方法来设置此ID:

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustEvent adjustEvent = new AdjustEvent("abc123");
adjustEvent.setCallbackId("Your-Custom-Id");
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adjustEvent = new AdjustEvent('abc123');
adjustEvent.setCallbackId('Your-Custom-Id');
Adjust.trackEvent(adjustEvent);
```
</td>
</tr>
</table>

### <a id="cp-session-parameters"></a>会话参数

一些参数被保存发送到Adjust SDK的每一个**事件**和**会话**中。一旦您已经添加任一这些参数，您无需再每次添加它们，因为这些参数已经被保存至本地。如果您添加同样参数两次，也不会有任何影响。

这些会话参数在Adjust SDK上线之前可以被调用，以确保它们即使在安装时也可被发送。如果您需要在安装同时发送参数，但只有在SDK上线后才能获取所需的值，您可以通过[延迟](#cp-delay-start) Adjust SDK第一次上线以允许该行为。

### <a id="cp-session-callback-parameters">会话回调参数

注册在[事件](#cp-event-callback-parameters)中的相同回调参数也可以被保存发送至Adjust SDK的每一个事件和会话中。

会话回调参数拥有与事件回调参数类似的接口。该参数是通过调用`Adjust.addSessionCallbackParameter(String key, String value)`被添加，而不是通过添加Key和值至事件:

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.addSessionCallbackParameter("foo", "bar");
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.addSessionCallbackParameter('foo', 'bar');
```
</td>
</tr>
</table>

会话回调参数将与被添加至事件的回调参数合并。被添加至事件的回调参数拥有高于会话回调参数的优先级。这意味着，当被添加至事件的回调参数拥有与会话回调参数同样Key时，以被添加至事件的回调参数值为准。

您可以通过传递Key至`Adjust.removeSessionCallbackParameter(String key)`的方式来删除特定会话回调参数。

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.removeSessionCallbackParameter("foo");
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.removeSessionCallbackParameter('foo');
```
</td>
</tr>
</table>

如果您希望删除会话回调参数中所有的Key及相应值，您可以通过`Adjust.resetSessionCallbackParameters()` 方式重置：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.resetSessionCallbackParameters();
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.resetSessionCallbackParameters();
```
</td>
</tr>
</table>


### <a id="cp-session-partner-parameters"></a>会话合作伙伴参数

与[会话回调参数](#cp-session-callback-parameters)的方式一样，会话合作伙伴参数也将被发送至Adjust SDK的每一个事件和会话中。

它们将被传送至渠道合作伙伴，以集成您在Adjust[控制面板][dashboard]上已经激活的模块。

会话合作伙伴参数具有与事件合作伙伴参数类似的接口。该参数是通过调用`Adjust.addSessionPartnerParameter(String key, String value)`被添加，而不是通过添加Key和值至事件:

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.addSessionPartnerParameter("foo", "bar");
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.addSessionPartnerParameter('foo', 'bar');
```
</td>
</tr>
</table>

会话合作伙伴参数将与被添加至事件的合作伙伴参数合并。被添加至事件的合作伙伴参数具有高于会话合作伙伴参数的优先级。这意味着，当被添加至事件的合作伙伴参数拥有与会话合作伙伴参数同样Key时，以被添加至事件的合作伙伴参数值为准。

您可以通过传递Key至`Adjust.removeSessionPartnerParameter(String key)`方式来删除特定的会话合作伙伴参数：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.removeSessionPartnerParameter("foo");
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.removeSessionPartnerParameter('foo');
```
</td>
</tr>
</table>


如果您希望删除会话合作伙伴参数中所有的Key及其相应值，您可以通过`Adjust.resetSessionPartnerParameters()`方式重置：

<table>
<tr>
<td>
<b>Native App SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.resetSessionPartnerParameters();
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.resetSessionPartnerParameters();
```
</td>
</tr>
</table>

### <a id="cp-delay-start"></a>延迟启动

延迟Adjust的SDK启动可以给您的应用一些时间获取被发送至安装的会话参数，如唯一识别码（unique identifiers）等。

通过在config实例中的`setDelayStart`（设置延迟启动）方式以秒为单位设置初始延迟时间：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
adjustConfig.setDelayStart(5.5);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```java
adjustConfig.setDelayStart(5.5);
```
</td>
</tr>
</table>

在此种情况下，Adjust SDK不会在5.5秒内发送初始安装会话以及创建任何事件。在该时间过期后或您同时调用`Adjust.sendFirstPackages()`，每个会话参数将被添加至延迟安装的会话和事件中，Adjust SDK将恢复正常。

**Adjust SDK最长的延迟启动时间为10秒。**


## 附加功能

一旦您已经成功将Adjust SDK集成到您的项目中，您便可以使用以下功能。

### <a id="af-push-token"></a> 推送标签（Push token）—— 卸载跟踪

推送标签适用于Adjust受众分群工具（Audience Builder）和客户回传，是卸载跟踪功能的必需信息。

每当您获取或更新识别码时，请添加以下调用至Adjust，以发送推送标签给我们：

<table>
<tr>
<td>
<b>原生SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.setPushToken(pushNotificationsToken, context);
```

以上添加了`context`的更新后签名让SDK可以涵盖更多的场景，以确保推送标签被发送。因此，我们建议您使用以上签名方式。

尽管如此，我们仍支持之前没有`context`的签名方式。

</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.setPushToken(pushNotificationsToken);
```
</td>
</tr>
</table>

### <a id="af-attribution-callback"></a>归因回传

您可以注册一个监听器（listener），以获取跟踪链接归因变化的通知。由于考虑到归因的不同来源，归因信息无法被同步提供。最简单的方式是创建一个单一的匿名监听器：

请您务必考虑我们的[适用归因数据政策][attribution-data]。

使用Config实例，在启动SDK之前添加匿名监听器：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustConfig config = new AdjustConfig(this, appToken, environment);

config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
    @Override
    public void onAttributionChanged(AdjustAttribution attribution) {}
});

Adjust.onCreate(config);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
function attributionCallback(attribution) {}

// ...

let adjustConfig = new AdjustConfig(yourAppToken, environment);
adjustConfig.setAttributionCallback(attributionCallback);
Adjust.onCreate(adjustConfig);
```
</td>
</tr>
</table>

监听器函数将在SDK接收到最终归因数据后被调用。在监听器函数中，您可以访问`attribution`(归因)参数。这里是其属性的快捷摘要：

- `trackerToken`目前归因的跟踪码字符串
- `trackerName`目前归因的跟踪链接名称字符串
- `network` 目前归因的渠道分组层字符串
- `campaign` 目前归因的推广分组层字符串
- `adgroup` 目前归因的广告组分组层字符串
- `creative` 目前归因的素材分组层字符串
- `clickLabel` 目前归因的点击标签字符串
- `adid` Adjust设备ID字符串

### <a id="af-ad-revenue"></a>广告收入跟踪

**注意**：此功能仅适用于原生 SDK 4.18.0 及以上版本。

您可以通过调用以下方法，使用 Adjust SDK 对广告收入进行跟踪：

<table> 
<tr> 
<td> 
<b>原生应用 SDK</b>
</td> 
</tr> 
<tr> 
<td> 

```java
Adjust.trackAdRevenue(source, payload);
```
</td> 
</tr> 
</table> 

您需要传递的方法参数包括：
- `source` - 表明广告收入来源信息的` String` 对象。
- `payload` - 包含广告收入 JSON 的` JSONObject` 对象。

目前，我们支持以下 `source` 参数值：
- `AD_REVENUE_MOPUB` - 代表 MoPub 广告聚合平台（更多相关信息，请查看 [集成指南][sdk2sdk-mopub]）

### <a id="af-session-event-callbacks"></a>会话和事件回传

您可以注册一个监听器(listener)，以在事件或者会话被跟踪时获取通知。共有四个监听器：一个是用来跟踪成功事件，一个跟踪失败事件，一个跟踪成功会话，一个跟踪失败会话。您可以在创建配置对象后添加任意数量的监听器：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustConfig config = new AdjustConfig(this, appToken, environment);

// Set event success tracking delegate.
config.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
    @Override
    public void onFinishedEventTrackingSucceeded(AdjustEventSuccess eventSuccessResponseData) {
        // ...
    }
});

// Set event failure tracking delegate.
config.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
    @Override
    public void onFinishedEventTrackingFailed(AdjustEventFailure eventFailureResponseData) {
        // ...
    }
});

// Set session success tracking delegate.
config.setOnSessionTrackingSucceededListener(new OnSessionTrackingSucceededListener() {
    @Override
    public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess sessionSuccessResponseData) {
        // ...
    }
});

// Set session failure tracking delegate.
config.setOnSessionTrackingFailedListener(new OnSessionTrackingFailedListener() {
    @Override
    public void onFinishedSessionTrackingFailed(AdjustSessionFailure sessionFailureResponseData) {
        // ...
    }
});

Adjust.onCreate(config);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
function eventSuccessCallback(eventSuccessResponseData) {}
function eventFailureCallback(eventFailureResponseData) {}
function sessionSuccessCallback(sessionSuccessResponseData) {}
function sessionFailureCallback(sessionFailureResponseData) {}

// ...

let adjustConfig = new AdjustConfig(yourAppToken, environment);
adjustConfig.setEventSuccessCallback(eventSuccessCallback);
adjustConfig.setEventFailureCallback(eventFailureCallback);
adjustConfig.setSessionSuccessCallback(sessionSuccessCallback);
adjustConfig.setSessionFailureCallback(sessionFailureCallback);
Adjust.onCreate(adjustConfig);
```
</td>
</tr>
</table>

监听器函数将于SDK发送包（package）到服务器后调用。在监听器函数中，您可以访问专为监听器所设的响应数据对象。成功会话的响应数据对象字段摘要如下：

- `message` 服务器信息字符串或者SDK纪录的错误信息
- `timestamp` 服务器的时间戳字符串
- `adid` Adjust提供的设备唯一识别码字符串
- `jsonResponse` JSON对象及服务器响应

两个事件响应数据对象均包含：

- 如果跟踪的包是一个事件，`eventToken` 代表事件识别码。
- `callbackId` 为事件对象设置的自定义[回调ID](#cp-event-callback-id)。

事件和会话跟踪不成功的对象也均包含：

- `willRetry` Boolean表示稍后将再尝试发送数据包。

### <a id="af-user-attribution"></a>用户归因

正如我们在[归因回传](#af-attribution-callback)章节中所描述的，每当归因信息更改时都会触发此回调。如果您想要在任何其他时间访问用户当前归因值的信息，您可以通过对`Adjust`实例调用如下方式来实现：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
AdjustAttribution attribution = Adjust.getAttribution();
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let attribution = Adjust.getAttribution();
```
</td>
</tr>
</table>

**注意**：此调用只能在Adjust SDK 4.11.0和以上版本中进行。

**注意**：只有在Adjust后台跟踪到应用安装以及归因回传被触发后，您才能获取有关当前归因的信息。因此，在SDK被初始化以及归因回传被触发之前，您将**无法**访问用户归因值。

### <a id="af-device-ids"></a>设备ID

Adjust SDK支持您获取一些设备ID。

### <a id="af-gps-adid"></a>Google Play服务广告ID

某些服务（如Google Analytics）要求您协调设备及客户ID以避免重复报告。

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>
 
如果您需要获取Google广告ID，则存在一个限制，即仅限于在后台线程里读取。如您调用带有背景关系（context）的`getGoogleAdId`函数及`OnDeviceIdsRead`实例，则可在任何情况下都可成功获取ID：

```java
Adjust.getGoogleAdId(this, new OnDeviceIdsRead() {
    @Override
    public void onGoogleAdIdRead(String googleAdId) {}
});
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>
 
如需获取设备的Google广告设备ID，您必须传递一个回调函数到`Adjust.getGoogleAdId`，其将在参数中接收Google广告ID，如下所示：

```js
Adjust.getGoogleAdId(function(googleAdId) {
    // ...
});
```
</td>
</tr>
</table>

### <a id="af-amazon-adid"></a>Amazon广告ID

如果您需要获取Amazon广告ID，请在`Adjust`实例上调用以下方法：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
String amazonAdId = Adjust.getAmazonAdId(context);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let amazonAdId = Adjust.getAmazonAdId();
```
</td>
</tr>
</table>

### <a id="af-adid"></a>Adjust设备ID

Adjust后台将为每一台安装了您应用的设备生成一个唯一的**Adjust设备ID** (**adid**)。您可在`Adjust`实例上调用以下方法来获取该ID:

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
String adid = Adjust.getAdid();
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
let adid = Adjust.getAdid();
```
</td>
</tr>
</table>

**注意**: 此调用只能在Adjust SDK 4.11.0和以上版本中进行。

**注意**: 只有在Adjust后台跟踪到应用安装后，您才能获取**adid**的相关信息。自此之后，Adjust SDK已经拥有关于设备**adid**的信息，您可以使用此方法来访问它。因此，在SDK被初始化以及您的应用安装被成功跟踪之前，您将**无法访问adid**。

### <a id="af-pre-installed-trackers"></a>预安装跟踪码

如果您希望使用Adjust SDK来识别已在设备中预安装您的应用的用户，请执行以下步骤。

- 在[控制面板][dashboard]中创建一个新的跟踪码。
- 打开应用委托，并在`AdjustConfig`实例中添加设置默认跟踪码:

  <table>
  <tr>
  <td>
  <b>原生应用SDK</b>
  </td>
  </tr>
  <tr>
  <td>

  ```java
  adjustConfig.setDefaultTracker("{TrackerToken}");
  ```
  </td>
  </tr>
  <tr>
  <td>
  <b>Web View SDK</b>
  </td>
  </tr>
  <tr>
  <td>

  ```js
  adjustConfig.setDefaultTracker('{TrackerToken}');
  ```
  </td>
  </tr>
  </table>

- 用您在步骤1中创建的跟踪码替换`{TrackerToken}`（跟踪码）。请注意，控制面板中显示的是跟踪URL(包括`http://app.adjust.com/`)。在源代码中，您应该仅指定六个字符的识别码，而不是整个URL。
 
- 创建并运行您的应用。您应该可以在应用日志输出中看到如下行：

  ```
  Default tracker: 'abc123'
  ```

### <a id="af-offline-mode"></a>离线模式

您可以把Adjust SDK设置离线模式，以暂停发送数据到我们的服务器，但仍然继续跟踪及保存数据并在之后发送。当设为离线模式时，所有数据将存放于一个文件中，所以请注意不要于离线模式触发太多事件。

您可以调用`setOfflineMode`，启用参数为`true`，以激活离线模式。

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.setOfflineMode(true);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.setOfflineMode(true);
```
</td>
</tr>
</table>

相反地，您可以调用`setOfflineMode`，启用参数为`false`，以终止离线模式。当Adjust SDK回到在线模式时，所有被保存的数据将被发送到我们的服务器，并保留正确的时间信息。

跟禁用跟踪设置不同的是，此设置在会话之间将**不被保存**。这意味着，即使应用在离线模式时被终止，每当SDK启动时都必定处于在线模式。

### <a id="af-disable-tracking"></a>禁用跟踪

您可以通过调用`setEnabled`，启用参数为`false`，来禁用Adjust SDK的跟踪功能。**该设置在会话间保存**。

<table>
<tr>
<td>
<b>Native App SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.setEnabled(false);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.setEnabled(false);
```
</td>
</tr>
</table>

您可以通过调用`isEnabled`函数来查看Adjust SDK目前是否被启用。您始终可以通过调用`setEnabled`，启用参数为`true`，来激活Adjust SDK。

### <a id="af-event-buffering"></a>事件缓冲

如果您的应用大量使用事件跟踪，您可能想要延迟部分网络请求，以便按分钟成批发送这些请求。您可以通过配置实例启用事件缓冲：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
adjustConfig.setEventBufferingEnabled(true);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
adjustConfig.setEventBufferingEnabled(true);
```
</td>
</tr>
</table>

### <a id="af-background-tracking"></a>后台跟踪

Adjust SDK的默认行为是当应用处于后台时暂停发送网络请求。您可以通过配置实例更改该设置：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
adjustConfig.setSendInBackground(true);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
adjustConfig.setSendInBackground(true);
```
</td>
</tr>
</table>

### <a id="af-gdpr-forget-me"></a>GDPR 的被遗忘权

根据欧盟的《一般数据保护条例》(GDPR) 第17条规定，用户行使被遗忘权时，您可以通知Adjust。调用以下方法，Adjust SDK将会收到指示向Adjust后端传达用户选择被遗忘的信息：

<table>
<tr>
<td>
<b>原生应用SDK</b>
</td>
</tr>
<tr>
<td>

```java
Adjust.gdprForgetMe(context);
```
</td>
</tr>
<tr>
<td>
<b>Web View SDK</b>
</td>
</tr>
<tr>
<td>

```js
Adjust.gdprForgetMe();
```
</td>
</tr>
</table>

收到此信息后，Adjust将清除该用户数据，并且Adjust SDK将停止跟踪该用户。以后不会再向Adjust发送来自此设备的请求。

请注意，即便在测试环境中，该决定也是永久且不可逆转的。

## 测试及故障排查

### <a id="tt-session-failed"></a>显示"Session failed (Ignoring too frequent session. ...)" 出错信息

该错误信息一般于测试安装时出现的。单单卸载然后再安装应用并不足以触发新的安装。由于我们服务器已经有该设备的纪录，服务器只会判断为SDK失去了本地的会话数据而忽视该错误信息。

虽然该行为可能在测试阶段比较麻烦，但却是必须的，目的是为了让Sandbox尽量等同于生产行为。

如果您拥有应用编辑级别（或更高级别）的访问权限，您可以使用我们的[测试控制台][testing_console]直接从Adjust控制面板重置任何设备的应用会话数据。

一旦设备被正确遗忘，测试控制台将返回`Forgot device`。如果设备已经被遗忘（或者值不正确），链接将返回`Advertising ID not found`。

遗忘设备将不会逆转GDPR遗忘调用。

如果您当前的包允许访问，您还可以使用我们的[开发者API][dev_api]检查和忘记设备。

### <a id="tt-broadcast-receiver"></a>我的广播接收器是否能成功获取install referrer？

如果您按照[指南](#qs-gps-intent)描述的步骤来设置, 广播接收器就应该可以将install referrer发送到SDK以及我们的服务器。

为了测试设置是否正确，您可以手动触发一个install referrer测试。用您的应用ID替换`com.your.appid`，然后使用Android Studio包括的[adb](http://developer.android.com/tools/help/adb.html)工具，执行以下命令。

```
adb shell am broadcast -a com.android.vending.INSTALL_REFERRER -n com.your.appid/com.adjust.sdk.AdjustReferrerReceiver --es "referrer" "adjust_reftag%3Dabc1234%26tracking_id%3D123456789%26utm_source%3Dnetwork%26utm_medium%3Dbanner%26utm_campaign%3Dcampaign"
```

如果您已经按照该[指南][referrer]设置了另一个广播接收器使用`INSTALL_REFERRER`intent，请用`com.adjust.sdk.AdjustReferrerReceiver`替换您设置的广播接收器。

您也可以删除`-n com.your.appid/com.adjust.sdk.AdjustReferrerReceiver`参数，以让设备上的所有应用接收`INSTALL_REFERRER`intent。

如果您将日志量设置为`verbose`，您应该可以通过读取referrer查看日志：

```
V/Adjust: Reading query string (adjust_reftag=abc1234&tracking_id=123456789&utm_source=network&utm_medium=banner&utm_campaign=campaign) from reftag
```

以及添加点击包(click package）到SDK包处理器：

```
V/Adjust: Path:      /sdk_click
    ClientSdk: android4.6.0
    Parameters:
      app_token        abc123abc123
      click_time       yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z
      created_at       yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z
      environment      sandbox
      gps_adid         12345678-0abc-de12-3456-7890abcdef12
      needs_attribution_data 1
      referrer         adjust_reftag=abc1234&tracking_id=123456789&utm_source=network&utm_medium=banner&utm_campaign=campaign
      reftag           abc1234
      source           reftag
      tracking_enabled 1
```

如果您在启动应用前执行以上测试，数据包将不会被发送。以上数据包将于应用启动后被发送。

**重要:** 我们建议您**不要**使用`adb`工具来测试该特定功能。为了测试完整的referrer内容（在由`&`分隔多个参数的情况下），如使用adb工具您需要对内容进行编码以便发送给广播接收器。如未编码，`adb`将在第一个`&`符号后剪切referrer，并向您的广播接收器发送错误内容。

如果您希望查看应用如何接收未编码的referrer值，我们建议您使用我们的示例应用，并更改传递的内容，以便被`MainActivity.java`文件内`onFireIntentClick`方法中的意图触发：


 ```java
public void onFireIntentClick(View v) {
    Intent intent = new Intent("com.android.vending.INSTALL_REFERRER");
    intent.setPackage("com.adjust.examples");
    intent.putExtra("referrer", "utm_source=test&utm_medium=test&utm_term=test&utm_content=test&utm_campaign=test");
    sendBroadcast(intent);
}
```

您可随意使用自选内容更改`putExtra`方法的第二个参数。

### <a id="tt-event-at-launch"></a>我是否可以在应用激活时触发事件？

此时触发事件可能无法达到您预期的效果。原因如下：

全局`Application`类上的`onCreate`方式不仅在应用启动时调用，而且在应用记录到系统或应用事件时调用。

此时，我们的SDK已经准备初始化了，但是还没有正式启动。只有当activity发生时，即当用户真正激活应用时，SDK才会正式启动。

即使用户没有激活应用，此时触发事件将启动Adjust SDK以及发送事件——具体时间取决于应用的外部因素。

在应用激活时触发事件将会导致被跟踪的安装及会话数量报告不准确。

如果您希望在安装后触发事件，请使用[归因回传](#af-attribution-callback)。

如果您希望在应用激活后触发事件，请使用被启动的Activity的`onCreate`方法。

[dashboard]:  http://adjust.com
[adjust.com]: http://adjust.com

[en-readme]:  ../../README.md
[zh-readme]:  ../chinese/README.md
[ja-readme]:  ../japanese/README.md
[ko-readme]:  ../korean/README.md

[example-java]:                   ../../Adjust/example-app-java
[example-kotlin]:                 ../../Adjust/example-app-kotlin
[example-tv]:                     ../../Adjust/example-app-tv
[example-webbridge]:              ../../Adjust/example-app-webbridge

[maven]:                          http://maven.org
[referrer]:                       ../chinese/multiple-receivers.md
[releases]:                       https://github.com/adjust/adjust_android_sdk/releases
[google_ad_id]:                   https://support.google.com/googleplay/android-developer/answer/6048248?hl=en
[event-tracking]:                 https://docs.adjust.com/zh/event-tracking
[callbacks-guide]:                https://docs.adjust.com/zh/callbacks
[new-referrer-api]:               https://developer.android.com/google/play/installreferrer/library.html
[special-partners]:               https://docs.adjust.com/zh/special-partners
[attribution-data]:               https://github.com/adjust/sdks/blob/master/doc/attribution-data.md
[android-dashboard]:              http://developer.android.com/about/dashboards/index.html
[currency-conversion]:            https://docs.adjust.com/zh/event-tracking/#tracking-purchases-in-different-currencies
[android_application]:            http://developer.android.com/reference/android/app/Application.html
[android-launch-modes]:           https://developer.android.com/guide/topics/manifest/activity-element.html
[google_play_services]:           http://developer.android.com/google/play-services/setup.html
[reattribution-with-deeplinks]:   https://docs.adjust.com/zh/deeplinking/#manually-appending-attribution-data-to-a-deep-link
[android-purchase-verification]:  https://github.com/adjust/android_purchase_sdk
[testing_console]:                https://docs.adjust.com/zh/testing-console/#part-2
[dev_api]:                        https://docs.adjust.com/zh/adjust-for-developers/

[sdk2sdk-mopub]:                  ../chinese/sdk-to-sdk/mopub.md


## <a id="license"></a>许可

Adjust SDK 拥有MIT 许可证。

版权所有(c) 2012-2019 Adjust GmbH，http://www.adjust.com

特此免费授予获得本软件及相关文档文件（“软件”）副本的任何人，得以无限制地处理本软件，其范围包括但不限于使用、复制、修改、合并、发布、分发、再许可和/或销售本软件的副本；具备本软件上述权限之人员需遵守以下条件：

上述版权声明和本许可声明，应包含在本软件的所有副本或主要部分中。

本软件“按原样”提供，不提供任何形式的明示或暗示保证，包括对适销性、适用于特定用途或非侵权性的保证。任何情况下，作者或版权所有者都不应承担任何索赔、损害赔偿或其他责任，无论是因软件或使用或其他软件处理引起的或与其相关的合同行为、侵权行为或其他行为。


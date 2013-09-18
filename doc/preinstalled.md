## Using AdjustIo for pre-installed apps

If you want to use the AdjustIo SDK to recognize users that found your app pre-installed on their device, follow these steps.

1. Integrate AdjustIo as described in our [README].
2. Create a new tracker in your [dashboard].
3. Open your `AndroidManifest.xml` and add the following line to your AdjustIo settings:

    ```xml
    <meta-data android:name="AdjustIoDefaultTracker" android:value="{Tracker}" />
    ```

    Replace `{Tracker}` with the tracker you created in step 2.

4. Build and run your app. You should see a line like the following in LogCat:

    ```
    Default tracker: 'abc123'
    ```


[README]: ../README.md
[dashboard]: http://adjust.io

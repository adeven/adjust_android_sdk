//
//  ActivityState.java
//  Adjust
//
//  Created by Christian Wellenbrock on 2013-06-25.
//  Copyright (c) 2013 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

class ActivityState implements Serializable {
    private static final long serialVersionUID = 9039439291143138148L;
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("uuid", String.class),
            new ObjectStreamField("enabled", boolean.class),
            new ObjectStreamField("askingAttribution", boolean.class),
            new ObjectStreamField("eventCount", int.class),
            new ObjectStreamField("sessionCount", int.class),
            new ObjectStreamField("subsessionCount", int.class),
            new ObjectStreamField("sessionLength", long.class),
            new ObjectStreamField("timeSpent", long.class),
            new ObjectStreamField("lastActivity", long.class),
            new ObjectStreamField("lastInterval", long.class),
            new ObjectStreamField("updatePackages", boolean.class),
            new ObjectStreamField("orderIds", (Class<LinkedList<String>>) (Class) LinkedList.class),
            new ObjectStreamField("pushToken", String.class)
    };

    // persistent data
    String uuid;
    boolean enabled;
    boolean askingAttribution;

    // global counters
    int eventCount;
    int sessionCount;

    // session attributes
    int subsessionCount;
    long sessionLength;   // all durations in milliseconds
    long timeSpent;
    long lastActivity;    // all times in milliseconds since 1970

    long lastInterval;

    boolean updatePackages;

    LinkedList<String> orderIds;

    String pushToken;

    protected ActivityState() {
        // create UUID for new devices
        uuid = Util.createUuid();
        enabled = true;
        askingAttribution = false;

        eventCount = 0; // no events yet
        sessionCount = 0; // the first session just started
        subsessionCount = -1; // we don't know how many subsessions this first  session will have
        sessionLength = -1; // same for session length and time spent
        timeSpent = -1; // this information will be collected and attached to the next session
        lastActivity = -1;
        lastInterval = -1;
        updatePackages = false;
        orderIds = null;
        pushToken = null;
    }

    protected void resetSessionAttributes(final long now) {
        subsessionCount = 1; // first subsession
        sessionLength = 0; // no session length yet
        timeSpent = 0; // no time spent yet
        lastActivity = now;
        lastInterval = -1;
    }

    protected void addOrderId(final String orderId) {
        if (orderIds == null) {
            orderIds = new LinkedList<>();
        }

        final int orderIdMaxcount = 10;
        if (orderIds.size() >= orderIdMaxcount) {
            orderIds.removeLast();
        }
        orderIds.addFirst(orderId);
    }

    protected boolean findOrderId(final String orderId) {
        if (orderIds == null) {
            return false;
        }
        return orderIds.contains(orderId);
    }

    @Override
    public final String toString() {
        return String.format(Locale.US,
                "ec:%d sc:%d ssc:%d sl:%.1f ts:%.1f la:%s uuid:%s",
                eventCount, sessionCount, subsessionCount,
                sessionLength / 1000.0, timeSpent / 1000.0,
                stamp(lastActivity), uuid);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;
        ActivityState otherActivityState = (ActivityState) other;

        if (!Util.equalString(uuid, otherActivityState.uuid))               return false;
        if (!Util.equalBoolean(enabled, otherActivityState.enabled))            return false;
        if (!Util.equalBoolean(askingAttribution, otherActivityState.askingAttribution))  return false;
        if (!Util.equalInt(eventCount, otherActivityState.eventCount))         return false;
        if (!Util.equalInt(sessionCount, otherActivityState.sessionCount))       return false;
        if (!Util.equalInt(subsessionCount, otherActivityState.subsessionCount))    return false;
        if (!Util.equalLong(sessionLength, otherActivityState.sessionLength))      return false;
        if (!Util.equalLong(timeSpent, otherActivityState.timeSpent))          return false;
        if (!Util.equalLong(lastInterval, otherActivityState.lastInterval))       return false;
        if (!Util.equalBoolean(updatePackages, otherActivityState.updatePackages))            return false;
        if (!Util.equalObject(orderIds, otherActivityState.orderIds)) return false;
        return Util.equalString(pushToken, otherActivityState.pushToken);
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode = 37 * hashCode + Util.hashString(uuid);
        hashCode = 37 * hashCode + Util.hashBoolean(enabled);
        hashCode = 37 * hashCode + Util.hashBoolean(askingAttribution);
        hashCode = 37 * hashCode + eventCount;
        hashCode = 37 * hashCode + sessionCount;
        hashCode = 37 * hashCode + subsessionCount;
        hashCode = 37 * hashCode + Util.hashLong(sessionLength);
        hashCode = 37 * hashCode + Util.hashLong(timeSpent);
        hashCode = 37 * hashCode + Util.hashLong(lastInterval);
        hashCode = 37 * hashCode + Util.hashBoolean(updatePackages);
        hashCode = 37 * hashCode + Util.hashObject(orderIds);
        hashCode = 37 * hashCode + Util.hashString(pushToken);
        return hashCode;
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        GetField fields = stream.readFields();

        eventCount = Util.readIntField(fields, "eventCount", 0);
        sessionCount = Util.readIntField(fields, "sessionCount", 0);
        subsessionCount = Util.readIntField(fields, "subsessionCount", -1);
        sessionLength = Util.readLongField(fields, "sessionLength", -1L);
        timeSpent = Util.readLongField(fields, "timeSpent", -1L);
        lastActivity = Util.readLongField(fields, "lastActivity", -1L);
        lastInterval = Util.readLongField(fields, "lastInterval", -1L);

        // new fields
        uuid = Util.readStringField(fields, "uuid", null);
        enabled = Util.readBooleanField(fields, "enabled", true);
        askingAttribution = Util.readBooleanField(fields, "askingAttribution", false);

        updatePackages = Util.readBooleanField(fields, "updatePackages", false);

        orderIds = Util.readObjectField(fields, "orderIds", null);

        pushToken = Util.readStringField(fields, "pushToken", null);

        // create UUID for migrating devices
        if (uuid == null) {
            uuid = Util.createUuid();
        }
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
     }

    private static String stamp(final long dateMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        return String.format(Locale.US,
                "%02d:%02d:%02d",
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND);
    }
}

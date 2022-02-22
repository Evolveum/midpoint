/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

public class NotificationsStatisticsKey {

    private String transport;
    private boolean success;

    public NotificationsStatisticsKey(String transport, boolean success) {
        this.transport = transport;
        this.success = success;
    }

    public String getTransport() {
        return transport;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationsStatisticsKey that = (NotificationsStatisticsKey) o;

        if (success != that.success) return false;
        return !(transport != null ? !transport.equals(that.transport) : that.transport != null);

    }

    @Override
    public int hashCode() {
        int result = transport != null ? transport.hashCode() : 0;
        result = 31 * result + (success ? 1 : 0);
        return result;
    }
}

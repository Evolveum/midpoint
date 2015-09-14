/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

/**
 * @author Pavol Mederly
 */
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

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

public interface NotificationStatisticsCollector {

    void recordNotificationOperation(String transportName, boolean success, long duration);

}

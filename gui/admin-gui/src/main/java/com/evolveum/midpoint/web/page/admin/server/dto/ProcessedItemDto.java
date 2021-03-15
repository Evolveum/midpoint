/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedItemType;

import java.io.Serializable;

public class ProcessedItemDto implements Serializable {

    private String displayName;
    private long duration;

    ProcessedItemDto(ProcessedItemType processedItemType) {
        createDisplayName(processedItemType);
    }

    private void createDisplayName(ProcessedItemType processedItemType) {
        if (processedItemType == null) {
            return;
        }

        Long start = WebComponentUtil.getTimestampAsLong(processedItemType.getStartTimestamp(), true);
        Long end = WebComponentUtil.getTimestampAsLong(processedItemType.getEndTimestamp(), true);
        duration = end - start;
        displayName = processedItemType.getName();

    }

    public String getDisplayName() {
        return displayName;
    }

    public long getDuration() {
        return duration;
    }
}

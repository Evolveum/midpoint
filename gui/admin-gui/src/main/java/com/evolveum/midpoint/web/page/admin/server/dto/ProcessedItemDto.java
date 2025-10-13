/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

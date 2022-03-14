/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.EnvironmentalPerformanceInformation;
import com.evolveum.midpoint.schema.statistics.StatusMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class StatisticsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String F_PROVISIONING_LINES = "provisioningLines";
    public static final String F_MAPPINGS_LINES = "mappingsLines";
    public static final String F_NOTIFICATIONS_LINES = "notificationsLines";
    public static final String F_LAST_MESSAGE = "lastMessage";

    private EnvironmentalPerformanceInformationType environmentalPerformanceInformationType;
    private List<ProvisioningStatisticsLineDto> provisioningLines;
    private List<MappingsLineDto> mappingsLines;
    private List<NotificationsLineDto> notificationsLines;
    private String lastMessage;

    public StatisticsDto() {
    }

    public StatisticsDto(EnvironmentalPerformanceInformationType environmentalPerformanceInformationType) {
        this.environmentalPerformanceInformationType = environmentalPerformanceInformationType;
        provisioningLines = ProvisioningStatisticsLineDto.extractFromOperationalInformation(environmentalPerformanceInformationType.getProvisioningStatistics());
        mappingsLines = MappingsLineDto.extractFromOperationalInformation(environmentalPerformanceInformationType.getMappingsStatistics());
        notificationsLines = NotificationsLineDto.extractFromOperationalInformation(environmentalPerformanceInformationType.getNotificationsStatistics());
        lastMessage = extractLastMessageFromOperationalInformation(environmentalPerformanceInformationType);
    }

    private String extractLastMessageFromOperationalInformation(EnvironmentalPerformanceInformationType environmentalPerformanceInformationType) {
        if (environmentalPerformanceInformationType.getLastMessageTimestamp() == null) {
            return null;
        }
        Date timestamp = XmlTypeConverter.toDate(environmentalPerformanceInformationType.getLastMessageTimestamp());
        return timestamp + ": " + environmentalPerformanceInformationType.getLastMessage();
    }

    private String extractLastMessageFromOperationalInformation(EnvironmentalPerformanceInformation environmentalPerformanceInformation) {
        StatusMessage lastStatusMessage = environmentalPerformanceInformation.getLastMessage();
        if (lastStatusMessage == null) {
            return null;
        }
        return lastStatusMessage.getDate() + ": " + lastStatusMessage.getMessage();
    }

    public List<ProvisioningStatisticsLineDto> getProvisioningLines() {
        return provisioningLines;
    }

    public void setProvisioningLines(List<ProvisioningStatisticsLineDto> provisioningLines) {
        this.provisioningLines = provisioningLines;
    }

    public List<MappingsLineDto> getMappingsLines() {
        return mappingsLines;
    }

    public void setMappingsLines(List<MappingsLineDto> mappingsLines) {
        this.mappingsLines = mappingsLines;
    }

    public List<NotificationsLineDto> getNotificationsLines() {
        return notificationsLines;
    }

    public void setNotificationsLines(List<NotificationsLineDto> notificationsLines) {
        this.notificationsLines = notificationsLines;
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "(" + PageBase.createStringResourceStatic("StatisticsDto.getLastMessage.none").getString() + ")";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public EnvironmentalPerformanceInformationType getEnvironmentalPerformanceInformationType() {
        return environmentalPerformanceInformationType;
    }
}

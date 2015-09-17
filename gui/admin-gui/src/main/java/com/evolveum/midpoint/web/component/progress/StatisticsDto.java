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

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.OperationalInformation;
import com.evolveum.midpoint.schema.statistics.StatusMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalInformationType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public class StatisticsDto implements Serializable {

    public static final String F_PROVISIONING_LINES = "provisioningLines";
    public static final String F_MAPPINGS_LINES = "mappingsLines";
    public static final String F_NOTIFICATIONS_LINES = "notificationsLines";
    public static final String F_LAST_MESSAGE = "lastMessage";

    private OperationalInformationType operationalInformationType;
    private List<ProvisioningStatisticsLineDto> provisioningLines;
    private List<MappingsLineDto> mappingsLines;
    private List<NotificationsLineDto> notificationsLines;
    private String lastMessage;

    public StatisticsDto() {
    }

    public StatisticsDto(OperationalInformationType operationalInformationType) {
        this.operationalInformationType = operationalInformationType;
        provisioningLines = ProvisioningStatisticsLineDto.extractFromOperationalInformation(operationalInformationType.getProvisioningStatistics());
        mappingsLines = MappingsLineDto.extractFromOperationalInformation(operationalInformationType.getMappingsStatistics());
        notificationsLines = NotificationsLineDto.extractFromOperationalInformation(operationalInformationType.getNotificationsStatistics());
        lastMessage = extractLastMessageFromOperationalInformation(operationalInformationType);
    }

    private String extractLastMessageFromOperationalInformation(OperationalInformationType operationalInformationType) {
        if (operationalInformationType.getLastMessageTimestamp() == null) {
            return null;
        }
        Date timestamp = XmlTypeConverter.toDate(operationalInformationType.getLastMessageTimestamp());
        return timestamp + ": " + operationalInformationType.getLastMessage();
    }

    private String extractLastMessageFromOperationalInformation(OperationalInformation operationalInformation) {
        StatusMessage lastStatusMessage = operationalInformation.getLastMessage();
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
        return lastMessage != null ? lastMessage : "(none)";        // i18n
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public OperationalInformationType getOperationalInformationType() {
        return operationalInformationType;
    }
}

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

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class OperationalInformation {

    private Map<ProvisioningStatisticsKey,ProvisioningStatisticsData> provisioningData = new HashMap<>();
    private Map<NotificationsStatisticsKey,GenericStatisticsData> notificationsData = new HashMap<>();
    private Map<MappingsStatisticsKey,GenericStatisticsData> mappingsData = new HashMap<>();

    private List<StatusMessage> messages = new ArrayList<>();

    public void recordState(String message) {
        messages.add(new StatusMessage(message));
    }

    public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName, ProvisioningOperation operation, boolean success, int count, long duration) {
        ProvisioningStatisticsKey key = new ProvisioningStatisticsKey(resourceOid, resourceName, objectClassName, operation, success);
        ProvisioningStatisticsData data = provisioningData.get(key);
        if (data == null) {
            data = new ProvisioningStatisticsData();
            provisioningData.put(key, data);
        }
        data.recordOperation((int) duration, count);       // TODO long vs int
    }

    public void recordNotificationOperation(String transportName, boolean success, long duration) {
        NotificationsStatisticsKey key = new NotificationsStatisticsKey(transportName, success);
        GenericStatisticsData data = notificationsData.get(key);
        if (data == null) {
            data = new GenericStatisticsData();
            notificationsData.put(key, data);
        }
        data.recordOperation((int) duration, 1);
    }

    public void recordMappingOperation(String objectOid, String objectName, String mappingName, long duration) {
        // ignoring mapping name for now
        if (objectName == null) {
            System.out.println("Null objectName");
        }
        MappingsStatisticsKey key = new MappingsStatisticsKey(objectOid, objectName);
        GenericStatisticsData data = mappingsData.get(key);
        if (data == null) {
            data = new GenericStatisticsData();
            mappingsData.put(key, data);
        }
        data.recordOperation((int) duration, 1);
    }

    public Map<ProvisioningStatisticsKey, ProvisioningStatisticsData> getProvisioningData() {
        return provisioningData;
    }

    public Map<NotificationsStatisticsKey, GenericStatisticsData> getNotificationsData() {
        return notificationsData;
    }

    public Map<MappingsStatisticsKey, GenericStatisticsData> getMappingsData() {
        return mappingsData;
    }

    public List<StatusMessage> getMessages() {
        return messages;
    }
}

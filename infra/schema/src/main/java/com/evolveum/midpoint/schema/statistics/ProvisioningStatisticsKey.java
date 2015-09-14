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

/**
 * @author Pavol Mederly
 */
public class ProvisioningStatisticsKey {

    private String resourceOid;
    private String resourceName;        // TODO normalize
    private QName objectClass;
    private ProvisioningOperation operation;
    private ProvisioningStatusType statusType;

    public ProvisioningStatisticsKey(String resourceOid, String resourceName, QName objectClass, ProvisioningOperation operation, boolean success) {
        this.resourceOid = resourceOid;
        this.resourceName = resourceName;
        this.objectClass = objectClass;
        this.operation = operation;
        this.statusType = success ? ProvisioningStatusType.SUCCESS : ProvisioningStatusType.FAILURE;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public ProvisioningOperation getOperation() {
        return operation;
    }

    public ProvisioningStatusType getStatusType() {
        return statusType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProvisioningStatisticsKey that = (ProvisioningStatisticsKey) o;

        if (resourceOid != null ? !resourceOid.equals(that.resourceOid) : that.resourceOid != null) return false;
        if (resourceName != null ? !resourceName.equals(that.resourceName) : that.resourceName != null) return false;
        if (objectClass != null ? !objectClass.equals(that.objectClass) : that.objectClass != null) return false;
        if (operation != that.operation) return false;
        return statusType == that.statusType;

    }

    @Override
    public int hashCode() {
        int result = resourceOid != null ? resourceOid.hashCode() : 0;
        result = 31 * result + (resourceName != null ? resourceName.hashCode() : 0);
        result = 31 * result + (objectClass != null ? objectClass.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (statusType != null ? statusType.hashCode() : 0);
        return result;
    }
}

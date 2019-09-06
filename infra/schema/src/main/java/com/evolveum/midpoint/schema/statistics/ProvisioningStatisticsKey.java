/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

public class MappingsStatisticsKey {

    private final String objectOid;
    private final String objectName;
    private final String objectType;

    public MappingsStatisticsKey(String objectOid, String objectName, String objectType) {
        this.objectOid = objectOid;
        this.objectName = objectName;
        this.objectType = objectType;
    }

    public String getObjectOid() {
        return objectOid;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectType() {
        return objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MappingsStatisticsKey that = (MappingsStatisticsKey) o;

        if (objectOid != null ? !objectOid.equals(that.objectOid) : that.objectOid != null) return false;
        if (objectType != null ? !objectType.equals(that.objectType) : that.objectType != null) return false;
        return !(objectName != null ? !objectName.equals(that.objectName) : that.objectName != null);

    }

    @Override
    public int hashCode() {
        int result = objectOid != null ? objectOid.hashCode() : 0;
        result = 31 * result + (objectName != null ? objectName.hashCode() : 0);
        return result;
    }
}

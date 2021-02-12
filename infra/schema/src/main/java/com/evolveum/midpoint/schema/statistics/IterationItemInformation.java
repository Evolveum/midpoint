/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

/**
 * TODO
 */
public class IterationItemInformation {

    private String objectName;
    private String objectDisplayName;
    private QName objectType;
    private String objectOid;

    public IterationItemInformation() {
    }

    public IterationItemInformation(PrismObject<? extends ObjectType> object) {
        this.objectName = PolyString.getOrig(object.getName());
        this.objectDisplayName = StatisticsUtil.getDisplayName(object);
        this.objectType = object.getDefinition().getTypeName();
        this.objectOid = object.getOid();
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectDisplayName() {
        return objectDisplayName;
    }

    public void setObjectDisplayName(String objectDisplayName) {
        this.objectDisplayName = objectDisplayName;
    }

    public QName getObjectType() {
        return objectType;
    }

    public void setObjectType(QName objectType) {
        this.objectType = objectType;
    }

    public String getObjectOid() {
        return objectOid;
    }

    public void setObjectOid(String objectOid) {
        this.objectOid = objectOid;
    }

    @Override
    public String toString() {
        return getTypeLocalPart() + ":" + objectName + " (" + objectDisplayName + ", " + objectOid + ")";
    }

    private String getTypeLocalPart() {
        return objectType != null ? objectType.getLocalPart() : null;
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * TODO better name
 */
public class IterationItemInformation {

    private String objectName;
    private String objectDisplayName;
    private QName objectType;
    private String objectOid;

    public IterationItemInformation() {
    }

    public IterationItemInformation(String objectName, String objectDisplayName, QName objectType, String objectOid) {
        this.objectName = objectName;
        this.objectDisplayName = objectDisplayName;
        this.objectType = objectType;
        this.objectOid = objectOid;
    }

    public IterationItemInformation(PrismObject<? extends ObjectType> object) {
        this(object, object.getPrismContext());
    }

    public IterationItemInformation(PrismObject<? extends ObjectType> object, PrismContext prismContext) {
        this.objectName = PolyString.getOrig(object.getName());
        this.objectDisplayName = ObjectTypeUtil.getDetailedDisplayName(object);
        this.objectType = determineTypeName(object, prismContext);
        this.objectOid = object.getOid();
    }

    private QName determineTypeName(PrismObject<? extends ObjectType> object, PrismContext prismContext) {
        if (prismContext != null) {
            return ObjectTypeUtil.getObjectType(object.asObjectable(), prismContext);
        } else {
            PrismObjectDefinition<? extends ObjectType> definition = object.getDefinition();
            return definition != null ? definition.getTypeName() : null;
        }
    }

    public IterationItemInformation(ShadowType shadow) {
        this(PolyString.getOrig(shadow.getName()),
                ObjectTypeUtil.getDetailedDisplayName(shadow),
                ShadowType.COMPLEX_TYPE, shadow.getOid());
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

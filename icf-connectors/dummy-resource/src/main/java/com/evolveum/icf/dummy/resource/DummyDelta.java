/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import java.util.Collection;

/**
 * @author semancik
 *
 */
public class DummyDelta {

    private final int syncToken;
    private final Class<? extends DummyObject> objectJavaClass;
    private final String objectClassName;
    private final String objectId;
    private String objectName;
    private DummyDeltaType type;
    private String attributeName;
    private Collection<Object> valuesAdded = null;
    private Collection<Object> valuesDeleted = null;
    private Collection<Object> valuesReplaced = null;

    DummyDelta(
            int syncToken,
            Class<? extends DummyObject> objectJavaClass,
            String objectClassName,
            String objectId,
            String objectName,
            DummyDeltaType type) {
        this.syncToken = syncToken;
        this.objectJavaClass = objectJavaClass;
        this.objectClassName = objectClassName;
        this.objectId = objectId;
        this.objectName = objectName;
        this.type = type;
    }

    public int getSyncToken() {
        return syncToken;
    }

    public Class<? extends DummyObject> getObjectJavaClass() {
        return objectJavaClass;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public DummyDeltaType getType() {
        return type;
    }

    public void setType(DummyDeltaType type) {
        this.type = type;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public Collection<Object> getValuesAdded() {
        return valuesAdded;
    }

    public void setValuesAdded(Collection<Object> valuesAdded) {
        this.valuesAdded = valuesAdded;
    }

    public Collection<Object> getValuesDeleted() {
        return valuesDeleted;
    }

    public void setValuesDeleted(Collection<Object> valuesDeleted) {
        this.valuesDeleted = valuesDeleted;
    }

    public Collection<Object> getValuesReplaced() {
        return valuesReplaced;
    }

    public void setValuesReplaced(Collection<Object> valuesReplaced) {
        this.valuesReplaced = valuesReplaced;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DummyDelta(");
        dumpToString(sb);
        sb.append(")");
        return sb.toString();
    }

    public void dump(StringBuilder sb) {
        dumpToString(sb);
        sb.append(", A=").append(attributeName).append(": ");
        if (valuesAdded != null) {
            sb.append(" +").append(valuesAdded);
        }
        if (valuesDeleted != null) {
            sb.append(" -").append(valuesDeleted);
        }
        if (valuesReplaced != null) {
            sb.append(" =").append(valuesReplaced);
        }
    }

    private void dumpToString(StringBuilder sb) {
        sb.append("T=").append(syncToken);
        sb.append(", c=").append(objectJavaClass.getSimpleName());
        sb.append(", id=").append(objectId);
        sb.append(", name=").append(objectName);
        sb.append(", t=").append(type);
    }
}

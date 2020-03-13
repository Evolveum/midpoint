/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

/**
 * TODO change to ObjectReferenceType
 */
public class SimpleObjectRefImpl implements SimpleObjectRef {

    private String oid;
    private ObjectType objectType;
    private NotificationFunctionsImpl functions;        // used to resolve object refs

    SimpleObjectRefImpl(NotificationFunctionsImpl functions, ObjectType objectType) {
        this.oid = objectType.getOid();
        this.objectType = objectType;
        this.functions = functions;
    }

    public SimpleObjectRefImpl(NotificationFunctionsImpl functions, PrismObject<?> object) {
        this.oid = object.getOid();
        this.objectType = (ObjectType) object.asObjectable();
        this.functions = functions;
    }

    public SimpleObjectRefImpl(NotificationFunctionsImpl functions, ObjectReferenceType ref) {
        Validate.notNull(ref);
        this.oid = ref.getOid();
        if (ref.asReferenceValue().getObject() != null) {
            this.objectType = (ObjectType) ref.asReferenceValue().getObject().asObjectable();
        }
        this.functions = functions;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    @Override
    public ObjectType resolveObjectType(OperationResult result, boolean allowNotFound) {
        return functions.getObjectType(this, allowNotFound, result);
    }

    @Override
    public ObjectType resolveObjectType() {
        return resolveObjectType(new OperationResult(SimpleObjectRefImpl.class.getName() + ".resolveObjectType"), true);
    }

    @Override
    public String toString() {
        return "SimpleObjectRef{" +
                "oid='" + oid + '\'' +
                ", objectType=" + objectType +
                '}';
    }

    public static SimpleObjectRef create(NotificationFunctionsImpl functions, ObjectReferenceType ref) {
        return ref != null ? new SimpleObjectRefImpl(functions, ref) : null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "oid", oid, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "objectType", objectType, indent + 1);
        return sb.toString();
    }
}

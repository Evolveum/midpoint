/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

/**
 * TODO change to ObjectReferenceType
 *
 * @author mederly
 */
public class SimpleObjectRefImpl implements SimpleObjectRef {
    private String oid;
    private ObjectType objectType;
    private NotificationFunctionsImpl functions;        // used to resolve object refs

    public SimpleObjectRefImpl(NotificationFunctionsImpl functions, ObjectType objectType) {
        this.oid = objectType.getOid();
        this.objectType = objectType;
        this.functions = functions;
    }

    public SimpleObjectRefImpl(NotificationFunctionsImpl functions, PrismObject object) {
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

    public SimpleObjectRefImpl(NotificationFunctionsImpl functions, String oid) {
        this.oid = oid;
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
    public String toString() {
        return "SimpleObjectRef{" +
                "oid='" + oid + '\'' +
                ", objectType=" + objectType +
                '}';
    }

    public static SimpleObjectRef create(NotificationFunctionsImpl functions, ObjectReferenceType ref) {
        return ref != null ? new SimpleObjectRefImpl(functions, ref) : null;
    }
}

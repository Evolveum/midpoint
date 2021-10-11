/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

/**
 * @author mederly
 */
public class TypedObjectQuery<T extends ObjectType> {

    private Class<T> objectClass;
    private ObjectQuery objectQuery;

    public TypedObjectQuery() {
    }

    public TypedObjectQuery(Class<T> objectClass, ObjectQuery objectQuery) {
        this.objectClass = objectClass;
        this.objectQuery = objectQuery;
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    public ObjectQuery getObjectQuery() {
        return objectQuery;
    }

    public void setObjectQuery(ObjectQuery objectQuery) {
        this.objectQuery = objectQuery;
    }
}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogQueryItem implements Serializable {

    private ObjectReferenceType orgRef;

    private boolean scopeOne;

    private ObjectReferenceType collectionRef;

    public ObjectReferenceType orgRef() {
        return orgRef;
    }

    public RoleCatalogQueryItem orgRef(ObjectReferenceType orgRef) {
        this.orgRef = orgRef;
        return this;
    }

    public ObjectReferenceType collectionRef() {
        return collectionRef;
    }

    public RoleCatalogQueryItem collectionRef(ObjectReferenceType collectionRef) {
        this.collectionRef = collectionRef;
        return this;
    }

    public boolean scopeOne() {
        return scopeOne;
    }

    public RoleCatalogQueryItem scopeOne(boolean scopeOne) {
        this.scopeOne = scopeOne;
        return this;
    }
}

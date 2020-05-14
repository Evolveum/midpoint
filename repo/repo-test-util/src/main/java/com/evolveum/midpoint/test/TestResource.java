/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.File;

/**
 * Representation of any prism object in tests.
 */
@Experimental
public class TestResource<T extends ObjectType> {

    public final File file;
    public final String oid;
    public PrismObject<T> object;

    public TestResource(File dir, String fileName, String oid) {
        this.file = new File(dir, fileName);
        this.oid = oid;
    }

    public String getNameOrig() {
        return object.getName().getOrig();
    }

    public T getObjectable() {
        return object.asObjectable();
    }

    public Class<T> getObjectClass() {
        return object.getCompileTimeClass();
    }
}

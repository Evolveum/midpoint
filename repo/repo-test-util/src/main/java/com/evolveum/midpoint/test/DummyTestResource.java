/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.io.File;

/**
 * Representation of Dummy Resource in tests.
 */
@Experimental
public class DummyTestResource extends TestResource<ResourceType> {

    public final String name;
    public DummyResourceContoller controller;

    public DummyTestResource(File dir, String fileName, String oid, String name) {
        super(dir, fileName, oid);
        this.name = name;
    }
}

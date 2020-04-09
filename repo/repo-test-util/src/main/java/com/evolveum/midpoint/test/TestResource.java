/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.File;

/**
 * Representation of any prism object in tests.
 */
@Experimental
public class TestResource {

    public final File file;
    public final String oid;

    public TestResource(File dir, String fileName, String oid) {
        this.file = new File(dir, fileName);
        this.oid = oid;
    }
}

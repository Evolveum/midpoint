/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.util;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FileReference {

    private File reference;
    private String value;

    public FileReference(File reference) {
        this.reference = reference;
    }

    public FileReference(String value) {
        this.value = value;
    }

    public File getReference() {
        return reference;
    }

    public String getValue() {
        return value;
    }
}

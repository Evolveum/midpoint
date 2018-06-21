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

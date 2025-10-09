/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.manual;

import java.io.File;
import java.io.IOException;

/**
 * @author semancik
 *
 */
public class CsvDisablingBackingStore extends CsvBackingStore {

    public CsvDisablingBackingStore() {
        super();
    }

    public CsvDisablingBackingStore(File sourceFile, File targetFile) {
        super(sourceFile, targetFile);
    }

    protected void deprovisionInCsv(String username) throws IOException {
        disableInCsv(username);
    }
}

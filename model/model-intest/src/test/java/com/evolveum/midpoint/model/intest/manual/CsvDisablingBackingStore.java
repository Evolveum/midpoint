/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class CsvDisablingBackingStore extends CsvBackingStore {

    private static final Trace LOGGER = TraceManager.getTrace(CsvDisablingBackingStore.class);

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

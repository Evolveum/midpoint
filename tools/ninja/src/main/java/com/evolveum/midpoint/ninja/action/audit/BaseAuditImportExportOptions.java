/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.audit;

import com.beust.jcommander.Parameter;

import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;

public class BaseAuditImportExportOptions {

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    public static final String P_MULTI_THREAD = "-l";
    public static final String P_MULTI_THREAD_LONG = "--multi-thread";

    @Parameter(names = { P_FILTER, P_FILTER_LONG }, descriptionKey = "base.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    @Parameter(names = { P_ZIP, P_ZIP_LONG }, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    @Parameter(names = { P_MULTI_THREAD, P_MULTI_THREAD_LONG }, descriptionKey = "baseImportExport.multiThread")
    private int multiThread = 1;

    public FileReference getFilter() {
        return filter;
    }

    public boolean isZip() {
        return zip;
    }

    public int getMultiThread() {
        return multiThread;
    }
}

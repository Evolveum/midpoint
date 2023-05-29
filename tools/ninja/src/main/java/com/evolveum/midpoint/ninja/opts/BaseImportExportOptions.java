/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BaseImportExportOptions {

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_OID_LONG = "--oid";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    public static final String P_MULTI_THREAD = "-l";
    public static final String P_MULTI_THREAD_LONG = "--multi-thread";

    @Parameter(names = {P_RAW, P_RAW_LONG}, descriptionKey = "baseImportExport.raw")
    private boolean raw;

    @Parameter(names = {P_OID_LONG}, descriptionKey = "baseImportExport.oid")
    private String oid;

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, descriptionKey = "base.type",
            validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class)
    private Set<ObjectTypes> type = new HashSet<>();

    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "base.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    @Parameter(names = {P_ZIP, P_ZIP_LONG}, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    @Parameter(names = {P_MULTI_THREAD, P_MULTI_THREAD_LONG}, descriptionKey = "baseImportExport.multiThread")
    private int multiThread = 1;

    public boolean isRaw() {
        return raw;
    }

    public Set<ObjectTypes> getType() {
        return type;
    }

    public FileReference getFilter() {
        return filter;
    }

    public boolean isZip() {
        return zip;
    }

    public String getOid() {
        return oid;
    }

    public int getMultiThread() {
        return multiThread;
    }

    public BaseImportExportOptions setRaw(boolean raw) {
        this.raw = raw;
        return this;
    }

    public BaseImportExportOptions setOid(String oid) {
        this.oid = oid;
        return this;
    }

    public BaseImportExportOptions setType(Set<ObjectTypes> type) {
        this.type = type;
        return this;
    }

    public BaseImportExportOptions setFilter(FileReference filter) {
        this.filter = filter;
        return this;
    }

    public BaseImportExportOptions setZip(boolean zip) {
        this.zip = zip;
        return this;
    }

    public BaseImportExportOptions setMultiThread(int multiThread) {
        this.multiThread = multiThread;
        return this;
    }
}

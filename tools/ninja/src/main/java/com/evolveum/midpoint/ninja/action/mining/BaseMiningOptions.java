/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import com.beust.jcommander.Parameter;

import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;

public class BaseMiningOptions {

    public static final String P_FILTER_ROLE = "-fr";
    public static final String P_FILTER_ROLE_LONG = "--filterRole";
    public static final String P_FILTER_USER = "-fu";
    public static final String P_FILTER_USER_LONG = "--filterUser";
    public static final String P_FILTER_ORG = "-fo";
    public static final String P_FILTER_ORG_LONG = "--filterOrg";
    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";
    public static final String P_MULTI_THREAD = "-l";
    public static final String P_MULTI_THREAD_LONG = "--multi-thread";

    @Parameter(names = { P_FILTER_ROLE, P_FILTER_ROLE_LONG }, descriptionKey = "base.filterRole",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference roleFilter;

    @Parameter(names = { P_FILTER_USER, P_FILTER_USER_LONG }, descriptionKey = "base.filterUser",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference userFilter;

    @Parameter(names = { P_FILTER_ORG, P_FILTER_ORG_LONG }, descriptionKey = "base.filterOrg",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference orgFilter;

    @Parameter(names = { P_ZIP, P_ZIP_LONG }, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    @Parameter(names = { P_MULTI_THREAD, P_MULTI_THREAD_LONG }, descriptionKey = "baseImportExport.multiThread")
    private int multiThread = 1;

    public FileReference getRoleFilter() {
        return roleFilter;
    }

    public FileReference getUserFilter() {
        return userFilter;
    }

    public FileReference getOrgFilter() {
        return orgFilter;
    }

    public boolean isZip() {
        return zip;
    }

    public int getMultiThread() {
        return multiThread;
    }
}

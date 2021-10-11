/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class InternalsConfigDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String F_CONSISTENCY_CHECKS = "consistencyChecks";
    public static final String F_ENCRYPTION_CHECKS = "encryptionChecks";
    public static final String F_READ_ENCRYPTION_CHECKS = "readEncryptionChecks";
    public static final String F_MODEL_PROFILING = "modelProfiling";
    public static final String F_DETAILED_DEBUG_DUMP = "detailedDebugDump";
    public static final String F_TOLERATE_UNDECLARED_PREFIXES = "tolerateUndeclaredPrefixes";

    //internals config
    private boolean consistencyChecks;
    private boolean encryptionChecks;
    private boolean readEncryptionChecks;
    private boolean modelProfiling;
    //debug util
    private boolean detailedDebugDump;
    //DOM util
    private boolean tolerateUndeclaredPrefixes;

    public InternalsConfigDto() {
        consistencyChecks = InternalsConfig.isConsistencyChecks();
        encryptionChecks = InternalsConfig.isEncryptionChecks();
        readEncryptionChecks = InternalsConfig.isReadEncryptionChecks();
        modelProfiling = InternalsConfig.isModelProfiling();

        detailedDebugDump = DebugUtil.isDetailedDebugDump();

        tolerateUndeclaredPrefixes = QNameUtil.isTolerateUndeclaredPrefixes();
    }

    public boolean isConsistencyChecks() {
        return consistencyChecks;
    }

    public void setConsistencyChecks(boolean consistencyChecks) {
        this.consistencyChecks = consistencyChecks;
    }

    public boolean isEncryptionChecks() {
        return encryptionChecks;
    }

    public void setEncryptionChecks(boolean encryptionChecks) {
        this.encryptionChecks = encryptionChecks;
    }

    public boolean isReadEncryptionChecks() {
        return readEncryptionChecks;
    }

    public void setReadEncryptionChecks(boolean readEncryptionChecks) {
        this.readEncryptionChecks = readEncryptionChecks;
    }

    protected boolean isModelProfiling() {
        return modelProfiling;
    }

    protected void setModelProfiling(boolean modelProfiling) {
        this.modelProfiling = modelProfiling;
    }

    public boolean isDetailedDebugDump() {
        return detailedDebugDump;
    }

    public void setDetailedDebugDump(boolean detailedDebugDump) {
        this.detailedDebugDump = detailedDebugDump;
    }

    public boolean isTolerateUndeclaredPrefixes() {
        return tolerateUndeclaredPrefixes;
    }

    public void setTolerateUndeclaredPrefixes(boolean tolerateUndeclaredPrefixes) {
        this.tolerateUndeclaredPrefixes = tolerateUndeclaredPrefixes;
    }

    // undeclared prefixes is also handled here
    public void saveInternalsConfig() {
        InternalsConfig.setConsistencyChecks(consistencyChecks);
        InternalsConfig.setEncryptionChecks(encryptionChecks);
        InternalsConfig.setReadEncryptionChecks(readEncryptionChecks);
        InternalsConfig.setModelProfiling(modelProfiling);
        QNameUtil.setTolerateUndeclaredPrefixes(tolerateUndeclaredPrefixes);
    }

    public void saveDebugUtil() {
        DebugUtil.setDetailedDebugDump(detailedDebugDump);
    }
}

/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

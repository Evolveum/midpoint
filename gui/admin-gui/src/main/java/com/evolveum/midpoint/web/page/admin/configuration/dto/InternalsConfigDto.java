/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.schema.internals.InternalMonitor;
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
    public static final String F_DETAILED_DEBUG_DUMP = "detailedDebugDump";
    public static final String F_TOLERATE_UNDECLARED_PREFIXES = "tolerateUndeclaredPrefixes";
    public static final String F_TRACE_REPOSITORY_OPERATIONS = "traceRepositoryOperations";

    //internals config
    private boolean consistencyChecks;
    private boolean encryptionChecks;
    private boolean readEncryptionChecks;
    //debug util
    private boolean detailedDebugDump;
    //DOM util
    private boolean tolerateUndeclaredPrefixes;
    // InternalMonitor
    private boolean traceRepositoryOperations;

    public InternalsConfigDto() {
        consistencyChecks = InternalsConfig.consistencyChecks;
        encryptionChecks = InternalsConfig.encryptionChecks;
        readEncryptionChecks = InternalsConfig.readEncryptionChecks;

        detailedDebugDump = DebugUtil.isDetailedDebugDump();

        tolerateUndeclaredPrefixes = QNameUtil.isTolerateUndeclaredPrefixes();
        
        traceRepositoryOperations = InternalMonitor.isTraceRepositoryOperations();
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

    public boolean isTraceRepositoryOperations() {
		return traceRepositoryOperations;
	}

	public void setTraceRepositoryOperations(boolean traceRepositoryOperations) {
		this.traceRepositoryOperations = traceRepositoryOperations;
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
        InternalsConfig.consistencyChecks = consistencyChecks;
        InternalsConfig.encryptionChecks = encryptionChecks;
        InternalsConfig.readEncryptionChecks = readEncryptionChecks;
        QNameUtil.setTolerateUndeclaredPrefixes(tolerateUndeclaredPrefixes);
        InternalMonitor.setTraceRepositoryOperations(traceRepositoryOperations);
    }

    public void saveDebugUtil() {
        DebugUtil.setDetailedDebugDump(detailedDebugDump);
    }
}

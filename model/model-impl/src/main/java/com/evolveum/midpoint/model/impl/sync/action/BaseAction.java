/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.impl.sync.action;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.sync.Action;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public abstract class BaseAction implements Action {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAction.class);

    private Clockwork clockwork;
    private ChangeExecutor executor;
    private ModelObjectResolver modelObjectResolver;
    private ProvisioningService provisioningService;
    private PrismContext prismContext;
    private AuditService auditService;
    private ContextFactory contextFactory;

	public AuditService getAuditService() {
    	return auditService;
    }

    public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    public void setProvisioningService(ProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    public ContextFactory getContextFactory() {
		return contextFactory;
	}

	public void setContextFactory(ContextFactory contextFactory) {
		this.contextFactory = contextFactory;
	}

    protected UserType getUser(String oid, OperationResult result) {
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        try {
            return modelObjectResolver.getObjectSimple(UserType.class, oid, null, null, result);
        } catch (ObjectNotFoundException ex) {
            // user was not found, we return null
        	return null;
        }
    }

	public ModelObjectResolver getModelObjectResolver() {
		return modelObjectResolver;
	}

	public void setModelObjectResolver(ModelObjectResolver modelObjectResolver) {
		this.modelObjectResolver = modelObjectResolver;
	}

	public void setClockwork(Clockwork clockwork) {
        this.clockwork = clockwork;
    }

    public ChangeExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ChangeExecutor executor) {
        this.executor = executor;
    }

}

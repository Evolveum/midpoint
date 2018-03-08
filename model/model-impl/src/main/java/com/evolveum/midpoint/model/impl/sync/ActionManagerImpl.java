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

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.sync.action.BaseAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class ActionManagerImpl<T extends Action> implements ActionManager<T> {

    private static transient Trace trace = TraceManager.getTrace(ActionManagerImpl.class);
    private Map<String, Class<T>> actionMap;
    private Clockwork clockwork;
    private ChangeExecutor changeExecutor;
    private PrismContext prismContext;
    private ProvisioningService provisioningService;
    private ContextFactory contextFactory;
    private AuditService auditService;
    private ModelObjectResolver modelObjectResolver;

    @Override
    public void setActionMapping(Map<String, Class<T>> actionMap) {
        Validate.notNull(actionMap, "Action mapping must not be null.");
        this.actionMap = actionMap;
    }

    @Override
    public Action getActionInstance(String uri) {
        Validate.notEmpty(uri, "Action URI must not be null or empty.");
        Class<T> clazz = actionMap.get(uri);
        if (clazz == null) {
            return null;
        }

        Action action = null;
        try {
            action = clazz.newInstance();
            if (action instanceof BaseAction) {
                BaseAction baseAction = (BaseAction) action;
                baseAction.setClockwork(clockwork);
                baseAction.setExecutor(changeExecutor);
                baseAction.setPrismContext(prismContext);
                baseAction.setAuditService(auditService);
                baseAction.setProvisioningService(provisioningService);
                baseAction.setContextFactory(contextFactory);
                baseAction.setModelObjectResolver(modelObjectResolver);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(trace, "Couldn't create action instance", ex);
        }

        return action;
    }

    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        if (actionMap != null) {
            actions.addAll(actionMap.keySet());
        }

        return actions;
    }

    public void setClockwork(Clockwork clockwork) {
        this.clockwork = clockwork;
    }

    public void setChangeExecutor(ChangeExecutor executor) {
        this.changeExecutor = executor;
    }

    public void setModelObjectResolver(ModelObjectResolver modelObjectResolver) {
        this.modelObjectResolver = modelObjectResolver;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
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
}

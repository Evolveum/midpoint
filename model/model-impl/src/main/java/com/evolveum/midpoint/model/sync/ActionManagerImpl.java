/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.sync.action.BaseAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
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
    private AuditService auditService;
    
    @Deprecated
    private ModelController model;

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
                baseAction.setModel(model);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(trace, "Couldn't create action instance", ex);
        }

        return action;
    }

    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<String>();
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

    @Deprecated
    public void setModel(ModelController model) {
        this.model = model;
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
}

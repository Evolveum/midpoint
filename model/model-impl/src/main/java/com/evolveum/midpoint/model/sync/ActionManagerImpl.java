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
import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.action.BaseAction;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.schema.SchemaRegistry;
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
    private UserSynchronizer synchronizer;
    private ChangeExecutor changeExecutor;
    private SchemaRegistry schemaRegistry;
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
                baseAction.setSynchronizer(synchronizer);
                baseAction.setExecutor(changeExecutor);
                baseAction.setSchemaRegistry(schemaRegistry);
                baseAction.setAuditService(auditService);
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

    public void setSynchronizer(UserSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public void setChangeExecutor(ChangeExecutor executor) {
        this.changeExecutor = executor;
    }

    @Deprecated
    public void setModel(ModelController model) {
        this.model = model;
    }

    public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}
    
}

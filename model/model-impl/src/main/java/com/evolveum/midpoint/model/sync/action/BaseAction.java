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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.*;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class BaseAction implements Action {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAction.class);

    private UserSynchronizer synchronizer;
    private ChangeExecutor executor;
    private ModelController model;
    private SchemaRegistry schemaRegistry;
    private List<Object> parameters;

    @Override
    public List<Object> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<Object>();
        }
        return parameters;
    }

    @Override
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    protected Element getParameterElement(QName qname) {
        Validate.notNull(qname, "QName must not be null.");

        List<Object> parameters = getParameters();
        Element element = null;
        for (Object object : parameters) {
            if (!(object instanceof Element)) {
                continue;
            }
            Element parameter = (Element) object;
            if (parameter.getLocalName().equals(qname.getLocalPart())
                    && qname.getNamespaceURI().equals(parameter.getNamespaceURI())) {
                element = parameter;
                break;
            }
        }

        return element;
    }

    protected UserType getUser(String oid, OperationResult result) throws SynchronizationException {
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        try {
            return model.getObject(UserType.class, oid, new PropertyReferenceListType(), result);
        } catch (ObjectNotFoundException ex) {
            // user was not found, we return null
        } catch (Exception ex) {
            throw new SynchronizationException("Can't get user with oid '" + oid
                    + "'. Unknown error occurred.", ex);
        }

        return null;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, OperationResult result) throws SynchronizationException {
        Validate.notNull(change, "Resource object change description must not be null.");
        Validate.notNull(situation, "Synchronization situation must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        return null;
    }

    public void setModel(ModelController model) {
        this.model = model;
    }

    protected ModelController getModel() {
        return model;
    }

    public UserSynchronizer getSynchronizer() {
        return synchronizer;
    }

    public void setSynchronizer(UserSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public ChangeExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ChangeExecutor executor) {
        this.executor = executor;
    }

    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

    public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    protected AccountSyncContext createAccountSyncContext(SyncContext context,
            ResourceObjectShadowChangeDescription change, PolicyDecision policyDecision,
            ActivationDecision activationDecision) throws SchemaException {
        LOGGER.debug("Creating account context for sync change.");

        ResourceType resource = change.getResource();

        String accountType = getAccountTypeFromChange(change);
        ResourceAccountType resourceAccount = new ResourceAccountType(resource.getOid(), accountType);
        AccountSyncContext accountContext = context.createAccountSyncContext(resourceAccount);
        accountContext.setResource(resource);
        accountContext.setOid(getOidFromChange(change));

        //insert object delta if available in change
        ObjectDelta<? extends ResourceObjectShadowType> delta = change.getObjectDelta();
        if (delta != null && AccountShadowType.class.isAssignableFrom(delta.getObjectTypeClass())) {
            accountContext.setAccountSyncDelta((ObjectDelta<AccountShadowType>) delta);
        }

        //we insert account if available in change
        accountContext.setAccountOld(getAccountObject(change));

        accountContext.setPolicyDecision(policyDecision);
        accountContext.setActivationDecision(activationDecision);
        boolean doReconciliation = determineAttributeReconciliation(change);
        accountContext.setDoReconciliation(doReconciliation);

        LOGGER.debug("Setting account context policy decision ({}), activation decision ({}), do reconciliation ({})",
                new Object[]{policyDecision, activationDecision, doReconciliation});

        return accountContext;
    }

    private boolean determineAttributeReconciliation(ResourceObjectShadowChangeDescription change) {
        Boolean reconcileAttributes = change.getResource().getSynchronization().isReconcileAttributes();
        if (reconcileAttributes == null) {
            // "Automatic mode", do reconciliation only if the complete current shadow was provided
            reconcileAttributes = change.getCurrentShadow() != null;
            LOGGER.trace("Attribute reconciliation automatic mode: {}", reconcileAttributes);
        } else {
            LOGGER.trace("Attribute reconciliation manual mode: {}", reconcileAttributes);
        }
        return reconcileAttributes;
    }

    private MidPointObject<AccountShadowType> getAccountObject(ResourceObjectShadowChangeDescription change)
            throws SchemaException {

        AccountShadowType account = getAccountShadowFromChange(change);
        if (account == null) {
            return null;
        }

        ObjectDefinition<AccountShadowType> definition = RefinedResourceSchema.getRefinedSchema(
                change.getResource(), getSchemaRegistry()).getObjectDefinition(account);

        return definition.parseObjectType(account);
    }

    protected AccountShadowType getAccountShadowFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getCurrentShadow() != null) {
            return (AccountShadowType) change.getCurrentShadow();
        }

        if (change.getOldShadow() != null) {
            return (AccountShadowType) change.getOldShadow();
        }

        return null;
    }

    private String getAccountTypeFromChange(ResourceObjectShadowChangeDescription change) {
        AccountShadowType account = getAccountShadowFromChange(change);
        if (account != null) {
            return account.getAccountType();
        }

        LOGGER.warn("Can't get account type from change (resource {}), because current and old shadow are null. " +
                "Therefore we can't create account sync context.", change.getResource().getName());

        return null;
    }

    private String getOidFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getOid();
        }

        return change.getCurrentShadow().getOid();
    }
}

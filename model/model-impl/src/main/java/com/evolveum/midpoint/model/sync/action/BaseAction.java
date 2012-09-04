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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class BaseAction implements Action {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAction.class);

    private Clockwork clockwork;
    private ChangeExecutor executor;
    private ModelController model;
    private List<Object> parameters;

    private PrismContext prismContext;
    private AuditService auditService;
    
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
            return model.getObjectResolver().getObject(UserType.class, oid, null, result);
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
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) 
    		throws SynchronizationException, SchemaException {
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

    public void setClockwork(Clockwork clockwork) {
        this.clockwork = clockwork;
    }

    public ChangeExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ChangeExecutor executor) {
        this.executor = executor;
    }

    protected LensProjectionContext<AccountShadowType> createAccountSyncContext(LensContext<UserType, AccountShadowType> context,
            ResourceObjectShadowChangeDescription change, PolicyDecision policyDecision,
            ActivationDecision activationDecision) throws SchemaException {
        LOGGER.debug("Creating account context for sync change.");

        ResourceType resource = change.getResource().asObjectable();

        String accountType = getAccountTypeFromChange(change);
        boolean thombstone = isThombstone(change);
		ResourceShadowDiscriminator resourceAccountType = new ResourceShadowDiscriminator(resource.getOid(), accountType, thombstone);
		LensProjectionContext<AccountShadowType> accountContext = context.createProjectionContext(resourceAccountType);
        accountContext.setResource(resource);
        accountContext.setOid(getOidFromChange(change));

        //insert object delta if available in change
        ObjectDelta<? extends ResourceObjectShadowType> delta = change.getObjectDelta();
        if (delta != null && AccountShadowType.class.isAssignableFrom(delta.getObjectTypeClass())) {
            accountContext.setSyncDelta((ObjectDelta<AccountShadowType>) delta);
        }

        //we insert account if available in change
        accountContext.setObjectOld(getAccountObject(change));

        accountContext.setPolicyDecision(policyDecision);
        if (activationDecision != null) {
            updateAccountActivation(accountContext, activationDecision);
        }
        boolean doReconciliation = determineAttributeReconciliation(change);
        accountContext.setDoReconciliation(doReconciliation);

        LOGGER.debug("Setting account context policy decision ({}), activation decision ({}), do reconciliation ({})",
                new Object[]{policyDecision, activationDecision, doReconciliation});

        return accountContext;
    }

	private boolean isThombstone(ResourceObjectShadowChangeDescription change) {
		ObjectDelta<? extends ResourceObjectShadowType> objectDelta = change.getObjectDelta();
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isDelete();
	}

	private void updateAccountActivation(LensProjectionContext<AccountShadowType> accContext, ActivationDecision activationDecision) throws SchemaException {
        PrismObject<AccountShadowType> object = accContext.getObjectOld();
        if (object == null) {
            LOGGER.debug("Account object is null, skipping activation property check/update.");
            return;
        }

        PrismProperty enable = object.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ENABLE);
        LOGGER.debug("Account activation defined, activation property found {}", enable);

        ObjectDelta<AccountShadowType> accDelta = accContext.getSecondaryDelta();
        if (accDelta == null) {
            accDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.MODIFY, prismContext);
            accDelta.setOid(accContext.getOid());
            accContext.setSecondaryDelta(accDelta);
        }

        PrismPropertyValue<Boolean> value = enable.getValue(Boolean.class);
        if (value != null) {
            Boolean isEnabled = value.getValue();
            if (isEnabled == null) {
                createActivationPropertyDelta(accDelta, activationDecision, null);
            }

            if ((isEnabled && ActivationDecision.DISABLE.equals(activationDecision))
                    || (!isEnabled && ActivationDecision.ENABLE.equals(activationDecision))) {

                createActivationPropertyDelta(accDelta, activationDecision, isEnabled);
            }
        } else {
            createActivationPropertyDelta(accDelta, activationDecision, null);
        }
    }

    private boolean determineAttributeReconciliation(ResourceObjectShadowChangeDescription change) {
    	ObjectSynchronizationType synchronization = ResourceTypeUtil.determineSynchronization(change.getResource().asObjectable(), UserType.class);
        Boolean reconcileAttributes = synchronization.isReconcileAttributes();
        if (reconcileAttributes == null) {
            // "Automatic mode", do reconciliation only if the complete current shadow was provided
            reconcileAttributes = change.getCurrentShadow() != null;
            LOGGER.trace("Attribute reconciliation automatic mode: {}", reconcileAttributes);
        } else {
            LOGGER.trace("Attribute reconciliation manual mode: {}", reconcileAttributes);
        }
        return reconcileAttributes;
    }

    private PrismObject<AccountShadowType> getAccountObject(ResourceObjectShadowChangeDescription change)
            throws SchemaException {

        AccountShadowType account = getAccountShadowFromChange(change);
        if (account == null) {
            return null;
        }

        PrismObjectDefinition<AccountShadowType> definition = RefinedResourceSchema.getRefinedSchema(
                change.getResource().asObjectable(), getPrismContext()).getObjectDefinition(account);

        return account.asPrismObject();
    }

    protected AccountShadowType getAccountShadowFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getCurrentShadow() != null) {
            return (AccountShadowType) change.getCurrentShadow().asObjectable();
        }

        if (change.getOldShadow() != null) {
            return (AccountShadowType) change.getOldShadow().asObjectable();
        }

        return null;
    }

    private String getAccountTypeFromChange(ResourceObjectShadowChangeDescription change) {
        AccountShadowType account = getAccountShadowFromChange(change);
        if (account != null) {
            return ResourceObjectShadowUtil.getIntent(account);
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

    protected void synchronizeUser(LensContext<UserType, AccountShadowType> context, Task task, OperationResult result) throws SynchronizationException {
        try {
            Validate.notNull(context, "Sync context must not be null.");
            Validate.notNull(result, "Operation result must not be null.");

            clockwork.run(context, task, result);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't synchronize user, reason: " + ex.getMessage(), ex);
        }
    }

    protected void executeChanges(LensContext<UserType, AccountShadowType> context, OperationResult result) throws SynchronizationException {
        try {
            Validate.notNull(context, "Sync context must not be null.");
            getExecutor().executeChanges(context, result);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't execute changes from context, reason: " + ex.getMessage(), ex);
        }
    }

    protected void createActivationPropertyDelta(ObjectDelta<?> objectDelta, ActivationDecision activationDecision,
            Boolean oldValue) {
        LOGGER.debug("Updating activation for {}, activation decision {}, old value was {}",
                new Object[]{objectDelta.getClass().getSimpleName(), activationDecision, oldValue});

        PropertyDelta delta = objectDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ENABLE);
        if (delta == null) {
            delta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ENABLE, ResourceObjectShadowType.class,
            		getPrismContext());
            objectDelta.addModification(delta);
        }
        delta.clear();

        Boolean newValue = ActivationDecision.ENABLE.equals(activationDecision) ? Boolean.TRUE : Boolean.FALSE;
        PrismPropertyValue value = new PrismPropertyValue<Object>(newValue, SourceType.SYNC_ACTION, null);
        if (oldValue == null) {
            delta.addValueToAdd(value);
        } else {
            Collection<PrismPropertyValue<Object>> values = new ArrayList<PrismPropertyValue<Object>>();
            values.add(value);
            delta.setValuesToReplace(values);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} activation property delta: {}", new Object[]{objectDelta.getClass().getSimpleName(),
                    delta.debugDump()});
        }
    }
}

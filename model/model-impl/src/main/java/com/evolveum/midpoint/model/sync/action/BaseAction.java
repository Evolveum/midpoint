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

package com.evolveum.midpoint.model.sync.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.ContextFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author lazyman
 */
public abstract class BaseAction implements Action {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAction.class);

    private Clockwork clockwork;
    private ChangeExecutor executor;
    private ModelController model;
    private List<Object> parameters;
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

    protected UserType getUser(String oid, OperationResult result) {
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        try {
            return model.getObjectResolver().getObjectSimple(UserType.class, oid, null, null, result);
        } catch (ObjectNotFoundException ex) {
            // user was not found, we return null
        	return null;
        }
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate, 
            SynchronizationSituationType situation, Task task, OperationResult result) 
    		throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
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
    
    /**
     * Creates empty lens context, filling in only the very basic metadata (such as channel).
     */
    protected <F extends FocusType> LensContext<F> createEmptyLensContext(ResourceObjectShadowChangeDescription change) {
    	return contextFactory.createSyncContext(change);
    }

    protected <F extends FocusType> LensProjectionContext createAccountLensContext(LensContext<F> context,
            ResourceObjectShadowChangeDescription change, SynchronizationIntent syncIntent,
            ActivationDecision activationDecision) throws SchemaException {
        LOGGER.trace("Creating account context for sync change.");

        ResourceType resource = change.getResource().asObjectable();

        String accountType = getAccountTypeFromChange(change);
        boolean thombstone = isThombstone(change);
		ResourceShadowDiscriminator resourceAccountType = new ResourceShadowDiscriminator(resource.getOid(), accountType, thombstone);
		LensProjectionContext accountContext = context.createProjectionContext(resourceAccountType);
        accountContext.setResource(resource);
        accountContext.setOid(getOidFromChange(change));

        //insert object delta if available in change
        ObjectDelta<? extends ShadowType> delta = change.getObjectDelta();
        if (delta != null) {
            accountContext.setSyncDelta((ObjectDelta<ShadowType>) delta);
        }

        //we insert account if available in change
        accountContext.setLoadedObject(getAccountObject(change));

        if (delta != null && delta.isDelete()) {
        	accountContext.setExists(false);
        } else {
        	accountContext.setExists(true);
        }
        
        accountContext.setSynchronizationIntent(syncIntent);
        if (activationDecision != null) {
            updateAccountActivation(accountContext, activationDecision);
        }
        
        boolean doReconciliation = determineAttributeReconciliation(change);
        accountContext.setDoReconciliation(doReconciliation);

        LOGGER.trace("Setting account context sync intent ({}), activation decision ({}), do reconciliation ({})",
                new Object[]{syncIntent, activationDecision, doReconciliation});

        return accountContext;
    }

	private boolean isThombstone(ResourceObjectShadowChangeDescription change) {
		PrismObject<? extends ShadowType> shadow = null;
		if (change.getOldShadow() != null){
			shadow = change.getOldShadow();
		} else if (change.getCurrentShadow() != null){
			shadow = change.getCurrentShadow();
		}
		if (shadow != null){
			if (shadow.asObjectable().isDead() != null){
				return shadow.asObjectable().isDead().booleanValue();
			}
		}
		ObjectDelta<? extends ShadowType> objectDelta = change.getObjectDelta();
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isDelete();
	}

	private void updateAccountActivation(LensProjectionContext accContext, ActivationDecision activationDecision) throws SchemaException {
        PrismObject<ShadowType> object = accContext.getObjectOld();
        if (object == null) {
            LOGGER.trace("Account object is null, skipping activation property check/update.");
            return;
        }

        PrismProperty enable = object.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        LOGGER.trace("Account activation defined, activation property found {}", enable);

        ObjectDelta<ShadowType> accDelta = accContext.getSecondaryDelta();
        if (accDelta == null) {
            accDelta = new ObjectDelta<ShadowType>(ShadowType.class, ChangeType.MODIFY, prismContext);
            accDelta.setOid(accContext.getOid());
            accContext.setSecondaryDelta(accDelta);
        }

        PrismPropertyValue<ActivationStatusType> value = enable.getValue(ActivationStatusType.class);
        if (value != null) {
            ActivationStatusType status = value.getValue();
            if (status == null) {
                createActivationPropertyDelta(accDelta, activationDecision, null);
            }
            
            Boolean isEnabled = ActivationStatusType.ENABLED == status ? Boolean.TRUE : Boolean.FALSE;

            if ((isEnabled && ActivationDecision.DISABLE.equals(activationDecision))
                    || (!isEnabled && ActivationDecision.ENABLE.equals(activationDecision))) {

                createActivationPropertyDelta(accDelta, activationDecision, status);
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

    private PrismObject<ShadowType> getAccountObject(ResourceObjectShadowChangeDescription change)
            throws SchemaException {

    	PrismObject<ShadowType> account = getAccountShadowFromChange(change);
        if (account == null) {
            return null;
        }
        
        if (InternalsConfig.consistencyChecks) account.checkConsistence();
        return account;
    }

    protected PrismObject<ShadowType> getAccountShadowFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getCurrentShadow() != null) {
            return (PrismObject<ShadowType>) change.getCurrentShadow();
        }

        if (change.getOldShadow() != null) {
            return (PrismObject<ShadowType>) change.getOldShadow();
        }

        return null;
    }

    private String getAccountTypeFromChange(ResourceObjectShadowChangeDescription change) {
    	PrismObject<ShadowType> account = getAccountShadowFromChange(change);
        if (account != null) {
            return ShadowUtil.getIntent(account.asObjectable());
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

    protected <F extends FocusType> void synchronizeUser(LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
    	Validate.notNull(context, "Sync context must not be null.");
        Validate.notNull(result, "Operation result must not be null.");
        try {
        	context.setLazyAuditRequest(true);
            clockwork.run(context, task, result);
        } catch (SchemaException e) {
        	logSynchronizationError(e, context);
            throw e;
		} catch (PolicyViolationException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (ExpressionEvaluationException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (ObjectNotFoundException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (ObjectAlreadyExistsException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (CommunicationException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (ConfigurationException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (SecurityViolationException e) {
			logSynchronizationError(e, context);
            throw e;
		} catch (RuntimeException ex) {
        	logSynchronizationError(ex, context);
            throw ex;
		}
    }

	private <F extends FocusType> void logSynchronizationError(Throwable ex, LensContext<F> context) {
		if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Synchronization error: {}\n{})", ex.getMessage(), context.dump());
    	}
	}

	protected <F extends FocusType> void executeChanges(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notNull(context, "Sync context must not be null.");
        try {
            getExecutor().executeChanges(context, task, result);
        } catch (RuntimeException ex) {
        	logExecutionError(ex, context);
            throw ex;
        } catch (ObjectAlreadyExistsException e) {
        	logExecutionError(e, context);
            throw e;
		} catch (ObjectNotFoundException e) {
			logExecutionError(e, context);
            throw e;
		} catch (SchemaException e) {
			logExecutionError(e, context);
            throw e;
		} catch (CommunicationException e) {
			logExecutionError(e, context);
            throw e;
		} catch (ConfigurationException e) {
			logExecutionError(e, context);
            throw e;
		} catch (SecurityViolationException e) {
			logExecutionError(e, context);
            throw e;
		} catch (ExpressionEvaluationException e) {
			logExecutionError(e, context);
            throw e;
		} 
    }
	
	private <F extends FocusType> void logExecutionError(Throwable ex, LensContext<F> context) {
		if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Execution error: {}\n{})", ex.getMessage(), context.dump());
    	}
	}

    protected void createActivationPropertyDelta(ObjectDelta<?> objectDelta, ActivationDecision activationDecision,
            ActivationStatusType oldValue) {
        LOGGER.trace("Updating activation for {}, activation decision {}, old value was {}",
                new Object[]{objectDelta.getClass().getSimpleName(), activationDecision, oldValue});

        PropertyDelta delta = objectDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (delta == null) {
            delta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ShadowType.class,
            		getPrismContext());
            objectDelta.addModification(delta);
        }
        delta.clear();

        ActivationStatusType newValue = ActivationDecision.ENABLE.equals(activationDecision) ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
        PrismPropertyValue value = new PrismPropertyValue<Object>(newValue, OriginType.SYNC_ACTION, null);
        if (oldValue == null) {
            delta.addValueToAdd(value);
        } else {
            Collection<PrismPropertyValue<Object>> values = new ArrayList<PrismPropertyValue<Object>>();
            values.add(value);
            delta.setValuesToReplace(values);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("{} activation property delta: {}", new Object[]{objectDelta.getClass().getSimpleName(),
                    delta.debugDump()});
        }
    }
}

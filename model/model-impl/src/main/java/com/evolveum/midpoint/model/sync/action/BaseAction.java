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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.CompiletimeConfig;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.RewindException;
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
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
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

    protected UserType getUser(String oid, OperationResult result) {
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        try {
            return model.getObjectResolver().getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException ex) {
            // user was not found, we return null
        	return null;
        }
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) 
    		throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
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
    protected LensContext<UserType, ResourceObjectShadowType> createEmptyLensContext(ResourceObjectShadowChangeDescription change) {
    	LensContext<UserType, ResourceObjectShadowType> context = new LensContext<UserType, ResourceObjectShadowType>(UserType.class, ResourceObjectShadowType.class, getPrismContext());
    	context.setChannel(change.getSourceChannel());
    	return context;
    }

    protected LensProjectionContext<ResourceObjectShadowType> createAccountLensContext(LensContext<UserType, ResourceObjectShadowType> context,
            ResourceObjectShadowChangeDescription change, SynchronizationIntent syncIntent,
            ActivationDecision activationDecision) throws SchemaException {
        LOGGER.trace("Creating account context for sync change.");

        ResourceType resource = change.getResource().asObjectable();

        String accountType = getAccountTypeFromChange(change);
        boolean thombstone = isThombstone(change);
		ResourceShadowDiscriminator resourceAccountType = new ResourceShadowDiscriminator(resource.getOid(), accountType, thombstone);
		LensProjectionContext<ResourceObjectShadowType> accountContext = context.createProjectionContext(resourceAccountType);
        accountContext.setResource(resource);
        accountContext.setOid(getOidFromChange(change));

        //insert object delta if available in change
        ObjectDelta<? extends ResourceObjectShadowType> delta = change.getObjectDelta();
        if (delta != null) {
            accountContext.setSyncDelta((ObjectDelta<ResourceObjectShadowType>) delta);
        }

        //we insert account if available in change
        accountContext.setObjectOld(getAccountObject(change));

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
		PrismObject<? extends ResourceObjectShadowType> shadow = null;
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
		ObjectDelta<? extends ResourceObjectShadowType> objectDelta = change.getObjectDelta();
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isDelete();
	}

	private void updateAccountActivation(LensProjectionContext<ResourceObjectShadowType> accContext, ActivationDecision activationDecision) throws SchemaException {
        PrismObject<ResourceObjectShadowType> object = accContext.getObjectOld();
        if (object == null) {
            LOGGER.trace("Account object is null, skipping activation property check/update.");
            return;
        }

        PrismProperty enable = object.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ENABLE);
        LOGGER.trace("Account activation defined, activation property found {}", enable);

        ObjectDelta<ResourceObjectShadowType> accDelta = accContext.getSecondaryDelta();
        if (accDelta == null) {
            accDelta = new ObjectDelta<ResourceObjectShadowType>(ResourceObjectShadowType.class, ChangeType.MODIFY, prismContext);
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

    private PrismObject<ResourceObjectShadowType> getAccountObject(ResourceObjectShadowChangeDescription change)
            throws SchemaException {

    	PrismObject<ResourceObjectShadowType> account = getAccountShadowFromChange(change);
        if (account == null) {
            return null;
        }
        
        if (CompiletimeConfig.CONSISTENCY_CHECKS) account.checkConsistence();
        return account;
    }

    protected PrismObject<ResourceObjectShadowType> getAccountShadowFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getCurrentShadow() != null) {
            return (PrismObject<ResourceObjectShadowType>) change.getCurrentShadow();
        }

        if (change.getOldShadow() != null) {
            return (PrismObject<ResourceObjectShadowType>) change.getOldShadow();
        }

        return null;
    }

    private String getAccountTypeFromChange(ResourceObjectShadowChangeDescription change) {
    	PrismObject<ResourceObjectShadowType> account = getAccountShadowFromChange(change);
        if (account != null) {
            return ResourceObjectShadowUtil.getIntent(account.asObjectable());
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

    protected void synchronizeUser(LensContext<UserType, ResourceObjectShadowType> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
    	Validate.notNull(context, "Sync context must not be null.");
        Validate.notNull(result, "Operation result must not be null.");
        try {
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
		} catch (RewindException e) {
			logSynchronizationError(e, context);
            throw e;
        } catch (RuntimeException ex) {
        	logSynchronizationError(ex, context);
            throw ex;
		}
    }

	private void logSynchronizationError(Throwable ex, LensContext<UserType,ResourceObjectShadowType> context) {
		if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Synchronization error: {}\n{})", ex.getMessage(), context.dump());
    	}
	}

	protected void executeChanges(LensContext<UserType, ResourceObjectShadowType> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, RewindException {
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
		} catch (RewindException e) {
			logExecutionError(e, context);
            throw e;
		}
    }
	
	private void logExecutionError(Throwable ex, LensContext<UserType,ResourceObjectShadowType> context) {
		if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Execution error: {}\n{})", ex.getMessage(), context.dump());
    	}
	}

    protected void createActivationPropertyDelta(ObjectDelta<?> objectDelta, ActivationDecision activationDecision,
            Boolean oldValue) {
        LOGGER.trace("Updating activation for {}, activation decision {}, old value was {}",
                new Object[]{objectDelta.getClass().getSimpleName(), activationDecision, oldValue});

        PropertyDelta delta = objectDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ENABLE);
        if (delta == null) {
            delta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ENABLE, ResourceObjectShadowType.class,
            		getPrismContext());
            objectDelta.addModification(delta);
        }
        delta.clear();

        Boolean newValue = ActivationDecision.ENABLE.equals(activationDecision) ? Boolean.TRUE : Boolean.FALSE;
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

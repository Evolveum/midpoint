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
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.DefaultMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.marshaller.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.SynchronizationIntent;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author semancik
 *
 */
@Component
public class MidpointFunctionsImpl implements MidpointFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointFunctionsImpl.class);

    @Autowired(required=true)
    private PrismContext prismContext;

    @Autowired(required=true)
    private ModelService modelService;
    
    @Autowired(required=true)
    private ModelObjectResolver modelObjectResolver;

    @Autowired(required=true)
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private ProvisioningService provisioningService;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired(required = true)
    private transient Protector protector;

    @Autowired
    private OrgStructFunctionsImpl orgStructFunctions;
    
    public String hello(String name) {
        return "Hello "+name;
    }
    
    public PrismContext getPrismContext() {
		return prismContext;
	}

    @Override
    public List<String> toList(String... s) {
        return Arrays.asList(s);
    }


    @Override
    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, null, new OperationResult("getUserByOid")).asObjectable();
    }

	@Override
    public boolean isMemberOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPlaintextUserPassword(UserType user) throws EncryptionException {
        if (user == null || user.getCredentials() == null || user.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = user.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPassword(ShadowType account) throws EncryptionException {
        if (account == null || account.getCredentials() == null || account.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = account.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException {

        if (delta.isAdd()) {
            ShadowType newShadow = delta.getObjectToAdd().asObjectable();
            return getPlaintextAccountPassword(newShadow);
        }
        if (!delta.isModify()) {
            return null;
        }

        List<ProtectedStringType> passwords = new ArrayList<ProtectedStringType>();
        for (ItemDelta itemDelta : delta.getModifications()) {
            takePasswordsFromItemDelta(passwords, itemDelta);
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size()-1));
        } else {
            return null;
        }
    }

    private void takePasswordsFromItemDelta(List<ProtectedStringType> passwords, ItemDelta itemDelta) {
        if (itemDelta.isDelete()) {
            return;
        }

        if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE))) {
            LOGGER.trace("Found password value add/modify delta");
            Collection<PrismPropertyValue<ProtectedStringType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismPropertyValue<ProtectedStringType> value : values) {
                passwords.add(value.getValue());
            }
        } else if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD))) {
            LOGGER.trace("Found password add/modify delta");
            Collection<PrismContainerValue<PasswordType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismContainerValue<PasswordType> value : values) {
                if (value.asContainerable().getValue() != null) {
                    passwords.add(value.asContainerable().getValue());
                }
            }
        } else if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS))) {
            LOGGER.trace("Found credentials add/modify delta");
            Collection<PrismContainerValue<CredentialsType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismContainerValue<CredentialsType> value : values) {
                if (value.asContainerable().getPassword() != null && value.asContainerable().getPassword().getValue() != null) {
                    passwords.add(value.asContainerable().getPassword().getValue());
                }
            }
        }
    }

    @Override
    public String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<UserType>> objectDeltas) throws EncryptionException {

        List<ProtectedStringType> passwords = new ArrayList<ProtectedStringType>();

        for (ObjectDelta<UserType> delta : objectDeltas) {

            if (delta.isAdd()) {
                UserType newUser = delta.getObjectToAdd().asObjectable();
                return getPlaintextUserPassword(newUser);       // for simplicity we do not look for other values
            }

            if (!delta.isModify()) {
                continue;
            }

            for (ItemDelta itemDelta : delta.getModifications()) {
                takePasswordsFromItemDelta(passwords, itemDelta);
            }
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size()-1));
        } else {
            return null;
        }
    }
    
    public <F extends ObjectType> boolean hasLinkedAccount(String resourceOid) {
    	LensContext<F> ctx = ModelExpressionThreadLocalHolder.getLensContext();
    	if (ctx == null) {
    		throw new IllegalStateException("No lens context");
    	}
    	LensFocusContext<F> focusContext = ctx.getFocusContext();
    	if (focusContext == null) {
    		throw new IllegalStateException("No focus in lens context");
    	}

        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();

    	ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, ShadowKindType.ACCOUNT, null);
		LensProjectionContext projectionContext = ctx.findProjectionContext(rat);
		if (projectionContext == null) {
			// but check if it is not among list of deleted contexts
            if (scriptContext == null || scriptContext.isEvaluateNew()) {
                return false;
            }
            // evaluating old state
            for (ResourceShadowDiscriminator deletedOne: ctx.getHistoricResourceObjects()) {
                if (resourceOid.equals(deletedOne.getResourceOid()) && deletedOne.getKind() == ShadowKindType.ACCOUNT
                        && deletedOne.getIntent() == null || "default".equals(deletedOne.getIntent())) {        // TODO implement this seriously
                    LOGGER.trace("Found deleted one: {}", deletedOne);  // TODO remove
                    return true;
                }
            }
            return false;
		}

		if (projectionContext.isThombstone()) {
			return false;
		}
		
		SynchronizationPolicyDecision synchronizationPolicyDecision = projectionContext.getSynchronizationPolicyDecision();
		SynchronizationIntent synchronizationIntent = projectionContext.getSynchronizationIntent();
		if (scriptContext == null) {
			if (synchronizationPolicyDecision == null) {
				if (synchronizationIntent == SynchronizationIntent.DELETE || synchronizationIntent == SynchronizationIntent.UNLINK) {
					return false;
				} else {
					return true;
				}
			} else {
				if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE || synchronizationPolicyDecision == SynchronizationPolicyDecision.UNLINK) {
					return false;
				} else {
					return true;
				}
			}
		} else if (scriptContext.isEvaluateNew()) {
			// Evaluating new state
			if (focusContext.isDelete()) {
				return false;
			}
			if (synchronizationPolicyDecision == null) {
				if (synchronizationIntent == SynchronizationIntent.DELETE || synchronizationIntent == SynchronizationIntent.UNLINK) {
					return false;
				} else {
					return true;
				}
			} else {
				if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE || synchronizationPolicyDecision == SynchronizationPolicyDecision.UNLINK) {
					return false;
				} else {
					return true;
				}
			}
		} else {
	    	// Evaluating old state
	    	if (focusContext.isAdd()) {
	    		return false;
	    	}
	    	if (synchronizationPolicyDecision == null) {
				if (synchronizationIntent == SynchronizationIntent.ADD) {
					return false;
				} else {
					return true;
				}
			} else {
				if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD) {
					return false;
				} else {
					return true;
				}
			}
	    }
    }
    
    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focusType, String targetOid) {
    	for (AssignmentType assignment: focusType.getAssignment()) {
    		ObjectReferenceType targetRef = assignment.getTargetRef();
    		if (targetRef != null && targetRef.getOid().equals(targetOid)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focusType, ObjectType target) {
    	return isDirectlyAssigned(focusType, target.getOid());
    }
    
    @Override
    public boolean isDirectlyAssigned(String targetOid) {
    	LensContext<? extends FocusType> ctx = ModelExpressionThreadLocalHolder.getLensContext();
    	if (ctx == null) {
    		throw new IllegalStateException("No lens context");
    	}
    	LensFocusContext<? extends FocusType> focusContext = ctx.getFocusContext();
    	if (focusContext == null) {
    		throw new IllegalStateException("No focus in lens context");
    	}
    	
    	PrismObject<? extends FocusType> focus;
    	ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
    	if (scriptContext == null) {
    		focus = focusContext.getObjectAny();
    	} else if (scriptContext.isEvaluateNew()) {
			// Evaluating new state
			if (focusContext.isDelete()) {
				return false;
			}
			focus = focusContext.getObjectNew();
		} else {
	    	// Evaluating old state
	    	if (focusContext.isAdd()) {
	    		return false;
	    	}
	    	focus = focusContext.getObjectOld();
	    }
    	if (focus == null) {
    		return false;
    	}
    	return isDirectlyAssigned(focus.asObjectable(), targetOid);
    }
    
    @Override
    public boolean isDirectlyAssigned(ObjectType target) {
    	return isDirectlyAssigned(target.getOid());
    }

    
    @Override
	public ShadowType getLinkedShadow(FocusType focus, ResourceType resource) throws SchemaException,
			SecurityViolationException, CommunicationException, ConfigurationException {
		return getLinkedShadow(focus, resource.getOid());
	}

    @Override
	public ShadowType getLinkedShadow(FocusType focus, ResourceType resource, boolean repositoryObjectOnly) throws SchemaException,
			SecurityViolationException, CommunicationException, ConfigurationException {
		return getLinkedShadow(focus, resource.getOid(), repositoryObjectOnly);
	}

    
    @Override
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
    	return getLinkedShadow(focus, resourceOid, false);
    }
    
	@Override
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid, boolean repositoryObjectOnly) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		if (focus == null) {
			return null;
		}
		List<ObjectReferenceType> linkRefs = focus.getLinkRef();
		for (ObjectReferenceType linkRef: linkRefs) {
			ShadowType shadowType;
			try {
				shadowType = getObject(ShadowType.class, linkRef.getOid(), SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));
			} catch (ObjectNotFoundException e) {
				// Shadow is gone in the meantime. MidPoint will resolve that by itself.
				// It is safe to ignore this error in this method.
				LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists in repository");
				continue;
			}
			if (shadowType.getResourceRef().getOid().equals(resourceOid)) {
				// We have repo shadow here. Re-read resource shadow if necessary.
				if (!repositoryObjectOnly) {
					try {
						shadowType = getObject(ShadowType.class, shadowType.getOid());
					} catch (ObjectNotFoundException e) {
						// Shadow is gone in the meantime. MidPoint will resolve that by itself.
						// It is safe to ignore this error in this method.
						LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists on resource");
						continue;
					}	
				}
				return shadowType;
			}
		}
		return null;
    }
    
	@Override
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getLinkedShadow(focus, resourceOid, kind, intent, false);
	}
	
    @Override
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent, boolean repositoryObjectOnly) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		List<ObjectReferenceType> linkRefs = focus.getLinkRef();
		for (ObjectReferenceType linkRef: linkRefs) {
			ShadowType shadowType;
			try {
				shadowType = getObject(ShadowType.class, linkRef.getOid(), SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));
			} catch (ObjectNotFoundException e) {
				// Shadow is gone in the meantime. MidPoint will resolve that by itself.
				// It is safe to ignore this error in this method.
				LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists in repository");
				continue;
			}
			if (ShadowUtil.matches(shadowType, resourceOid, kind, intent)) {
				// We have repo shadow here. Re-read resource shadow if necessary.
				if (!repositoryObjectOnly) {
					try {
						shadowType = getObject(ShadowType.class, shadowType.getOid());
					} catch (ObjectNotFoundException e) {
						// Shadow is gone in the meantime. MidPoint will resolve that by itself.
						// It is safe to ignore this error in this method.
						LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists on resource");
						continue;
					}	
				}
				return shadowType;
			}
		}
		return null;
	}
    
    @Override
    public boolean isFullShadow() {
    	LensProjectionContext projectionContext = getProjectionContext();
    	if (projectionContext == null) {
    		LOGGER.debug("Call to isFullShadow while there is no projection context");
    		return false;
    	}
    	return projectionContext.isFullShadow();
    }

	public <T> Integer countAccounts(String resourceOid, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	ResourceType resourceType = modelObjectResolver.getObjectSimple(ResourceType.class, resourceOid, null, null, result);
    	return countAccounts(resourceType, attributeName, attributeValue, getCurrentTask(), result);
    }
    
    public <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	return countAccounts(resourceType, attributeName, attributeValue, getCurrentTask(), result);
    }
    
    public <T> Integer countAccounts(ResourceType resourceType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return countAccounts(resourceType, attributeQName, attributeValue, getCurrentTask(), result);
    }
    
    private <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue, Task task, OperationResult result)
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getName()).eq(attributeValue)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccountDef.getObjectClassDefinition().getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
				.build();
		return modelObjectResolver.countObjects(ShadowType.class, query, null, task, result);
    }

    public <T> boolean isUniquePropertyValue(ObjectType objectType, String propertyPathString, T propertyValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".isUniquePropertyValue");
        ItemPath propertyPath = new XPathHolder(propertyPathString).toItemPath();
        return isUniquePropertyValue(objectType, propertyPath, propertyValue, getCurrentTask(), result);
    }

    private <T> boolean isUniquePropertyValue(final ObjectType objectType, ItemPath propertyPath, T propertyValue, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
		List<? extends ObjectType> conflictingObjects = getObjectsInConflictOnPropertyValue(objectType, propertyPath, propertyValue, null, false, task, result);
        return conflictingObjects.isEmpty();
    }

    public <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(O objectType, String propertyPathString, T propertyValue, boolean getAllConflicting) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        return getObjectsInConflictOnPropertyValue(objectType, propertyPathString, propertyValue, DefaultMatchingRule.NAME.getLocalPart(), getAllConflicting);
    }

    public <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(O objectType, String propertyPathString, T propertyValue, String matchingRuleName, boolean getAllConflicting) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".getObjectsInConflictOnPropertyValue");
        ItemPath propertyPath = new XPathHolder(propertyPathString).toItemPath();
        QName matchingRuleQName = new QName(matchingRuleName);      // no namespace for now
        return getObjectsInConflictOnPropertyValue(objectType, propertyPath, propertyValue, matchingRuleQName, getAllConflicting, getCurrentTask(), result);
    }

    private <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(final O objectType, ItemPath propertyPath, T propertyValue, QName matchingRule, final boolean getAllConflicting, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        Validate.notNull(objectType, "Null object");
        Validate.notNull(propertyPath, "Null property path");
        Validate.notNull(propertyValue, "Null property value");
        PrismPropertyDefinition<T> propertyDefinition = objectType.asPrismObject().getDefinition().findPropertyDefinition(propertyPath);
        if (matchingRule == null) {
        	if (propertyDefinition != null && PolyStringType.COMPLEX_TYPE.equals(propertyDefinition.getTypeName())) {
        		matchingRule = PolyStringOrigMatchingRule.NAME;
        	} else {
        		matchingRule = DefaultMatchingRule.NAME;
        	}
        }
        ObjectQuery query = QueryBuilder.queryFor(objectType.getClass(), prismContext)
				.item(propertyPath, propertyDefinition).eq(propertyValue).matching(matchingRule)
				.build();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Determining uniqueness of property {} using query:\n{}", propertyPath, query.debugDump());
        }

        final List<O> conflictingObjects = new ArrayList<>();
        ResultHandler<O> handler = new ResultHandler<O>() {
            @Override
            public boolean handle(PrismObject<O> object, OperationResult parentResult) {
                if (objectType.getOid() == null) {
                    // We have found a conflicting object
                    conflictingObjects.add(object.asObjectable());
                    return getAllConflicting;
                } else {
                    if (objectType.getOid().equals(object.getOid())) {
                        // We have found ourselves. No conflict (yet). Just go on.
                        return true;
                    } else {
                        // We have found someone else. Conflict.
                        conflictingObjects.add(object.asObjectable());
                        return getAllConflicting;
                    }
                }
            }
        };

        modelObjectResolver.searchIterative((Class) objectType.getClass(), query, null, handler, task, result);

        return conflictingObjects;
    }

    public <T> boolean isUniqueAccountValue(ResourceType resourceType, ShadowType shadowType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	Validate.notEmpty(attributeName,"Empty attribute name");
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".isUniqueAccountValue");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return isUniqueAccountValue(resourceType, shadowType, attributeQName, attributeValue, getCurrentTask(), result);
    }

    private <T> boolean isUniqueAccountValue(ResourceType resourceType, final ShadowType shadowType,
    		QName attributeName, T attributeValue, Task task, OperationResult result)
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	Validate.notNull(resourceType, "Null resource");
    	Validate.notNull(shadowType, "Null shadow");
    	Validate.notNull(attributeName, "Null attribute name");
    	Validate.notNull(attributeValue, "Null attribute value");
    	RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getName()).eq(attributeValue)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccountDef.getObjectClassDefinition().getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
				.build();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Determining uniqueness of attribute {} using query:\n{}", attributeName, query.debugDump());
		}
        
        final Holder<Boolean> isUniqueHolder = new Holder<Boolean>(true);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				if (shadowType == null || shadowType.getOid() == null) {
					// We have found a conflicting object
					isUniqueHolder.setValue(false);
					return false;
				} else {
					if (shadowType.getOid().equals(object.getOid())) {
						// We have found ourselves. No conflict (yet). Just go on.
						return true;
					} else {
						// We have found someone else. Conflict.
						isUniqueHolder.setValue(false);
						return false;
					}
				}
			}
		};
        
		modelObjectResolver.searchIterative(ShadowType.class, query, null, handler, task, result);
		
		return isUniqueHolder.getValue();
    }
    
    private LensProjectionContext getProjectionContext() {
    	return ModelExpressionThreadLocalHolder.getProjectionContext();
    }
    
    private Task getCurrentTask() {
    	return ModelExpressionThreadLocalHolder.getCurrentTask();
    }
    
    private OperationResult getCurrentResult() {
    	return ModelExpressionThreadLocalHolder.getCurrentResult();
    }
    
    private OperationResult getCurrentResult(String operationName) {
    	OperationResult currentResult = ModelExpressionThreadLocalHolder.getCurrentResult();
    	if (currentResult == null) {
    		return new OperationResult(operationName);
    	} else {
    		return currentResult;
    	}
    }
        
    private OperationResult createSubresult(String operationName) {
    	OperationResult currentResult = ModelExpressionThreadLocalHolder.getCurrentResult();
    	if (currentResult == null) {
    		return new OperationResult(operationName);
    	} else {
    		return currentResult.createSubresult(operationName);
    	}
    }

    private OperationResult createMinorSubresult(String operationName) {
    	OperationResult currentResult = ModelExpressionThreadLocalHolder.getCurrentResult();
    	if (currentResult == null) {
    		return new OperationResult(operationName);
    	} else {
    		return currentResult.createMinorSubresult(operationName);
    	}
    }

    // functions working with ModelContext

    @Override
    public ModelContext unwrapModelContext(LensContextType lensContextType) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        return LensContext.fromLensContextType(lensContextType, prismContext, provisioningService, getCurrentResult(MidpointFunctions.class.getName()+".getObject"));
    }

    public LensContextType wrapModelContext(LensContext<?> lensContext) throws SchemaException {
        return lensContext.toLensContextType();
    }
    
    // Convenience functions
    
	@Override
	public <T extends ObjectType> T createEmptyObject(Class<T> type) throws SchemaException {
		PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismObject<T> object = objectDefinition.instantiate();
		return object.asObjectable();
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, String name) throws SchemaException {
		T objectType = createEmptyObject(type);
		objectType.setName(new PolyStringType(name));
		return objectType;
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyString name) throws SchemaException {
		T objectType = createEmptyObject(type);
		objectType.setName(new PolyStringType(name));
		return objectType;
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyStringType name) throws SchemaException {
		T objectType = createEmptyObject(type);
		objectType.setName(name);
		return objectType;
	}
    
    // Functions accessing modelService

    @Override
    public <T extends ObjectType> T resolveReference(ObjectReferenceType reference)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException {
        if (reference == null) {
            return null;
        }
        QName type = reference.getType();           // TODO what about implicitly specified types, like in resourceRef?
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(reference.getType());
        if (objectDefinition == null) {
            throw new SchemaException("No definition for type " + type);
        }
        return modelService.getObject(
                objectDefinition.getCompileTimeClass(), reference.getOid(), null, getCurrentTask(), getCurrentResult()).asObjectable();
    }

    @Override
    public <T extends ObjectType> T resolveReferenceIfExists(ObjectReferenceType reference)
            throws SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException {
        try {
            return resolveReference(reference);
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
	public <T extends ObjectType> T getObject(Class<T> type, String oid, 
			Collection<SelectorOptions<GetOperationOptions>> options)
			throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException {
		return modelService.getObject(type, oid, options, getCurrentTask(), getCurrentResult()).asObjectable();
	}

	@Override
	public <T extends ObjectType> T getObject(Class<T> type, String oid) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<T> prismObject = modelService.getObject(type, oid, null, getCurrentTask(), getCurrentResult());
		return prismObject.asObjectable();
	}

	@Override
	public void executeChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas,
			ModelExecuteOptions options) throws ObjectAlreadyExistsException,
			ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		modelService.executeChanges(deltas, options, getCurrentTask(), getCurrentResult());
	}

	@Override
	public void executeChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		modelService.executeChanges(deltas, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public void executeChanges(ObjectDelta<? extends ObjectType>... deltas)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deltas);
		modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> newObject,
			ModelExecuteOptions options) throws ObjectAlreadyExistsException,
			ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		ObjectDelta<T> delta = ObjectDelta.createAddDelta(newObject);
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(delta);
		modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
		return delta.getOid();
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> newObject)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		return addObject(newObject, null);
	}

	@Override
	public <T extends ObjectType> String addObject(T newObject,
			ModelExecuteOptions options) throws ObjectAlreadyExistsException,
			ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		return addObject(newObject.asPrismObject(), options);
	}

	@Override
	public <T extends ObjectType> String addObject(T newObject)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		return addObject(newObject.asPrismObject(), null);
	}

	@Override
	public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta,
			ModelExecuteOptions options) throws ObjectAlreadyExistsException,
			ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
		modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
		modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid,
			ModelExecuteOptions options) throws ObjectAlreadyExistsException,
			ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		ObjectDelta<T> deleteDelta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
		modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<T> deleteDelta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
		modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <F extends FocusType> void recompute(Class<F> type, String oid) throws SchemaException, PolicyViolationException,
			ExpressionEvaluationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, CommunicationException,
			ConfigurationException, SecurityViolationException {
		modelService.recompute(type, oid, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public PrismObject<UserType> findShadowOwner(String accountOid) throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException {
		return modelService.findShadowOwner(accountOid, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> List<T> searchObjects(
			Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options)
			throws SchemaException, ObjectNotFoundException,
			SecurityViolationException, CommunicationException,
			ConfigurationException {
		return MiscSchemaUtil.toObjectableList(
				modelService.searchObjects(type, query, options, getCurrentTask(), getCurrentResult()));
	}

	@Override
	public <T extends ObjectType> List<T> searchObjects(
			Class<T> type, ObjectQuery query) throws SchemaException,
			ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException {
		return MiscSchemaUtil.toObjectableList(
				modelService.searchObjects(type, query, null, getCurrentTask(), getCurrentResult()));
	}

	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
			ObjectQuery query, ResultHandler<T> handler,
			Collection<SelectorOptions<GetOperationOptions>> options)
			throws SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException,
			SecurityViolationException {
		modelService.searchObjectsIterative(type, query, handler, options, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
			ObjectQuery query, ResultHandler<T> handler)
			throws SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException,
			SecurityViolationException {
		modelService.searchObjectsIterative(type, query, handler, null, getCurrentTask(), getCurrentResult());
	}
	
	@Override
	public <T extends ObjectType> T searchObjectByName(Class<T> type, String name) 
				throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException {
		ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
		List<PrismObject<T>> foundObjects = modelService.searchObjects(type, nameQuery, null, getCurrentTask(), getCurrentResult());
		if (foundObjects.isEmpty()) {
			return null;
		}
		if (foundObjects.size() > 1) {
			throw new IllegalStateException("More than one object found for type "+type+" and name '"+name+"'");
		}
		return foundObjects.iterator().next().asObjectable();
	}
	
	@Override
	public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyString name) 
				throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException {
		ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
		List<PrismObject<T>> foundObjects = modelService.searchObjects(type, nameQuery, null, getCurrentTask(), getCurrentResult());
		if (foundObjects.isEmpty()) {
			return null;
		}
		if (foundObjects.size() > 1) {
			throw new IllegalStateException("More than one object found for type "+type+" and name '"+name+"'");
		}
		return foundObjects.iterator().next().asObjectable();
	}
	
	@Override
	public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyStringType name) 
				throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException {
		ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
		List<PrismObject<T>> foundObjects = modelService.searchObjects(type, nameQuery, null, getCurrentTask(), getCurrentResult());
		if (foundObjects.isEmpty()) {
			return null;
		}
		if (foundObjects.size() > 1) {
			throw new IllegalStateException("More than one object found for type "+type+" and name '"+name+"'");
		}
		return foundObjects.iterator().next().asObjectable();
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type,
			ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options)
			throws SchemaException, ObjectNotFoundException,
			SecurityViolationException, ConfigurationException,
			CommunicationException {
		return modelService.countObjects(type, query, options, getCurrentTask(), getCurrentResult());
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type,
			ObjectQuery query) throws SchemaException, ObjectNotFoundException,
			SecurityViolationException, ConfigurationException,
			CommunicationException {
		return modelService.countObjects(type, query, null, getCurrentTask(), getCurrentResult());
	}

	@Override
	public OperationResult testResource(String resourceOid)
			throws ObjectNotFoundException {
		return modelService.testResource(resourceOid, getCurrentTask());
	}

    @Override
    public ObjectDeltaType getResourceDelta(ModelContext context, String resourceOid) throws SchemaException {
        List<ObjectDelta<ShadowType>> deltas = new ArrayList<>();
        for (Object modelProjectionContextObject : context.getProjectionContexts()) {
            LensProjectionContext lensProjectionContext = (LensProjectionContext) modelProjectionContextObject;
            if (lensProjectionContext.getResourceShadowDiscriminator() != null &&
                    resourceOid.equals(lensProjectionContext.getResourceShadowDiscriminator().getResourceOid())) {
                deltas.add(lensProjectionContext.getDelta());   // union of primary and secondary deltas
            }
        }
        ObjectDelta<ShadowType> sum = ObjectDelta.summarize(deltas);
        return DeltaConvertor.toObjectDeltaType(sum);
    }
    
    public long getSequenceCounter(String sequenceOid) throws ObjectNotFoundException, SchemaException {
    	return SequentialValueExpressionEvaluator.getSequenceCounter(sequenceOid, repositoryService, getCurrentResult());
    }
    
    // orgstruct related methods

    @Override
    public Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return orgStructFunctions.getManagersOids(user, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user, QName relation) {
        return orgStructFunctions.getOrgUnits(user, relation, false);
    }

    @Override
    public OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgByOrgType(object, orgType, false);
    }

    @Override
    public OrgType getOrgByOid(String oid) throws SchemaException {
        return orgStructFunctions.getOrgByOid(oid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user) {
        return orgStructFunctions.getOrgUnits(user, false);
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOfOrg(orgOid, false);
    }

    @Override
    public boolean isManagerOfOrgType(UserType user, String orgType) throws SchemaException {
        return orgStructFunctions.isManagerOfOrgType(user, orgType, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, false);
    }

    @Override
    public Collection<UserType> getManagersByOrgType(UserType user, String orgType) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return orgStructFunctions.getManagersByOrgType(user, orgType, false);
    }

    @Override
    public boolean isManagerOf(UserType user, String orgOid) {
        return orgStructFunctions.isManagerOf(user, orgOid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, orgType, allowSelf, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return orgStructFunctions.getManagersOidsExceptUser(user, false);
    }

	@Override
	public Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException {
		return orgStructFunctions.getManagersOidsExceptUser(userRefList, false);
	}

	@Override
    public OrgType getOrgByName(String name) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getOrgByName(name, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public boolean isManager(UserType user) {
        return orgStructFunctions.isManager(user);
    }

	@Override
	public Protector getProtector() {
		return protector;
	}

	@Override
	public String getPlaintext(ProtectedStringType protectedStringType) throws EncryptionException {
		    if (protectedStringType != null) {
	            return protector.decryptString(protectedStringType);
	        } else {
	            return null;
	        }
	}

	@Override
	public Map<String, String> parseXmlToMap(String xml) {
		Map<String, String> resultingMap = new HashMap<String, String>();
		if(xml!=null&&!xml.isEmpty()){
		XMLInputFactory factory = XMLInputFactory.newInstance();
		String value = "";
		String startName = "";
		InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		boolean isRootElement = true;
		try {
			XMLEventReader eventReader = factory.createXMLEventReader(stream);
			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();
				Integer code = event.getEventType();
				if (code == XMLStreamConstants.START_ELEMENT) {

					StartElement startElement = event.asStartElement();
					startName = startElement.getName().getLocalPart();
					if (!isRootElement) {
						resultingMap.put(startName, null);
					} else {
						isRootElement = false;
					}
				} else if (code == XMLStreamConstants.CHARACTERS) {
					Characters characters = event.asCharacters();
					if (!characters.isWhiteSpace()) {

						StringBuilder valueBuilder;
						if (value != null) {
							valueBuilder = new StringBuilder(value).append(" ").append(characters.getData().toString());
						} else {
							valueBuilder = new StringBuilder(characters.getData().toString());
						}
						value = valueBuilder.toString();
					}
				} else if (code == XMLStreamConstants.END_ELEMENT) {

					EndElement endElement = event.asEndElement();
					String endName = endElement.getName().getLocalPart();

					if (endName.equals(startName)) {
						if (value != null) {
							resultingMap.put(endName, value);
							value = null;
						}
					} else {
						LOGGER.trace("No value between xml tags, tag name : {0}", endName);
					}

				} else if (code == XMLStreamConstants.END_DOCUMENT) {
					isRootElement = true;
				}
			}
		} catch (XMLStreamException e) {

			StringBuilder error = new StringBuilder("Xml stream exception wile parsing xml string")
					.append(e.getLocalizedMessage());
			throw new SystemException(error.toString());
		}
		}else {
			LOGGER.trace("Input xml string null or empty.");
		}
		return resultingMap;
	}

	@Override
	public List<ObjectReferenceType> getMembersAsReferences(String orgOid) throws SchemaException, SecurityViolationException,
			CommunicationException, ConfigurationException, ObjectNotFoundException {
		return getMembers(orgOid).stream()
				.map(obj -> ObjectTypeUtil.createObjectRef(obj))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserType> getMembers(String orgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException {
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.isDirectChildOf(orgOid)
				.build();
		return searchObjects(UserType.class, query, null);
	}
	
	@Override
	public <F extends FocusType> String computeProjectionLifecycle(F focus, ShadowType shadow, ResourceType resource) {
		if (focus == null || shadow == null) {
			return null;
		}
		if (!(focus instanceof UserType)) {
			return null;
		}
		if (shadow.getKind() != null && shadow.getKind() != ShadowKindType.ACCOUNT) {
			return null;
		}
		ProtectedStringType passwordPs = FocusTypeUtil.getPasswordValue((UserType)focus);
		if (passwordPs != null && passwordPs.canGetCleartext()) {
			return null;
		}
		CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
		if (credentialsCapabilityType == null) {
			return null;
		}
		PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
		if (passwordCapabilityType == null) {
			return null;
		}
		if (passwordCapabilityType.isEnabled() == Boolean.FALSE) {
			return null;
		}
		return SchemaConstants.LIFECYCLE_PROPOSED;
	}
	
	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return securityEnforcer.getPrincipal();
	}
}

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
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.DefaultMatchingRule;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.SynchronizationIntent;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import java.util.*;

import javax.xml.namespace.QName;

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

    @Autowired(required = true)
    private transient Protector protector;

    public String hello(String name) {
        return "Hello "+name;
    }

    @Override
    public List<String> toList(String... s) {
        return Arrays.asList(s);
    }

    /**
     * Returns a list of user's managers. Formally, for each Org O which this user has (any) relation to,
     * all managers of O are added to the result.
     *
     * Some customizations are probably necessary here, e.g. filter out project managers (keep only line managers),
     * or defining who is a manager of a user who is itself a manager in its org.unit. (A parent org unit manager,
     * perhaps.)
     *
     * @param user
     * @return list of oids of the respective managers
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    @Override
    public Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            retval.add(u.getOid());
        }
        return retval;
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            if (!u.getOid().equals(user.getOid())) {
                retval.add(u.getOid());
            }
        }
        return retval;
    }

    @Override
    public Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<UserType> retval = new HashSet<UserType>();
        Collection<String> orgOids = getOrgUnits(user);
        for (String orgOid : orgOids) {
            retval.addAll(getManagersOfOrg(orgOid));
        }
        return retval;
    }

    @Override
    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, null, new OperationResult("getUserByOid")).asObjectable();
    }

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    @Override
    public Collection<String> getOrgUnits(UserType user) {
        Set<String> retval = new HashSet<String>();
        if (user == null){
        	return retval;
        }
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            retval.add(orgRef.getOid());
        }
        return retval;
    }

    @Override
    public OrgType getOrgByOid(String oid) throws SchemaException {
    	try {
    		return repositoryService.getObject(OrgType.class, oid, null, new OperationResult("getOrgByOid")).asObjectable();
    	} catch (ObjectNotFoundException e) {
    		return null;
    	}
    }

    @Override
    public OrgType getOrgByName(String name) throws SchemaException {
        PolyString polyName = new PolyString(name);
        ObjectQuery q = ObjectQueryUtil.createNameQuery(polyName, prismContext);
        List<PrismObject<OrgType>> result = repositoryService.searchObjects(OrgType.class, q, null, new OperationResult("getOrgByName"));
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            throw new IllegalStateException("More than one organizational unit with the name '" + name + "' (there are " + result.size() + " of them)");
        }
        return result.get(0).asObjectable();
    }

    @Override
	public OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
    	List<ObjectReferenceType> parentOrgRefs = object.getParentOrgRef();
    	for (ObjectReferenceType parentOrgRef: parentOrgRefs) {
    		OrgType parentOrg;
			try {
				parentOrg = getObject(OrgType.class, parentOrgRef.getOid());
			} catch (ObjectNotFoundException e) {
				return null;
			}
    		if (orgType == null || parentOrg.getOrgType().contains(orgType)) {
    			return parentOrg;
    		}
    	}
    	return null;
	}

	@Override
    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException {
        Set<UserType> retval = new HashSet<UserType>();
        OperationResult result = new OperationResult("getManagerOfOrg");
        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(orgOid, OrgFilter.Scope.ONE_LEVEL));
        List<PrismObject<ObjectType>> members = repositoryService.searchObjects(ObjectType.class, objectQuery, null, result);
        for (PrismObject<ObjectType> member : members) {
            if (member.asObjectable() instanceof UserType) {
                UserType user = (UserType) member.asObjectable();
                if (isManagerOf(user, orgOid)) {
                    retval.add(user);
                }
            }
        }
        return retval;
    }

    @Override
    public boolean isManagerOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid()) && SchemaConstants.ORG_MANAGER.equals(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
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
    	ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, ShadowKindType.ACCOUNT, null);
		LensProjectionContext projectionContext = ctx.findProjectionContext(rat);
		if (projectionContext == null) {
			return false;
		}
		if (projectionContext.isThombstone()) {
			return false;
		}
		
		SynchronizationPolicyDecision synchronizationPolicyDecision = projectionContext.getSynchronizationPolicyDecision();
		SynchronizationIntent synchronizationIntent = projectionContext.getSynchronizationIntent();
		ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
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
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		if (focus == null) {
			return null;
		}
		List<ObjectReferenceType> linkRefs = focus.getLinkRef();
		for (ObjectReferenceType linkRef: linkRefs) {
			ShadowType shadowType;
			try {
				shadowType = getObject(ShadowType.class, linkRef.getOid());
			} catch (ObjectNotFoundException e) {
				// Shadow is gone in the meantime. MidPoint will resolve that by itself.
				// It is safe to ignore this error in this method.
				LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists");
				continue;
			}
			if (shadowType.getResourceRef().getOid().equals(resourceOid)) {
				return shadowType;
			}
		}
		return null;
    }
    
    @Override
	public ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		List<ObjectReferenceType> linkRefs = focus.getLinkRef();
		for (ObjectReferenceType linkRef: linkRefs) {
			ShadowType shadowType;
			try {
				shadowType = getObject(ShadowType.class, linkRef.getOid());
			} catch (ObjectNotFoundException e) {
				// Shadow is gone in the meantime. MidPoint will resolve that by itself.
				// It is safe to ignore this error in this method.
				LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+focus+" because it no longer exists");
				continue;
			}
			if (ShadowUtil.matches(shadowType, resourceOid, kind, intent)) {
				return shadowType;
			}
		}
		return null;
	}

	public <T> Integer countAccounts(String resourceOid, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	ResourceType resourceType = modelObjectResolver.getObjectSimple(ResourceType.class, resourceOid, null, null, result);
    	return countAccounts(resourceType, attributeName, attributeValue, result);
    }
    
    public <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	return countAccounts(resourceType, attributeName, attributeValue, result);
    }
    
    public <T> Integer countAccounts(ResourceType resourceType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return countAccounts(resourceType, attributeQName, attributeValue, result);
    }
    
    private <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue, OperationResult result)
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
        EqualFilter idFilter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()), attrDef, attributeValue);
        EqualFilter ocFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null,
        		rAccountDef.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resourceType);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return modelObjectResolver.countObjects(ShadowType.class, query, result);
    }

    public <T> boolean isUniquePropertyValue(ObjectType objectType, String propertyPathString, T propertyValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".isUniquePropertyValue");
        ItemPath propertyPath = new XPathHolder(propertyPathString).toItemPath();
        return isUniquePropertyValue(objectType, propertyPath, propertyValue, result);
    }

    private <T> boolean isUniquePropertyValue(final ObjectType objectType, ItemPath propertyPath, T propertyValue, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        List<? extends ObjectType> conflictingObjects = getObjectsInConflictOnPropertyValue(objectType, propertyPath, propertyValue, DefaultMatchingRule.NAME, false, result);
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
        return getObjectsInConflictOnPropertyValue(objectType, propertyPath, propertyValue, matchingRuleQName, getAllConflicting, result);
    }

    private <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(final O objectType, ItemPath propertyPath, T propertyValue, QName matchingRule, final boolean getAllConflicting, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        Validate.notNull(objectType, "Null object");
        Validate.notNull(propertyPath, "Null property path");
        Validate.notNull(propertyValue, "Null property value");
        PrismPropertyDefinition<?> propertyDefinition = objectType.asPrismObject().getDefinition().findPropertyDefinition(propertyPath);
        EqualFilter filter = EqualFilter.createEqual(propertyPath, propertyDefinition, matchingRule, propertyValue);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
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

        modelObjectResolver.searchIterative((Class) objectType.getClass(), query, null, handler, result);

        return conflictingObjects;
    }

    public <T> boolean isUniqueAccountValue(ResourceType resourceType, ShadowType shadowType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	Validate.notEmpty(attributeName,"Empty attribute name");
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".isUniqueAccountValue");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return isUniqueAccountValue(resourceType, shadowType, attributeQName, attributeValue, result);
    }

    private <T> boolean isUniqueAccountValue(ResourceType resourceType, final ShadowType shadowType,
    		QName attributeName, T attributeValue, OperationResult result) 
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	Validate.notNull(resourceType, "Null resource");
    	Validate.notNull(shadowType, "Null shadow");
    	Validate.notNull(attributeName, "Null attribute name");
    	Validate.notNull(attributeValue, "Null attribute value");
    	RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
        EqualFilter idFilter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()), attrDef, attributeValue);
        EqualFilter ocFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, 
        		null, rAccountDef.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resourceType);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
        LOGGER.trace("Determining uniqueness of attribute {} using query:\n{}", attributeName, query.debugDump());
        
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
        
		modelObjectResolver.searchIterative(ShadowType.class, query, null, handler, result);
		
		return isUniqueHolder.getValue();
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
        return lensContext.toPrismContainer().getValue().asContainerable();
    }
    
    // Convenience functions
    
	@Override
	public <T extends ObjectType> T createEmptyObject(Class<T> type) {
		PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismObject<T> object = objectDefinition.instantiate();
		return object.asObjectable();
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, String name) {
		T objectType = createEmptyObject(type);
		objectType.setName(new PolyStringType(name));
		return objectType;
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyString name) {
		T objectType = createEmptyObject(type);
		objectType.setName(new PolyStringType(name));
		return objectType;
	}

	@Override
	public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyStringType name) {
		T objectType = createEmptyObject(type);
		objectType.setName(name);
		return objectType;
	}
    
    // Functions accessing modelService
    
	@Override
	public <T extends ObjectType> T getObject(Class<T> type,
			String oid, Collection<SelectorOptions<GetOperationOptions>> options)
			throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException {
		return modelService.getObject(type, oid, options, getCurrentTask(), getCurrentResult()).asObjectable();
	}

	@Override
	public <T extends ObjectType> T getObject(Class<T> type,
			String oid) throws ObjectNotFoundException, SchemaException,
			SecurityViolationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
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
		modelService.recompute(type, oid, getCurrentTask(), getCurrentResult());
	}

	@Override
	public PrismObject<UserType> findShadowOwner(String accountOid) throws ObjectNotFoundException, SecurityViolationException, SchemaException {
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
}

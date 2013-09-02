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
package com.evolveum.midpoint.model.expr;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;

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
    private RepositoryService repositoryService;

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
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            retval.add(orgRef.getOid());
        }
        return retval;
    }

    @Override
    public OrgType getOrgByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(OrgType.class, oid, null, new OperationResult("getOrgByOid")).asObjectable();
    }

    @Override
    public OrgType getOrgByName(String name) throws ObjectNotFoundException, SchemaException {
        PolyString polyName = new PolyString(name);
        polyName.recompute(prismContext.getDefaultPolyStringNormalizer());
        ObjectQuery q = QueryUtil.createNameQuery(polyName, prismContext);
        List<PrismObject<OrgType>> result = repositoryService.searchObjects(OrgType.class, q, null, new OperationResult("getOrgByName"));
        if (result.isEmpty()) {
            throw new ObjectNotFoundException("No organizational unit with the name '" + name + "'", name);
        }
        if (result.size() > 1) {
            throw new IllegalStateException("More than one organizational unit with the name '" + name + "' (there are " + result.size() + " of them)");
        }
        return result.get(0).asObjectable();
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException {
        Set<UserType> retval = new HashSet<UserType>();
        OperationResult result = new OperationResult("getManagerOfOrg");
        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(orgOid, null, 1));
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
    
    public boolean hasLinkedAccount(String resourceOid) {
    	LensContext<ObjectType, ShadowType> ctx = ModelExpressionThreadLocalHolder.getLensContext();
    	if (ctx == null) {
    		throw new IllegalStateException("No lens context");
    	}
    	LensFocusContext<ObjectType> focusContext = ctx.getFocusContext();
    	if (focusContext == null) {
    		throw new IllegalStateException("No focus in lens context");
    	}
    	ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, null);
		LensProjectionContext<ShadowType> projectionContext = ctx.findProjectionContext(rat);
		if (projectionContext == null) {
			return false;
		}
		if (projectionContext.isThombstone()) {
			return false;
		}
		
		SynchronizationPolicyDecision synchronizationPolicyDecision = projectionContext.getSynchronizationPolicyDecision();
		SynchronizationIntent synchronizationIntent = projectionContext.getSynchronizationIntent();
		ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
		if (scriptContext.isEvaluateNew()) {
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
    
    public <O extends ObjectType> O getObject(Class<O> type, String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
    	return modelObjectResolver.getObject(type, oid, null, null, getCurrentResult(MidpointFunctions.class.getName()+".getObject"));
    }
    
    public <T> int countAccounts(String resourceOid, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	ResourceType resourceType = modelObjectResolver.getObjectSimple(ResourceType.class, resourceOid, null, null, result);
    	return countAccounts(resourceType, attributeName, attributeValue, result);
    }
    
    public <T> int countAccounts(ResourceType resourceType, QName attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	return countAccounts(resourceType, attributeName, attributeValue, result);
    }
    
    public <T> int countAccounts(ResourceType resourceType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return countAccounts(resourceType, attributeQName, attributeValue, result);
    }
    
    private <T> int countAccounts(ResourceType resourceType, QName attributeName, T attributeValue, OperationResult result) 
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
        EqualsFilter idFilter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), attrDef, null, attributeValue);
        EqualsFilter ocFilter = EqualsFilter.createEqual(ShadowType.class, prismContext, 
        		ShadowType.F_OBJECT_CLASS, rAccountDef.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.class, 
        		ShadowType.F_RESOURCE_REF, resourceType.asPrismObject());
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return modelObjectResolver.countObjects(ShadowType.class, query, result);
    }
    
    public <T> boolean isUniqueAccountValue(ResourceType resourceType, ShadowType shadowType, String attributeName, T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	OperationResult result = getCurrentResult(MidpointFunctions.class.getName()+".countAccounts");
    	QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
		return isUniqueAccountValue(resourceType, shadowType, attributeQName, attributeValue, result);
    }
    
    private <T> boolean isUniqueAccountValue(ResourceType resourceType, final ShadowType shadowType, 
    		QName attributeName, T attributeValue, OperationResult result) 
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
    		SecurityViolationException {
    	RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
        EqualsFilter idFilter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), attrDef, null, attributeValue);
        EqualsFilter ocFilter = EqualsFilter.createEqual(ShadowType.class, prismContext, 
        		ShadowType.F_OBJECT_CLASS, rAccountDef.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.class, 
        		ShadowType.F_RESOURCE_REF, resourceType.asPrismObject());
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
        
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
        
		modelObjectResolver.searchIterative(ShadowType.class, query, handler, result);
		
		return isUniqueHolder.getValue();
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
}

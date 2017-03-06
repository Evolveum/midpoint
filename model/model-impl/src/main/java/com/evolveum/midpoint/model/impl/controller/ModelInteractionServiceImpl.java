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
package com.evolveum.midpoint.model.impl.controller;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyGenerator;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
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
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
@Component("modelInteractionService")
public class ModelInteractionServiceImpl implements ModelInteractionService {
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelInteractionServiceImpl.class);

	@Autowired(required = true)
	private ContextFactory contextFactory;

	@Autowired(required = true)
	private Projector projector;
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
	
	@Autowired(required = true)
	private SchemaTransformer schemaTransformer;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;
	
	@Autowired(required = true)
	private ModelObjectResolver objectResolver;
	
	@Autowired(required = true)
	private ObjectMerger objectMerger;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;
	
	@Autowired(required = true)
	private ValuePolicyGenerator valuePolicyGenerator;
	
	@Autowired(required = true)
	private Protector protector;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired
	private Visualizer visualizer;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelInteractionService#previewChanges(com.evolveum.midpoint.prism.delta.ObjectDelta, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends ObjectType> ModelContext<F> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		return previewChanges(deltas, options, task, Collections.<ProgressListener>emptyList(), parentResult);
	}

	@Override
	public <F extends ObjectType> ModelContext<F> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task,
			Collection<ProgressListener> listeners, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes input:\n{}", DebugUtil.debugDump(deltas));
		}
		int size = 0;
		if (deltas != null) {
			size = deltas.size();
		}
		Collection<ObjectDelta<? extends ObjectType>> clonedDeltas = new ArrayList<ObjectDelta<? extends ObjectType>>(size);
		if (deltas != null) {
			for (ObjectDelta delta : deltas){
				clonedDeltas.add(delta.clone());
			}
		}
		
		OperationResult result = parentResult.createSubresult(PREVIEW_CHANGES);
		LensContext<F> context = null;
		
		try {
			
			//used cloned deltas instead of origin deltas, because some of the values should be lost later..
			context = contextFactory.createContext(clonedDeltas, options, task, result);
//			context.setOptions(options);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.trace("Preview changes context:\n{}", context.debugDump());
			}
			context.setProgressListeners(listeners);
			
			projector.projectAllWaves(context, "preview", task, result);
			context.distributeResource();
			
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ExpressionEvaluationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (PolicyViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes output:\n{}", context.debugDump());
		}
		
		result.computeStatus();
		result.cleanupResult();

		return context;
	}

    @Override
    public <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        return LensContext.fromLensContextType(wrappedContext, prismContext, provisioning, result);
    }
    
	@Override
	public <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object, AuthorizationPhaseType phase, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
		OperationResult result = parentResult.createMinorSubresult(GET_EDIT_OBJECT_DEFINITION);
		PrismObjectDefinition<O> objectDefinition = object.getDefinition().deepClone(true);
		
		PrismObject<O> baseObject = object;
		if (object.getOid() != null) {
			// Re-read the object from the repository to make sure we have all the properties.
			// the object from method parameters may be already processed by the security code
			// and properties needed to evaluate authorizations may not be there
			// MID-3126, see also MID-3435
			baseObject = cacheRepositoryService.getObject(object.getCompileTimeClass(), object.getOid(), null, result);
		}
		
		// TODO: maybe we need to expose owner resolver in the interface?
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(baseObject, null);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security constrains for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
		}
		if (securityConstraints == null) {
			// Nothing allowed => everything denied
			result.setStatus(OperationResultStatus.NOT_APPLICABLE);
			return null;
		}
		
		ObjectTemplateType objectTemplateType;
		try {
			objectTemplateType = schemaTransformer.determineObjectTemplate(object, phase, result);
		} catch (ConfigurationException | ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		}
		schemaTransformer.applyObjectTemplateToDefinition(objectDefinition, objectTemplateType, result);
		
		schemaTransformer.applySecurityConstraints(objectDefinition, securityConstraints, phase);
		
		if (object.canRepresent(ShadowType.class)) {
			PrismObject<ShadowType> shadow = (PrismObject<ShadowType>)object;
			String resourceOid = ShadowUtil.getResourceOid(shadow);
			if (resourceOid != null) {
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
				PrismObject<ResourceType> resource;
				try {
					resource = provisioning.getObject(ResourceType.class, resourceOid, options, null, result);			// TODO include task here
				} catch (CommunicationException | SecurityViolationException e) {
					throw new ConfigurationException(e.getMessage(), e);
				}
				RefinedObjectClassDefinition refinedObjectClassDefinition = getEditObjectClassDefinition(shadow, resource, phase);
				if (refinedObjectClassDefinition != null) {
					((ComplexTypeDefinitionImpl) objectDefinition.getComplexTypeDefinition()).replaceDefinition(ShadowType.F_ATTRIBUTES,
						refinedObjectClassDefinition.toResourceAttributeContainerDefinition());
				}
			}
		}
		
		result.computeStatus();
		return objectDefinition;
	}
	
	@Override
	public PrismObjectDefinition<ShadowType> getEditShadowDefinition(ResourceShadowDiscriminator discr, AuthorizationPhaseType phase, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
		// HACK hack hack
		// Make a dummy shadow instance here and evaluate the schema for that. It is not 100% correct. But good enough for now.
		// TODO: refactor when we add better support for multi-tenancy
		
		PrismObject<ShadowType> shadow = prismContext.createObject(ShadowType.class);
		ShadowType shadowType = shadow.asObjectable();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		if (discr != null) {
			resourceRef.setOid(discr.getResourceOid());
			shadowType.setResourceRef(resourceRef);
			shadowType.setKind(discr.getKind());
			shadowType.setIntent(discr.getIntent());
			shadowType.setObjectClass(discr.getObjectClass());
		}
		
		return getEditObjectDefinition(shadow, phase, parentResult); 
	}

    @Override
	public RefinedObjectClassDefinition getEditObjectClassDefinition(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, AuthorizationPhaseType phase)
			throws SchemaException {
    	Validate.notNull(resource, "Resource must not be null");
    	
    	RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
    	CompositeRefinedObjectClassDefinition rocd = refinedSchema.determineCompositeObjectClassDefinition(shadow);
    	if (rocd == null) {
    		LOGGER.debug("No object class definition for shadow {}, returning null");
    		return null;
    	}
        LayerRefinedObjectClassDefinition layeredROCD = rocd.forLayer(LayerType.PRESENTATION);
    	
    	// TODO: maybe we need to expose owner resolver in the interface?
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(shadow, null);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security constrains for {}:\n{}", shadow, securityConstraints==null?"null":securityConstraints.debugDump());
		}
		if (securityConstraints == null) {
			return null;
		}

    	ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
		AuthorizationDecisionType attributesReadDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.READ.getUrl(), 
    			securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), phase), phase);
		AuthorizationDecisionType attributesAddDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.ADD.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.ADD.getUrl(), phase), phase);
		AuthorizationDecisionType attributesModifyDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.MODIFY.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase), phase);
		LOGGER.trace("Attributes container access read:{}, add:{}, modify:{}", new Object[]{attributesReadDecision, attributesAddDecision, attributesModifyDecision});

        /*
         *  We are going to modify attribute definitions list.
         *  So let's make a (shallow) clone here, although it is probably not strictly necessary.
         */
        layeredROCD = layeredROCD.clone();
        for (LayerRefinedAttributeDefinition rAttrDef: layeredROCD.getAttributeDefinitions()) {
			ItemPath attributePath = new ItemPath(ShadowType.F_ATTRIBUTES, rAttrDef.getName());
			AuthorizationDecisionType attributeReadDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.READ.getUrl(), attributesReadDecision, phase);
			AuthorizationDecisionType attributeAddDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.ADD.getUrl(), attributesAddDecision, phase);
			AuthorizationDecisionType attributeModifyDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.MODIFY.getUrl(), attributesModifyDecision, phase);
			LOGGER.trace("Attribute {} access read:{}, add:{}, modify:{}", new Object[]{rAttrDef.getName(), attributeReadDecision, attributeAddDecision, attributeModifyDecision});
			if (attributeReadDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanRead(false);
			}
			if (attributeAddDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanAdd(false);
			}
			if (attributeModifyDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanModify(false);
			}
		}

        // TODO what about activation and credentials?
    	
    	return layeredROCD;
	}


	@Override
	public Collection<? extends DisplayableValue<String>> getActionUrls() {
		return Arrays.asList(ModelAuthorizationAction.values());
	}

	@Override
	public <F extends FocusType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<F> focus, OperationResult parentResult) 
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		OperationResult result = parentResult.createMinorSubresult(GET_ASSIGNABLE_ROLE_SPECIFICATION);
		
		RoleSelectionSpecification spec = new RoleSelectionSpecification();
		
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(focus, null);
		if (securityConstraints == null) {
			return null;
		}
		AuthorizationDecisionType decision = securityConstraints.findItemDecision(new ItemPath(FocusType.F_ASSIGNMENT), 
				ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
		if (decision == AuthorizationDecisionType.ALLOW) {
			 getAllRoleTypesSpec(spec, result);
			result.recordSuccess();
			return spec;
		}
		if (decision == AuthorizationDecisionType.DENY) {
			result.recordSuccess();
			spec.setNoRoleTypes();
			spec.setFilter(NoneFilter.createNone());
			return spec;
		}
		decision = securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
		if (decision == AuthorizationDecisionType.ALLOW) {
			getAllRoleTypesSpec(spec, result);
			result.recordSuccess();
			return spec;
		}
		if (decision == AuthorizationDecisionType.DENY) {
			result.recordSuccess();
			spec.setNoRoleTypes();
			spec.setFilter(NoneFilter.createNone());
			return spec;
		}
		
		try {
			ObjectFilter filter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.ASSIGN.getUrl(), 
					AuthorizationPhaseType.REQUEST, RoleType.class, focus, AllFilter.createAll());
			LOGGER.trace("assignableRoleSpec filter: {}", filter);
			spec.setFilter(filter);
			if (filter instanceof NoneFilter) {
				result.recordSuccess();
				spec.setNoRoleTypes();
				return spec;
			} else if (filter == null || filter instanceof AllFilter) {
				getAllRoleTypesSpec(spec, result);
				result.recordSuccess();
				return spec;
			} else if (filter instanceof OrFilter) {
				Collection<RoleSelectionSpecEntry> allRoleTypeDvals = new ArrayList<>();
				for (ObjectFilter subfilter: ((OrFilter)filter).getConditions()) {
					Collection<RoleSelectionSpecEntry> roleTypeDvals =  getRoleSelectionSpecEntries(subfilter);
					if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
						// This branch of the OR clause does not have any constraint for roleType
						// therefore all role types are possible (regardless of other branches, this is OR)
						spec = new RoleSelectionSpecification();
						spec.setFilter(filter);
						getAllRoleTypesSpec(spec, result);
						result.recordSuccess();
						return spec;
					} else {
						allRoleTypeDvals.addAll(roleTypeDvals);
					}
				}
				addRoleTypeSpecEntries(spec, allRoleTypeDvals, result);
			} else {
				Collection<RoleSelectionSpecEntry> roleTypeDvals = getRoleSelectionSpecEntries(filter);
				if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
					getAllRoleTypesSpec(spec, result);
					result.recordSuccess();
					return spec;					
				} else {
					addRoleTypeSpecEntries(spec, roleTypeDvals, result);
				}
			}
			result.recordSuccess();
			return spec;
		} catch (SchemaException | ConfigurationException | ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		}
	}
	
	private void addRoleTypeSpecEntries(RoleSelectionSpecification spec, 
			Collection<RoleSelectionSpecEntry> roleTypeDvals, OperationResult result) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
			getAllRoleTypesSpec(spec, result);
		} else {
			if (RoleSelectionSpecEntry.hasNegative(roleTypeDvals)) {
				Collection<RoleSelectionSpecEntry> positiveList = RoleSelectionSpecEntry.getPositive(roleTypeDvals);
				if (positiveList == null || positiveList.isEmpty()) {
					positiveList = getRoleSpecEntriesForAllRoles(result);
				}
				if (positiveList == null || positiveList.isEmpty()) {
					return;
				}
				for (RoleSelectionSpecEntry positiveEntry: positiveList) {
					if (!RoleSelectionSpecEntry.hasNegativeValue(roleTypeDvals,positiveEntry.getValue())) {
						spec.addRoleType(positiveEntry);
					}
				}
				
			} else {
				spec.addRoleTypes(roleTypeDvals);
			}
		}
	}

	private RoleSelectionSpecification getAllRoleTypesSpec(RoleSelectionSpecification spec, OperationResult result) 
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		Collection<RoleSelectionSpecEntry> allEntries = getRoleSpecEntriesForAllRoles(result);
		if (allEntries == null || allEntries.isEmpty()) {
			return spec;
		}
		spec.addRoleTypes(allEntries);
		return spec;
	}
	
	private Collection<RoleSelectionSpecEntry> getRoleSpecEntriesForAllRoles(OperationResult result) 
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		ObjectTemplateType objectTemplateType = schemaTransformer.determineObjectTemplate(RoleType.class, AuthorizationPhaseType.REQUEST, result);
		if (objectTemplateType == null) {
			return null;
		}
		Collection<RoleSelectionSpecEntry> allEntries = new ArrayList();
		for(ObjectTemplateItemDefinitionType itemDef: objectTemplateType.getItem()) {
			ItemPathType ref = itemDef.getRef();
			if (ref == null) {
				continue;
			}
			ItemPath itemPath = ref.getItemPath();
			QName itemName = ItemPath.getName(itemPath.first());
			if (itemName == null) {
				continue;
			}
			if (QNameUtil.match(RoleType.F_ROLE_TYPE, itemName)) {
				ObjectReferenceType valueEnumerationRef = itemDef.getValueEnumerationRef();
				if (valueEnumerationRef == null || valueEnumerationRef.getOid() == null) {
					return allEntries;
				}
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
		    			GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
				PrismObject<LookupTableType> lookup = cacheRepositoryService.getObject(LookupTableType.class, valueEnumerationRef.getOid(), 
						options, result);
				for (LookupTableRowType row: lookup.asObjectable().getRow()) {
					PolyStringType polyLabel = row.getLabel();
					String key = row.getKey();
					String label = key;
					if (polyLabel != null) {
						label = polyLabel.getOrig();
					}
					RoleSelectionSpecEntry roleTypeDval = new RoleSelectionSpecEntry(key, label, null);
					allEntries.add(roleTypeDval);
				}
				return allEntries;
			}
		}
		return allEntries;
	}

	private Collection<RoleSelectionSpecEntry> getRoleSelectionSpecEntries(ObjectFilter filter) throws SchemaException {
		LOGGER.trace("getRoleSelectionSpec({})", filter);
		if (filter == null || filter instanceof AllFilter) {
			return null;
		} else if (filter instanceof EqualFilter<?>) {
			return createSingleDisplayableValueCollection(getRoleSelectionSpecEq((EqualFilter)filter));
		} else if (filter instanceof AndFilter) {
			for (ObjectFilter subfilter: ((AndFilter)filter).getConditions()) {
				if (subfilter instanceof EqualFilter<?>) {
					RoleSelectionSpecEntry roleTypeDval = getRoleSelectionSpecEq((EqualFilter)subfilter);
					if (roleTypeDval != null) {
						return createSingleDisplayableValueCollection(roleTypeDval);
					}
				}
			}
			return null;
		} else if (filter instanceof OrFilter) {
			Collection<RoleSelectionSpecEntry> col = new ArrayList<>(((OrFilter)filter).getConditions().size());
			for (ObjectFilter subfilter: ((OrFilter)filter).getConditions()) {
				if (subfilter instanceof EqualFilter<?>) {
					RoleSelectionSpecEntry roleTypeDval = getRoleSelectionSpecEq((EqualFilter)subfilter);
					if (roleTypeDval != null) {
						col.add(roleTypeDval);
					}
				}
			}
			return col;
		} else if (filter instanceof TypeFilter) {
			return getRoleSelectionSpecEntries(((TypeFilter)filter).getFilter());
		} else if (filter instanceof NotFilter) {
			Collection<RoleSelectionSpecEntry> subRoleSelectionSpec = getRoleSelectionSpecEntries(((NotFilter)filter).getFilter());
			RoleSelectionSpecEntry.negate(subRoleSelectionSpec);
			return subRoleSelectionSpec;
		} else if (filter instanceof RefFilter) {
			return null;
		} else {
			throw new UnsupportedOperationException("Unexpected filter "+filter);
		}
	}
	
	private Collection<RoleSelectionSpecEntry> createSingleDisplayableValueCollection(
			RoleSelectionSpecEntry dval) {
		if (dval == null) {
			return null;
		}
		Collection<RoleSelectionSpecEntry> col = new ArrayList<>(1);
		col.add(dval);
		return col;
	}

	private RoleSelectionSpecEntry getRoleSelectionSpecEq(EqualFilter<String> eqFilter) throws SchemaException {
		if (QNameUtil.match(RoleType.F_ROLE_TYPE,eqFilter.getElementName())) {
			List<PrismPropertyValue<String>> ppvs = eqFilter.getValues();
			if (ppvs.size() > 1) {
				throw new SchemaException("More than one value in roleType search filter");
			}
			String roleType = ppvs.get(0).getValue();
			RoleSelectionSpecEntry roleTypeDval = new RoleSelectionSpecEntry(roleType, roleType, null);
			return roleTypeDval;
		}
		return null;
	}

	@Override
	public AuthenticationsPolicyType getAuthenticationPolicy(PrismObject<UserType> user, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)

				OperationResult result = parentResult.createMinorSubresult(GET_AUTHENTICATIONS_POLICY);
					return resolvePolicyTypeFromSecurityPolicy(AuthenticationsPolicyType.class, SecurityPolicyType.F_AUTHENTICATION, user, task, result);

	}

	@Override
	public RegistrationsPolicyType getRegistrationPolicy(PrismObject<UserType> user, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)

					OperationResult result = parentResult.createMinorSubresult(GET_REGISTRATIONS_POLICY);
					return resolvePolicyTypeFromSecurityPolicy(RegistrationsPolicyType.class, SecurityPolicyType.F_REGISTRATION, user, task, result);
	}

	@Override
	public CredentialsPolicyType getCredentialsPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)
		
			OperationResult result = parentResult.createMinorSubresult(GET_CREDENTIALS_POLICY);
			return resolvePolicyTypeFromSecurityPolicy(CredentialsPolicyType.class, SecurityPolicyType.F_CREDENTIALS, user, task, result);


	}

	private <C extends Containerable> C  resolvePolicyTypeFromSecurityPolicy(Class<C> type, QName path, PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		SecurityPolicyType securityPolicyType = getSecurityPolicy(user, task, parentResult);
		if (securityPolicyType == null) {
			return null;
		}
		PrismContainer<C> container = securityPolicyType.asPrismObject().findContainer(path);
		if (container == null) {
			return null;
		}
		PrismContainerValue<C> containerValue = container.getValue();
		parentResult.recordSuccess();
		return containerValue.asContainerable();


	}

	@Override
	public SecurityPolicyType getSecurityPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(GET_SECURITY_POLICY);
		try {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		if (systemConfiguration == null) {
			result.recordNotApplicableIfUnknown();
			return null;
		}
		ObjectReferenceType secPolicyRef = systemConfiguration.asObjectable().getGlobalSecurityPolicyRef();
		if (secPolicyRef == null) {
			result.recordNotApplicableIfUnknown();
			return null;
		}

		 SecurityPolicyType securityPolicyType = objectResolver.resolve(secPolicyRef, SecurityPolicyType.class, null, "security policy referred from system configuration", task, result);
		if (securityPolicyType == null) {
			result.recordNotApplicableIfUnknown();
			return null;
		}
		return securityPolicyType;
		}catch (ObjectNotFoundException | SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@Override
	public AdminGuiConfigurationType getAdminGuiConfiguration(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		MidPointPrincipal principal = null;
		try {
			principal = securityEnforcer.getPrincipal();
		} catch (SecurityViolationException e) {
			LOGGER.warn("Security violation while getting principlal to get GUI config: {}", e.getMessage(), e);
		}
		
		if (principal == null) {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
			if (systemConfiguration == null) {
				return null;
			}
			return systemConfiguration.asObjectable().getAdminGuiConfiguration();
		} else {
			return principal.getAdminGuiConfiguration();
		}
	}
	
	@Override
	public SystemConfigurationType getSystemConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
			if (systemConfiguration == null) {
				return null;
			}
			return systemConfiguration.asObjectable();
	}

	@Override
	public DeploymentInformationType getDeploymentInformationConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
		if (systemConfiguration == null) {
			return null;
		}
			return systemConfiguration.asObjectable().getDeploymentInformation();
	}

	@Override
	public AccessCertificationConfigurationType getCertificationConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
		if (systemConfiguration == null) {
			return null;
		}
		return systemConfiguration.asObjectable().getAccessCertification();
	}

	@Override
	public boolean checkPassword(String userOid, ProtectedStringType password, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(CHECK_PASSWORD);
		UserType userType;
		try {
			 userType = objectResolver.getObjectSimple(UserType.class, userOid, null, task, result);
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		}
		if (userType.getCredentials() == null || userType.getCredentials().getPassword() == null 
				|| userType.getCredentials().getPassword().getValue() == null) {
			return password == null;
		}
		ProtectedStringType currentPassword = userType.getCredentials().getPassword().getValue();
		boolean cmp;
		try {
			cmp = protector.compare(password, currentPassword);
		} catch (EncryptionException e) {
			result.recordFatalError(e);
			throw new SystemException(e.getMessage(),e);
		}
		result.recordSuccess();
		return cmp;
	}

	@Override
	public List<? extends Scene> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) throws SchemaException {
		return visualizer.visualizeDeltas(deltas, task, result);
	}

	@Override
	@NotNull
	public Scene visualizeDelta(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result) throws SchemaException {
		return visualizer.visualizeDelta(delta, task, result);
	}

	@Override
	public ConnectorOperationalStatus getConnectorOperationalStatus(String resourceOid, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		OperationResult result = parentResult.createMinorSubresult(GET_CONNECTOR_OPERATIONAL_STATUS);
		ConnectorOperationalStatus status;
		try {
			status = provisioning.getConnectorOperationalStatus(resourceOid, result);
		} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		}
		result.computeStatus();
		return status;
	}

	@Override
	public <O extends ObjectType> MergeDeltas<O> mergeObjectsPreviewDeltas(Class<O> type, String leftOid,
			String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(MERGE_OBJECTS_PREVIEW_DELTA);
		
		try {
			
			MergeDeltas<O> mergeDeltas = objectMerger.computeMergeDeltas(type, leftOid, rightOid, mergeConfigurationName, task, result);
			
			result.computeStatus();
			return mergeDeltas;
			
		} catch (ObjectNotFoundException | SchemaException | ConfigurationException | ExpressionEvaluationException |
				CommunicationException | SecurityViolationException | RuntimeException | Error e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@Override
	public <O extends ObjectType> PrismObject<O> mergeObjectsPreviewObject(Class<O> type, String leftOid,
			String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(MERGE_OBJECTS_PREVIEW_OBJECT);
		
		try {
			
			MergeDeltas<O> mergeDeltas = objectMerger.computeMergeDeltas(type, leftOid, rightOid, mergeConfigurationName, task, result);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Merge preview {} + {} deltas:\n{}", leftOid, rightOid, mergeDeltas.debugDump(1));
			}
			
			final PrismObject<O> objectLeft = objectResolver.getObjectSimple(type, leftOid, null, task, result).asPrismObject();
			
			if (mergeDeltas == null) {
				result.computeStatus();
				return objectLeft;
			}
			
			mergeDeltas.getLeftObjectDelta().applyTo(objectLeft);
			mergeDeltas.getLeftLinkDelta().applyTo(objectLeft);
			
			result.computeStatus();
			return objectLeft;
			
		} catch (ObjectNotFoundException | SchemaException | ConfigurationException | ExpressionEvaluationException |
				CommunicationException | SecurityViolationException | RuntimeException | Error e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@Override
	public <O extends ObjectType> String generateValue(StringPolicyType policy, int defaultLength, boolean generateMinimalSize,
			PrismObject<O> object, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		return valuePolicyGenerator.generate(policy, defaultLength, generateMinimalSize, object, shortDesc, task, parentResult);
	}

}

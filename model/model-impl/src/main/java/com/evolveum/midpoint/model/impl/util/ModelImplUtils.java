/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author lazyman
 *
 */
public class ModelImplUtils {

	private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName() + ".resolveReference";
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelImplUtils.class);

	public static void validatePaging(ObjectPaging paging) {
		if (paging == null) {
			return;
		}

		if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
			throw new IllegalArgumentException("Paging max size must be more than 0.");
		}
		if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
			throw new IllegalArgumentException("Paging offset index must be more than 0.");
		}
	}

	public static void recordFatalError(OperationResult result, Throwable e) {
		recordFatalError(result, e.getMessage(), e);
	}

	public static void recordFatalError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordFatalError(message, e);
		result.cleanupResult(e);
	}

	public static void recordPartialError(OperationResult result, Throwable e) {
		recordPartialError(result, e.getMessage(), e);
	}

	public static void recordPartialError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordPartialError(message, e);
		result.cleanupResult(e);
	}

	public static <O extends ObjectType> String getOperationUrlFromDelta(ObjectDelta<O> delta) {
		if (delta == null) {
			return null;
		}
		if (delta.isAdd()) {
			return ModelAuthorizationAction.ADD.getUrl();
		}
		if (delta.isModify()) {
			return ModelAuthorizationAction.MODIFY.getUrl();
		}
		if (delta.isDelete()) {
			return ModelAuthorizationAction.DELETE.getUrl();
		}
		throw new IllegalArgumentException("Unknown delta type "+delta);
	}


	// from the most to least appropriate
	@NotNull
	public static <O extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(
			Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType)
			throws ConfigurationException {
		List<ObjectPolicyConfigurationType> rv = new ArrayList<>();
		List<ObjectPolicyConfigurationType> typeNoSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> typeWithSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> noTypeNoSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> noTypeWithSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> all = new ArrayList<>();

		all.addAll(systemConfigurationType.getDefaultObjectPolicyConfiguration());
		all.addAll(systemConfigurationType.getObjectTemplate());        // deprecated
		if (objectClass == UserType.class) {
			// Deprecated method to specify user template. For compatibility only
			ObjectReferenceType templateRef = systemConfigurationType.getDefaultUserTemplateRef();
			if (templateRef != null) {
				all.add(new ObjectPolicyConfigurationType().objectTemplateRef(templateRef.clone()));
			}
		}

		for (ObjectPolicyConfigurationType aPolicyConfigurationType: all) {
			QName typeQName = aPolicyConfigurationType.getType();
			if (typeQName != null) {
				ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
				if (objectType == null) {
					throw new ConfigurationException(
							"Unknown type " + typeQName + " in default object policy definition or object template definition in system configuration");
				}
				if (objectType.getClassDefinition() == objectClass) {
					String aSubType = aPolicyConfigurationType.getSubtype();
					if (aSubType == null) {
						typeNoSubtype.add(aPolicyConfigurationType);
					} else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
						typeWithSubtype.add(aPolicyConfigurationType);
					}
				}
			} else {
				String aSubType = aPolicyConfigurationType.getSubtype();
				if (aSubType == null) {
					noTypeNoSubtype.add(aPolicyConfigurationType);
				} else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
					noTypeWithSubtype.add(aPolicyConfigurationType);
				}
			}
		}
		rv.addAll(typeWithSubtype);
		rv.addAll(typeNoSubtype);
		rv.addAll(noTypeWithSubtype);
		rv.addAll(noTypeNoSubtype);
		return rv;
	}

	@NotNull
	public static <F extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(LensContext<F> context) {
		PrismObject<SystemConfigurationType> config = context.getSystemConfiguration();
		if (config == null) {
			return Collections.emptyList();
		}
		PrismObject<F> object = context.getFocusContext() != null ? context.getFocusContext().getObjectAny() : null;
		List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
		List<ObjectPolicyConfigurationType> relevantPolicies;
		try {
			relevantPolicies = ModelImplUtils.getApplicablePolicies(context.getFocusContext().getObjectTypeClass(), subTypes,
					config.asObjectable());
		} catch (ConfigurationException e) {
			throw new SystemException("Couldn't get relevant object policies", e);
		}
		LOGGER.trace("Relevant policies: {}", relevantPolicies);
		return relevantPolicies;
	}

	public static <F extends ObjectType> ConflictResolutionType getConflictResolution(LensContext<F> context) {
		for (ObjectPolicyConfigurationType p : ModelImplUtils.getApplicablePolicies(context)) {
			if (p.getConflictResolution() != null) {
				return p.getConflictResolution();
			}
		}
		return null;
	}


	@Deprecated	// use RepositoryService.objectSearchIterative instead
	public static <T extends ObjectType> void searchIterative(RepositoryService repositoryService, Class<T> type, ObjectQuery query,
			Handler<PrismObject<T>> handler, int blockSize, OperationResult opResult) throws SchemaException {
		ObjectQuery myQuery = query.clone();
		// TODO: better handle original values in paging
		ObjectPaging myPaging = ObjectPaging.createPaging(0, blockSize);
		myQuery.setPaging(myPaging);
		boolean cont = true;
		while (cont) {
			List<PrismObject<T>> objects = repositoryService.searchObjects(type, myQuery, null, opResult);
			for (PrismObject<T> object: objects) {
				if (!handler.handle(object)) {
	                return;
	            }
			}
			cont = objects.size() == blockSize;
			myPaging.setOffset(myPaging.getOffset() + blockSize);
		}
	}

	/**
	 * Resolves references contained in given PrismObject.
	 *
	 * @param object
	 * @param repository
	 * @param enforceReferentialIntegrity If true, missing reference causes fatal error when processing (if false, only warning is issued).
	 * @param forceFilterReevaluation If true, references are reevaluated even if OID is present. (Given that filter is present as well, of course.)
	 * @param prismContext
	 * @param result
	 */
	public static <T extends ObjectType> void resolveReferences(PrismObject<T> object, RepositoryService repository,
	    		boolean enforceReferentialIntegrity, boolean forceFilterReevaluation, EvaluationTimeType resolutionTime,
				boolean throwExceptionOnFailure,
				PrismContext prismContext, OperationResult result) {
	
		Visitor visitor = visitable -> {
			if (!(visitable instanceof PrismReferenceValue)) {
				return;
			}
			resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
					resolutionTime, prismContext, object.toString(), throwExceptionOnFailure, result);
		};
		object.accept(visitor);
	}

	/**
	 * Resolves references contained in ADD and REPLACE value sets for item modifications in a given ObjectDelta.
	 * (specially treats collisions with values to be deleted)
	 */
	
	public static <T extends ObjectType> void resolveReferences(ObjectDelta<T> objectDelta, RepositoryService repository,
			boolean enforceReferentialIntegrity, boolean forceFilterReevaluation,
			EvaluationTimeType resolutionTime, boolean throwExceptionOnFailure,
			PrismContext prismContext, OperationResult result) {
	
		Visitor visitor = visitable -> {
			if (!(visitable instanceof PrismReferenceValue)) {
				return;
			}
			resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
					resolutionTime, prismContext, objectDelta.toString(), throwExceptionOnFailure, result);
		};
		// We could use objectDelta.accept(visitor), but we want to visit only values to add and replace
		// (NOT values to delete! - otherwise very strange effects could result)
	
		// Another problem is that it is possible that one of valuesToAdd became (after resolving)
		// a value that is meant do be deleted. The result would be deletion of that value; definitely
		// not what we would want or expect. So we have to check whether a value that was not among
		// values to be deleted accidentally becomes one of values to be deleted.
		if (objectDelta.isAdd()) {
			objectDelta.getObjectToAdd().accept(visitor);
		} else if (objectDelta.isModify()) {
			for (ItemDelta<?,?> delta : objectDelta.getModifications()) {
				applyVisitorToValues(delta.getValuesToAdd(), delta, visitor);
				applyVisitorToValues(delta.getValuesToReplace(), delta, visitor);
			}
		}
	}

	// see description in caller
	static void applyVisitorToValues(Collection<? extends PrismValue> values, ItemDelta<?,?> delta, Visitor visitor) {
		Collection<? extends PrismValue> valuesToDelete = delta.getValuesToDelete();
		if (valuesToDelete == null) {
	        valuesToDelete = new ArrayList<>(0);		// just to simplify the code below
	    }
		if (values != null) {
	        for (PrismValue pval : values) {
	            boolean isToBeDeleted = valuesToDelete.contains(pval);
	            pval.accept(visitor);
	            if (!isToBeDeleted && valuesToDelete.contains(pval)) {
	                // value becomes 'to be deleted' -> we remove it from toBeDeleted list
	                delta.removeValueToDelete(pval);
	            }
	        }
	    }
	}

	static void resolveRef(PrismReferenceValue refVal, RepositoryService repository,
			boolean enforceReferentialIntegrity, boolean forceFilterReevaluation, EvaluationTimeType evaluationTimeType,
			PrismContext prismContext, String contextDesc, boolean throwExceptionOnFailure, OperationResult parentResult) {
		String refName = refVal.getParent() != null ?
				refVal.getParent().getElementName().toString() : "(unnamed)";
	
		if ((refVal.getResolutionTime() != null && refVal.getResolutionTime() != evaluationTimeType) ||
				(refVal.getResolutionTime() == null && evaluationTimeType != EvaluationTimeType.IMPORT)) {
			LOGGER.trace("Skipping resolution of reference {} in {} because the resolutionTime is set to {}", refName, contextDesc, refVal.getResolutionTime());
			return;
		}
	
		OperationResult result = parentResult.createMinorSubresult(OPERATION_RESOLVE_REFERENCE);
		result.addContext(OperationResult.CONTEXT_ITEM, refName);
	
		QName typeQName = null;
		if (refVal.getTargetType() != null) {
			typeQName = refVal.getTargetType();
		}
		if (typeQName == null) {
			PrismReferenceDefinition definition = (PrismReferenceDefinition) refVal.getParent().getDefinition();
			if (definition != null) {
				typeQName = definition.getTargetTypeName();
			}
		}
		Class<? extends ObjectType> type = ObjectType.class;
		if (typeQName != null) {
			type = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
			if (type == null) {
				result.recordWarning("Unknown type specified in reference or definition of reference " + refName + ": "
						+ typeQName);
				type = ObjectType.class;
			}
		}
		SearchFilterType filter = refVal.getFilter();
	
		if (!StringUtils.isBlank(refVal.getOid()) && (!forceFilterReevaluation || filter == null)) {
			// We have OID (and "force filter reevaluation" is not requested or not possible)
			if (filter != null) {
				// We have both filter and OID. We will choose OID, but let's at
				// least log a warning
				LOGGER.debug("Both OID and filter for property {} in {}, OID takes precedence", refName, contextDesc);
			}
			// Nothing to resolve, but let's check if the OID exists
			PrismObject<? extends ObjectType> object = null;
			try {
				object = repository.getObject(type, refVal.getOid(), null, result);
			} catch (ObjectNotFoundException e) {
				String message = "Reference " + refName + " refers to a non-existing object " + refVal.getOid();
				if (enforceReferentialIntegrity) {
					LOGGER.error(message);
					result.recordFatalError(message);
					if (throwExceptionOnFailure) {
						throw new SystemException(message, e);
					}
				} else {
					LOGGER.warn(message);
					result.recordWarning(message);
				}
			} catch (SchemaException e) {
				String message = "Schema error while trying to retrieve object " + refVal.getOid() + " : " + e.getMessage();
				result.recordPartialError(message, e);
				LOGGER.error(message, e);
				// But continue otherwise
			}
			if (object != null && refVal.getOriginType() != null) {
				// Check if declared and actual type matches
				if (!object.getClass().equals(type)) {
					result.recordWarning("Type mismatch on property " + refName + ": declared:"
							+ refVal.getOriginType() + ", actual: " + object.getClass());
				}
			}
			result.recordSuccessIfUnknown();
			parentResult.computeStatus();
			return;
		}
	
		if (filter == null) {
			if (refVal.getObject() != null) {
				LOGGER.trace("Skipping resolution of reference {} in {} because the object is present and the filter is not", refName, contextDesc);
				result.recordNotApplicableIfUnknown();
				return;
			}
			// No OID and no filter. We are lost.
			String message = "Neither OID nor filter for property " + refName + ": cannot resolve reference";
			result.recordFatalError(message);
			if (throwExceptionOnFailure) {
				throw new SystemException(message);
			}
			return;
		}
		// No OID and we have filter. Let's check the filter a bit
		ObjectFilter objFilter;
		try{
			PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
			objFilter = prismContext.getQueryConverter().parseFilter(filter, objDef);
		} catch (SchemaException ex){
			LOGGER.error("Failed to convert object filter from filter because of: "+ ex.getMessage() + "; filter: " + filter.debugDump(), ex);
			throw new SystemException("Failed to convert object filter from filter. Reason: " + ex.getMessage(), ex);
		}
	
		LOGGER.trace("Resolving using filter {}", objFilter.debugDump());
		List<PrismObject<? extends ObjectType>> objects;
		QName objectType = refVal.getTargetType();
		if (objectType == null) {
			String message = "Missing definition of type of reference " + refName;
			result.recordFatalError(message);
			if (throwExceptionOnFailure) {
				throw new SystemException(message);
			}
			return;
		}
	
		if (containExpression(objFilter)){
			result.recordSuccessIfUnknown();
			return;
		}
	
		try {
			ObjectQuery query = ObjectQuery.createObjectQuery(objFilter);
			objects = (List)repository.searchObjects(type, query, null, result);
	
		} catch (SchemaException e) {
			// This is unexpected, but may happen. Record fatal error
			String message = "Repository schema error during resolution of reference " + refName;
			result.recordFatalError(message, e);
			if (throwExceptionOnFailure) {
				throw new SystemException(message, e);
			}
			return;
		} catch (SystemException e) {
			// We don't want this to tear down entire import.
			String message = "Repository system error during resolution of reference " + refName;
			result.recordFatalError(message, e);
			if (throwExceptionOnFailure) {
				throw new SystemException(message, e);
			}
			return;
		}
		if (objects.isEmpty()) {
			String message = "Repository reference " + refName + " cannot be resolved: filter matches no object";
			result.recordFatalError(message);
			if (throwExceptionOnFailure) {
				throw new SystemException(message);
			}
			return;
		}
		if (objects.size() > 1) {
			String message = "Repository reference " + refName
					+ " cannot be resolved: filter matches " + objects.size() + " objects";
			result.recordFatalError(message);
			if (throwExceptionOnFailure) {
				throw new SystemException(message);
			}
			return;
		}
		// Bingo. We have exactly one object.
		String oid = objects.get(0).getOid();
		refVal.setOid(oid);
		result.recordSuccessIfUnknown();
	}

	private static boolean containExpression(ObjectFilter filter){
		if (filter == null){
			return false;
		}
		if (filter instanceof InOidFilter && ((InOidFilter) filter).getExpression() != null) {
			return true;
		}
		if (filter instanceof FullTextFilter && ((FullTextFilter) filter).getExpression() != null) {
			return true;
		}
		if (filter instanceof ValueFilter && ((ValueFilter) filter).getExpression() != null) {
			return true;
		}
		return false;
	}

	public static ObjectClassComplexTypeDefinition determineObjectClass(RefinedResourceSchema refinedSchema, Task task) throws SchemaException {
	
	    QName objectclass = null;
	    PrismProperty<QName> objectclassProperty = task.getExtensionProperty(ModelConstants.OBJECTCLASS_PROPERTY_NAME);
	    if (objectclassProperty != null) {
	        objectclass = objectclassProperty.getValue().getValue();
	    }
	
	    ShadowKindType kind = null;
	    PrismProperty<ShadowKindType> kindProperty = task.getExtensionProperty(ModelConstants.KIND_PROPERTY_NAME);
	    if (kindProperty != null) {
	        kind = kindProperty.getValue().getValue();
	    }
	
	    String intent = null;
	    PrismProperty<String> intentProperty = task.getExtensionProperty(ModelConstants.INTENT_PROPERTY_NAME);
	    if (intentProperty != null) {
	        intent = intentProperty.getValue().getValue();
	    }
	
	    return determineObjectClassInternal(refinedSchema, objectclass, kind, intent, task);
	}

	public static ObjectClassComplexTypeDefinition determineObjectClass(RefinedResourceSchema refinedSchema, PrismObject<ShadowType> shadow) throws SchemaException {
	    ShadowType s = shadow.asObjectable();
	    return determineObjectClassInternal(refinedSchema, s.getObjectClass(), s.getKind(), s.getIntent(), s);
	}

	private static ObjectClassComplexTypeDefinition determineObjectClassInternal(
	        RefinedResourceSchema refinedSchema, QName objectclass, ShadowKindType kind, String intent, Object source) throws SchemaException {
	
	    if (kind == null && intent == null && objectclass != null) {
	    	// Return generic object class definition from resource schema. No kind/intent means that we want
	    	// to process all kinds and intents in the object class.
	    	ObjectClassComplexTypeDefinition objectClassDefinition = refinedSchema.getOriginalResourceSchema().findObjectClassDefinition(objectclass);
	    	if (objectClassDefinition == null) {
	    		throw new SchemaException("No object class "+objectclass+" in the schema for "+source);
	    	}
	    	return objectClassDefinition;
	    }
	
	    RefinedObjectClassDefinition refinedObjectClassDefinition;
	
	    if (kind != null) {
	    	refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
	    	LOGGER.trace("Determined refined object class {} by using kind={}, intent={}",
	    			new Object[]{refinedObjectClassDefinition, kind, intent});
	    } else if (objectclass != null) {
	    	refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(objectclass);
	    	LOGGER.trace("Determined refined object class {} by using objectClass={}",
	    			new Object[]{refinedObjectClassDefinition, objectclass});
	    } else {
	    	if (LOGGER.isTraceEnabled()) {
	            LOGGER.debug("No kind or objectclass specified in {}, assuming null object class", source);
	        }
	    	refinedObjectClassDefinition = null;
	    }
	
	    return refinedObjectClassDefinition;
	}

	public static void encrypt(Collection<ObjectDelta<? extends ObjectType>> deltas, Protector protector, ModelExecuteOptions options,
			OperationResult result) {
		// Encrypt values even before we log anything. We want to avoid showing unencrypted values in the logfiles
		if (!ModelExecuteOptions.isNoCrypt(options)) {
			for(ObjectDelta<? extends ObjectType> delta: deltas) {
				try {
					CryptoUtil.encryptValues(protector, delta);
				} catch (EncryptionException e) {
					result.recordFatalError(e);
					throw new SystemException(e.getMessage(), e);
				}
			}
		}
	}

	public static void setRequestee(Task task, LensContext context) {
	    PrismObject<? extends ObjectType> object;
	    if (context != null && context.getFocusContext() != null
	            && UserType.class.isAssignableFrom(context.getFocusContext().getObjectTypeClass())) {
	        object = context.getFocusContext().getObjectAny();
	    } else {
	        object = null;
	    }
	    setRequestee(task, object);
	}

	public static <F extends ObjectType> void setRequestee(Task task, LensFocusContext<F> context) {
	    setRequestee(task, context.getLensContext());
	}

	public static void setRequestee(Task task, PrismObject object) {
	    LOGGER.trace("setting requestee in {} to {}", task, object);
	    if (task != null) {
	        task.setRequesteeTransient(object);
	    }
	}

	public static void clearRequestee(Task task) {
	    setRequestee(task, (PrismObject) null);
	}

	public static boolean isDryRun(Task task) throws SchemaException {
		Boolean dryRun = isDryRunInternal(task);
		if (dryRun == null && task.isLightweightAsynchronousTask() && task.getParentForLightweightAsynchronousTask() != null) {
			dryRun = isDryRunInternal(task.getParentForLightweightAsynchronousTask());
		}
		return dryRun != null ? dryRun : Boolean.FALSE;
	}

	static Boolean isDryRunInternal(Task task) throws SchemaException{
		Validate.notNull(task, "Task must not be null.");
		if (task.getExtension() == null) {
			return null;
		}
		PrismProperty<Boolean> item = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
		if (item == null || item.isEmpty()) {
			return null;
		}
		if (item.getValues().size() > 1) {
			throw new SchemaException("Unexpected number of values for option 'dry run'.");
		}
		return item.getValues().iterator().next().getValue();
	}

	public static ModelExecuteOptions getModelExecuteOptions(Task task) throws SchemaException {
		Validate.notNull(task, "Task must not be null.");
		if (task.getExtension() == null) {
			return null;
		}
		//LOGGER.info("Task:\n{}",task.debugDump(1));
		PrismProperty<ModelExecuteOptionsType> item = task.getExtensionProperty(SchemaConstants.C_MODEL_EXECUTE_OPTIONS);
		if (item == null || item.isEmpty()) {
			return null;
		}
		//LOGGER.info("Item:\n{}",item.debugDump(1));
		if (item.getValues().size() > 1) {
			throw new SchemaException("Unexpected number of values for option 'modelExecuteOptions'.");
		}
		ModelExecuteOptionsType modelExecuteOptionsType = item.getValues().iterator().next().getValue();
		if (modelExecuteOptionsType == null) {
			return null;
		}
		//LOGGER.info("modelExecuteOptionsType: {}",modelExecuteOptionsType);
		ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromModelExecutionOptionsType(modelExecuteOptionsType);
		//LOGGER.info("modelExecuteOptions: {}",modelExecuteOptions);
		return modelExecuteOptions;
	}

	public static ExpressionVariables getDefaultExpressionVariables(@NotNull LensContext<?> context, @Nullable LensProjectionContext projCtx) throws SchemaException {
		ExpressionVariables variables = new ExpressionVariables();
		if (context.getFocusContext() != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, context.getFocusContext().getObjectDeltaObject());
			variables.addVariableDefinition(ExpressionConstants.VAR_USER, context.getFocusContext().getObjectDeltaObject());
		}
		if (projCtx != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projCtx.getObjectDeltaObject());
			variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projCtx.getObjectDeltaObject());
			variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projCtx.getObjectDeltaObject());
			variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource());
		}
	
		variables.addVariableDefinition(ExpressionConstants.VAR_OPERATION, projCtx.getOperation().getValue());
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, LensUtil.getIterationVariableValue(projCtx));
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, LensUtil.getIterationTokenVariableValue(projCtx));
	
		variables.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, context.getSystemConfiguration());
		return variables;
	}

	public static ExpressionVariables getDefaultExpressionVariables(ObjectType focusType,
			ShadowType shadowType, ResourceType resourceType, SystemConfigurationType configurationType) {
		PrismObject<? extends ObjectType> focus = null;
		if (focusType != null) {
			focus = focusType.asPrismObject();
		}
		PrismObject<? extends ShadowType> shadow = null;
		if (shadowType != null) {
			shadow = shadowType.asPrismObject();
		}
		PrismObject<ResourceType> resource = null;
		if (resourceType != null) {
			resource = resourceType.asPrismObject();
		}
		PrismObject<SystemConfigurationType> configuration = null;
		if (configurationType != null) {
			configuration = configurationType.asPrismObject();
		}
		return getDefaultExpressionVariables(focus, shadow, null, resource, configuration, null);
	}

	public static <O extends ObjectType> ExpressionVariables getDefaultExpressionVariables(PrismObject<? extends ObjectType> focus,
			PrismObject<? extends ShadowType> shadow, ResourceShadowDiscriminator discr,
			PrismObject<ResourceType> resource, PrismObject<SystemConfigurationType> configuration, LensElementContext<O> affectedElementContext) {
		ExpressionVariables variables = new ExpressionVariables();
		addDefaultExpressionVariables(variables, focus, shadow, discr, resource, configuration, affectedElementContext);
		return variables;
	}

	public static <O extends ObjectType> void addDefaultExpressionVariables(ExpressionVariables variables, PrismObject<? extends ObjectType> focus,
			PrismObject<? extends ShadowType> shadow, ResourceShadowDiscriminator discr,
			PrismObject<ResourceType> resource, PrismObject<SystemConfigurationType> configuration, LensElementContext<O> affectedElementContext) {
	
	    // Legacy. And convenience/understandability.
	    if (focus == null || focus.canRepresent(UserType.class) || (discr != null && discr.getKind() == ShadowKindType.ACCOUNT)) {
		    variables.addVariableDefinition(ExpressionConstants.VAR_USER, focus);
	        variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadow);
	    }
	
	    variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus);
		variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadow);
		variables.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadow);
		variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
		variables.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, configuration);
	
		if (affectedElementContext != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_OPERATION, affectedElementContext.getOperation().getValue());
		}
	}

	public static void addAssignmentPathVariables(AssignmentPathVariables assignmentPathVariables, ExpressionVariables expressionVariables) {
		if (assignmentPathVariables != null) {
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignmentPathVariables.getMagicAssignment());
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_PATH, assignmentPathVariables.getAssignmentPath());
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ASSIGNMENT, assignmentPathVariables.getImmediateAssignment());
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_THIS_ASSIGNMENT, assignmentPathVariables.getThisAssignment());
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_FOCUS_ASSIGNMENT, assignmentPathVariables.getFocusAssignment());
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ROLE, assignmentPathVariables.getImmediateRole());
		} else {
			// to avoid "no such variable" exceptions in boundary cases
			// for null/empty paths we might consider creating empty AssignmentPathVariables objects to keep null/empty path distinction
			expressionVariables.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_PATH, null);
		}
	}

	public static PrismReferenceValue determineAuditTargetDeltaOps(Collection<ObjectDeltaOperation<? extends ObjectType>> deltaOps) {
		if (deltaOps == null || deltaOps.isEmpty()) {
			return null;
		}
		if (deltaOps.size() == 1) {
			ObjectDeltaOperation<? extends ObjectType> deltaOp = deltaOps.iterator().next();
			return getAditTarget(deltaOp.getObjectDelta());
		}
		for (ObjectDeltaOperation<? extends ObjectType> deltaOp: deltaOps) {
			if (!ShadowType.class.isAssignableFrom(deltaOp.getObjectDelta().getObjectTypeClass())) {
				return getAditTarget(deltaOp.getObjectDelta());
			}
		}
		// Several raw operations, all on shadows, no focus ... this should not happen
		// But if it does we rather do not specify any target. We should not like to choose
		// target randomly. That would be confusing.
		return null;
	}

	public static PrismReferenceValue determineAuditTarget(Collection<ObjectDelta<? extends ObjectType>> deltas) {
		if (deltas == null || deltas.isEmpty()) {
			return null;
		}
		if (deltas.size() == 1) {
			ObjectDelta<? extends ObjectType> delta = deltas.iterator().next();
			return getAditTarget(delta);
		}
		for (ObjectDelta<? extends ObjectType> delta: deltas) {
			if (!ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())) {
				return getAditTarget(delta);
			}
		}
		// Several raw operations, all on shadows, no focus ... this should not happen
		// But if it does we rather do not specify any target. We should not like to choose
		// target randomly. That would be confusing.
		return null;
	}

	public static PrismReferenceValue getAditTarget(ObjectDelta<? extends ObjectType> delta) {
		PrismReferenceValue targetRef = new PrismReferenceValueImpl(delta.getOid());
		targetRef.setTargetType(ObjectTypes.getObjectType(delta.getObjectTypeClass()).getTypeQName());
		if (delta.isAdd()) {
			targetRef.setObject(delta.getObjectToAdd());
		}
		return targetRef;
	}

	public static <V extends PrismValue, F extends ObjectType> List<V> evaluateScript(
	            ScriptExpression scriptExpression, LensContext<F> lensContext, ExpressionVariables variables, boolean useNew, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
			ExpressionEnvironment<F> env = new ExpressionEnvironment<>();
			env.setLensContext(lensContext);
			env.setCurrentResult(parentResult);
			env.setCurrentTask(task);
			ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
	        try {
	            return scriptExpression.evaluate(variables, ScriptExpressionReturnTypeType.SCALAR, useNew, shortDesc, task, parentResult);
	        } finally {
	        	ModelExpressionThreadLocalHolder.popExpressionEnvironment();
	//			if (lensContext.getDebugListener() != null) {
	//				lensContext.getDebugListener().afterScriptEvaluation(lensContext, scriptExpression);
	//			}
	        }
	    }

	public static CriticalityType handleConnectorErrorCriticality(ResourceType resourceType, Throwable e, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, 
	SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
		CriticalityType criticality;
		if (resourceType == null) {
			throwException(e, result);
			return CriticalityType.FATAL; // not reached
		} else {
			ErrorSelectorType errorSelector = ResourceTypeUtil.getConnectorErrorCriticality(resourceType);
			if (e instanceof CommunicationException) {
				// Network problem. Just continue evaluation. The error is recorded in the result.
				// The consistency mechanism has (most likely) already done the best.
				// We cannot do any better.
				criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.PARTIAL);
			} else if (e instanceof SchemaException || e instanceof PolicyViolationException) {
				// This may be caused by a variety of causes. It may be multiple values in a single-valued attribute.
				// But it may also be duplicate value or a problem of resource-side password policy.
				// Treat this as partial error by default. This is partially motivated by compatibility with
				// midPoint 3.8 and earlier. This may be reviewed in the future.
				criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.PARTIAL);
			} else {
				criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.FATAL);
			}
		}
		switch (criticality) {
			case FATAL:
				LOGGER.debug("Exception {} criticality set as FATAL in {}, stopping evaluation; exception message: {}", e.getClass().getSimpleName(), resourceType, e.getMessage());
				LOGGER.error("Fatal error while processing projection on {}: {}", resourceType, e.getMessage(), e);
				throwException(e, result);
				break; // not reached
			case PARTIAL:
				LOGGER.debug("Exception {} criticality set as PARTIAL in {}, continuing evaluation; exception message: {}", e.getClass().getSimpleName(), resourceType, e.getMessage());
				if (result != null) {
					result.recordPartialError(e);
				}
				LOGGER.warn("Partial error while processing projection on {}: {}", resourceType, e.getMessage(), e);
				break;
			case IGNORE:
				LOGGER.debug("Exception {} criticality set as IGNORE in {}, continuing evaluation; exception message: {}", e.getClass().getSimpleName(), resourceType, e.getMessage());
				if (result != null) {
					result.recordHandledError(e);
				}
				LOGGER.debug("Ignored error while processing projection on {}: {}", resourceType, e.getMessage(), e);
				break;
		}
		return criticality;
	}

	static void throwException(Throwable e, OperationResult result) 
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, 
				SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
				PreconditionViolationException {
		if (result != null) {
			result.recordFatalError(e);
		}
		if (e instanceof RuntimeException) {
			throw (RuntimeException)e;
		} else if (e instanceof Error) {
			throw (Error)e;
		} else if (e instanceof ObjectNotFoundException) {
			throw (ObjectNotFoundException)e;
		} else if (e instanceof CommunicationException) {
			throw (CommunicationException)e;
		} else if (e instanceof SchemaException) {
			throw (SchemaException)e;
		} else if (e instanceof ConfigurationException) {
			throw (ConfigurationException)e;
		} else if (e instanceof SecurityViolationException) {
			throw (SecurityViolationException)e;
		} else if (e instanceof PolicyViolationException) {
			throw (PolicyViolationException)e;
		} else if (e instanceof ExpressionEvaluationException) {
			throw (ExpressionEvaluationException)e;
		} else if (e instanceof ObjectAlreadyExistsException) {
			throw (ObjectAlreadyExistsException)e;
		} else if (e instanceof PreconditionViolationException) {
			throw (PreconditionViolationException)e;
		} else {
			throw new SystemException(e.getMessage(), e);
		}
	}

}

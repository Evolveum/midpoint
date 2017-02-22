/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
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
import java.util.List;

/**
 * @author lazyman
 * @author semancik
 */
public final class Utils {

	 private static final Trace LOGGER = TraceManager.getTrace(Utils.class);
	private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
	            + ".resolveReference";
	
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
	public static <T extends ObjectType> void resolveReferences(final PrismObject<T> object, final RepositoryService repository,
	    		final boolean enforceReferentialIntegrity, final boolean forceFilterReevaluation, final EvaluationTimeType resolutionTime,
				final PrismContext prismContext, final OperationResult result) {
	    	
	    	Visitor visitor = new Visitor() {
				@Override
				public void visit(Visitable visitable) {
					if (!(visitable instanceof PrismReferenceValue)) {
						return;
					}
					resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
							resolutionTime, prismContext, object.toString(), result);
				}
			};
			object.accept(visitor);
	    }

	/**
	 * Resolves references contained in ADD and REPLACE value sets for item modifications in a given ObjectDelta.
	 * (specially treats collisions with values to be deleted)
	 */

	public static <T extends ObjectType> void resolveReferences(final ObjectDelta<T> objectDelta, final RepositoryService repository,
																final boolean enforceReferentialIntegrity, final boolean forceFilterReevaluation,
																final EvaluationTimeType resolutionTime,
																final PrismContext prismContext, final OperationResult result) {

		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (!(visitable instanceof PrismReferenceValue)) {
					return;
				}
				resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
						resolutionTime, prismContext, objectDelta.toString(), result);
			}
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
	private static void applyVisitorToValues(Collection<? extends PrismValue> values, ItemDelta<?,?> delta, Visitor visitor) {
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
                    ((ItemDelta<PrismValue,?>)delta).removeValueToDelete(pval);
                }
            }
        }
	}

	private static void resolveRef(PrismReferenceValue refVal, RepositoryService repository,
									   boolean enforceReferentialIntegrity, boolean forceFilterReevaluation, EvaluationTimeType evaluationTimeType,
									   PrismContext prismContext, String contextDesc, OperationResult parentResult) {
			QName refName = refVal.getParent().getElementName();
			
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
	        	type = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
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
	            	} else {
	            		LOGGER.warn(message);
	            		result.recordWarning(message);
	            	}
	            } catch (SchemaException e) {
	            	
	                result.recordPartialError("Schema error while trying to retrieve object " + refVal.getOid()
	                        + " : " + e.getMessage(), e);
	                LOGGER.error(
	                        "Schema error while trying to retrieve object " + refVal.getOid() + " : "
	                                + e.getMessage(), e);
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
	            // No OID and no filter. We are lost.
	            result.recordFatalError("Neither OID nor filter for property " + refName
	                    + ": cannot resolve reference");
	            return;
	        }
	        // No OID and we have filter. Let's check the filter a bit
	        
	        ObjectFilter objFilter;
	        try{
	        	PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	        	objFilter = QueryConvertor.parseFilter(filter, objDef);
	        } catch (SchemaException ex){
	        	LOGGER.error("Failed to convert object filter from filter because of: "+ ex.getMessage() + "; filter: " + filter.debugDump(), ex);
	        	throw new SystemException("Failed to convert object filter from filter. Reason: " + ex.getMessage(), ex);
	        }
	        
	        LOGGER.trace("Resolving using filter {}", objFilter.debugDump());
//	        // Let's do resolving
	        List<PrismObject<? extends ObjectType>> objects;
	        QName objectType = refVal.getTargetType();
	        if (objectType == null) {
	            result.recordFatalError("Missing definition of type of reference " + refName);
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
	            result.recordFatalError("Repository schema error during resolution of reference " + refName, e);
	            return;
	        } catch (SystemException e) {
	            // We don't want this to tear down entire import.
	            result.recordFatalError("Repository system error during resolution of reference " + refName, e);
	            return;
	        }
	        if (objects.isEmpty()) {
	            result.recordFatalError("Repository reference " + refName
	                    + " cannot be resolved: filter matches no object");
	            return;
	        }
	        if (objects.size() > 1) {
	            result.recordFatalError("Repository reference " + refName
	                    + " cannot be resolved: filter matches " + objects.size() + " objects");
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
	        	ObjectClassComplexTypeDefinition objectClassDefinition = refinedSchema.findObjectClassDefinition(objectclass);
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
    
    public static boolean isDryRun(Task task) throws SchemaException{
		Boolean dryRun = isDryRunInternal(task);
		if (dryRun == null && task.isLightweightAsynchronousTask() && task.getParentForLightweightAsynchronousTask() != null) {
			dryRun = isDryRunInternal(task.getParentForLightweightAsynchronousTask());
		}
		return dryRun != null ? dryRun : Boolean.FALSE;
	}

    private static Boolean isDryRunInternal(Task task) throws SchemaException{
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
		variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
		variables.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, configuration);
		
		if (affectedElementContext != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_OPERATION, affectedElementContext.getOperation().getValue());
		}
	}
    
    public static void addAssignmentPathVariables(AssignmentPathVariables assignmentPathVariables, ExpressionVariables expressionVariables) {
    	expressionVariables.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignmentPathVariables.getMagicAssignment());
    	expressionVariables.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ASSIGNMENT, assignmentPathVariables.getImmediateAssignment());
    	expressionVariables.addVariableDefinition(ExpressionConstants.VAR_THIS_ASSIGNMENT, assignmentPathVariables.getThisAssignment());
    	expressionVariables.addVariableDefinition(ExpressionConstants.VAR_FOCUS_ASSIGNMENT, assignmentPathVariables.getFocusAssignment());
    	expressionVariables.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ROLE, assignmentPathVariables.getImmediateRole());
    }

	public static String getPolicyDesc(ObjectSynchronizationType synchronizationPolicy) {
		if (synchronizationPolicy == null) {
			return null;
		}
		if (synchronizationPolicy.getName() != null) {
			return synchronizationPolicy.getName();
		}
		return synchronizationPolicy.toString();
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
		PrismReferenceValue targetRef = new PrismReferenceValue(delta.getOid());
		targetRef.setTargetType(ObjectTypes.getObjectType(delta.getObjectTypeClass()).getTypeQName());
		if (delta.isAdd()) {
			targetRef.setObject(delta.getObjectToAdd());
		}
		return targetRef;
	}
	
	public static <V extends PrismValue, F extends ObjectType> List<V> evaluateScript(
            ScriptExpression scriptExpression, LensContext<F> lensContext, ExpressionVariables variables, boolean useNew, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        ModelExpressionThreadLocalHolder.pushLensContext(lensContext);
        ModelExpressionThreadLocalHolder.pushCurrentResult(parentResult);
        ModelExpressionThreadLocalHolder.pushCurrentTask(task);
        try {
            return scriptExpression.evaluate(variables, ScriptExpressionReturnTypeType.SCALAR, useNew, shortDesc, task, parentResult);
        } finally {
            ModelExpressionThreadLocalHolder.popLensContext();
            ModelExpressionThreadLocalHolder.popCurrentResult();
            ModelExpressionThreadLocalHolder.popCurrentTask();
//			if (lensContext.getDebugListener() != null) {
//				lensContext.getDebugListener().afterScriptEvaluation(lensContext, scriptExpression);
//			}
        }
    }
	
}

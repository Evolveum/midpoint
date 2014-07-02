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

package com.evolveum.midpoint.model.impl.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public final class Utils {

	 private static final Trace LOGGER = TraceManager.getTrace(Utils.class);
	private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
	            + ".resolveReference";
	
    private Utils() {
    }

    public static void resolveResource(ShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException, 
            SecurityViolationException {

        Validate.notNull(shadow, "Resource object shadow must not be null.");
        Validate.notNull(provisioning, "Provisioning service must not be null.");

        ResourceType resource = getResource(shadow, provisioning, result);
        shadow.setResourceRef(null);
        shadow.setResource(resource);
    }

    public static ResourceType getResource(ShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException, 
            SecurityViolationException {

        if (shadow.getResource() != null) {
            return shadow.getResource();
        }

        if (shadow.getResourceRef() == null) {
            throw new IllegalArgumentException("Couldn't resolve resource. Resource object shadow doesn't" +
                    " contain resource nor resource ref.");
        }

        ObjectReferenceType resourceRef = shadow.getResourceRef();
        return provisioning.getObject(ResourceType.class, resourceRef.getOid(), null, null, result).asObjectable();
    }
    
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
	
	  public static <T extends ObjectType> void resolveReferences(final PrismObject<T> object, final RepositoryService repository,
	    		final boolean enforceReferentialIntegrity, final PrismContext prismContext, final OperationResult result) {
	    	
	    	Visitor visitor = new Visitor() {
				@Override
				public void visit(Visitable visitable) {
					if (!(visitable instanceof PrismReferenceValue)) {
						return;
					}
					resolveRef((PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, prismContext, object.toString(), result);
				}
			};
			object.accept(visitor);
	    }
	    

	    private static void resolveRef(PrismReferenceValue refVal, RepositoryService repository,
	    				boolean enforceReferentialIntegrity, PrismContext prismContext, String contextDesc, OperationResult parentResult) {
	    	PrismReference reference = (PrismReference) refVal.getParent();
	    	QName refName = reference.getElementName();
	        OperationResult result = parentResult.createSubresult(OPERATION_RESOLVE_REFERENCE);
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
	        
	        if (!StringUtils.isBlank(refVal.getOid())) {
	            // We have OID
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
	        
	        ObjectFilter objFilter =  null;
	        try{
	        	PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	        	objFilter = QueryConvertor.parseFilter(filter, objDef);
	        } catch (SchemaException ex){
	        	LOGGER.error("Failed to convert object filter from filter because of: "+ ex.getMessage() + "; filter: " + filter.debugDump(), ex);
	        	throw new SystemException("Failed to convert object filter from filter. Reason: " + ex.getMessage(), ex);
	        }
	        
	        LOGGER.trace("Resolving using filter {}", objFilter.debugDump());
//	        NodeList childNodes = filter.getChildNodes();
//	        if (childNodes.getLength() == 0) {
	        if (objFilter == null){
	            result.recordFatalError("OID not specified and filter is empty for property " + refName);
	            return;
	        }
//	        // Let's do resolving
//	        QueryType queryType = new QueryType();
//	        queryType.setFilter(filter);
	        List<PrismObject<? extends ObjectType>> objects = null;
	        QName objectType = refVal.getTargetType();
	        if (objectType == null) {
	            result.recordFatalError("Missing definition of type of reference " + refName);
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

        public static ObjectClassComplexTypeDefinition determineObjectClass(RefinedResourceSchema refinedSchema, Task task) {

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

        public static ObjectClassComplexTypeDefinition determineObjectClass(RefinedResourceSchema refinedSchema, PrismObject<ShadowType> shadowToImport) {
            ShadowType s = shadowToImport.asObjectable();
            return determineObjectClassInternal(refinedSchema, s.getObjectClass(), s.getKind(), s.getIntent(), s);
        }

	    private static ObjectClassComplexTypeDefinition determineObjectClassInternal(
                RefinedResourceSchema refinedSchema, QName objectclass, ShadowKindType kind, String intent, Object source) {
	    	
	        if (kind == null && intent == null && objectclass != null) {
	        	// Return generic object class definition from resource schema. No kind/intent means that we want
	        	// to process all kinds and intents in the object class.
	        	ObjectClassComplexTypeDefinition objectClassDefinition = refinedSchema.findObjectClassDefinition(objectclass);
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
                    LOGGER.debug("No kind or objectclass specified in {}, using default values", source);
                }
	        	refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
	        	LOGGER.trace("Determined refined object class {} by using default ACCOUNT kind",
	        			new Object[]{refinedObjectClassDefinition});
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
    	
    	Validate.notNull(task, "Task must not be null.");
    	
    	if (task.getExtension() == null){
    		return false;
    	}
		
    	PrismProperty<Boolean> item = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
		if (item == null || item.isEmpty()){
			return false;
		}
		
		if (item.getValues().size() > 1){
			throw new SchemaException("Unexpected number of values for option 'dry run'.");
		}
				
		Boolean dryRun = item.getValues().iterator().next().getValue();
		
		if (dryRun == null){
			return false;
		}
    	
		return dryRun.booleanValue(); 
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
		return getDefaultExpressionVariables(focus, shadow, null, resource, configuration);
    }
    
    public static ExpressionVariables getDefaultExpressionVariables(PrismObject<? extends ObjectType> focus,
    		PrismObject<? extends ShadowType> shadow, ResourceShadowDiscriminator discr, 
    		PrismObject<ResourceType> resource, PrismObject<SystemConfigurationType> configuration) {
    	ExpressionVariables variables = new ExpressionVariables();
    	addDefaultExpressionVariables(variables, focus, shadow, discr, resource, configuration);
    	return variables;
    }
    
    public static void addDefaultExpressionVariables(ExpressionVariables variables, PrismObject<? extends ObjectType> focus,
    		PrismObject<? extends ShadowType> shadow, ResourceShadowDiscriminator discr, 
    		PrismObject<ResourceType> resource, PrismObject<SystemConfigurationType> configuration) {

        // Legacy. And convenience/understandability.
        if (focus == null || (focus != null && focus.canRepresent(UserType.class))
                || (discr != null && discr.getKind() == ShadowKindType.ACCOUNT)) {
		    variables.addVariableDefinition(ExpressionConstants.VAR_USER, focus);
            variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadow);
        }

        variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus);
		variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadow);
		variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
		variables.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, configuration);
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

	public static PrismObject<SystemConfigurationType> getSystemConfiguration(RepositoryService repositoryService, OperationResult result) throws SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = null;
		try {
				systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
						null, result);
		} catch (ObjectNotFoundException e) {
			// just go on ... we will return and continue
			// This is needed e.g. to set up new system configuration is the old one gets deleted
		}
		if (systemConfiguration == null) {
		    // throw new SystemException("System configuration object is null (should not happen!)");
		    // This should not happen, but it happens in tests. And it is a convenient short cut. Tolerate it for now.
		    LOGGER.warn("System configuration object is null (should not happen!)");
		    return null;
		}
		return systemConfiguration;
	}
}

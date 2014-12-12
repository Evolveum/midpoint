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
package com.evolveum.midpoint.model.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.task.api.TaskManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
@Component
public class ModelObjectResolver implements ObjectResolver {

	@Autowired(required = true)
	private transient ProvisioningService provisioning;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
	private transient PrismContext prismContext;

    @Autowired
    private transient TaskManager taskManager;

    @Autowired(required = false)
    private transient HookRegistry hookRegistry;
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelObjectResolver.class);
	
	@Override
	public <O extends ObjectType> O resolve(ObjectReferenceType ref, Class<O> expectedType, Collection<SelectorOptions<GetOperationOptions>> options,
			String contextDescription, OperationResult result) throws ObjectNotFoundException, SchemaException {
				String oid = ref.getOid();
				Class<?> typeClass = null;
				QName typeQName = ref.getType();
				if (typeQName != null) {
					typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
				}
				if (typeClass != null && expectedType.isAssignableFrom(typeClass)) {
					expectedType = (Class<O>) typeClass;
				}
				try {
					return getObject(expectedType, oid, options, null, result);
				} catch (SystemException ex) {
					throw ex;
				} catch (ObjectNotFoundException ex) {
					throw ex;
				} catch (CommonException ex) {
					LoggingUtils.logException(LOGGER, "Error resolving object with oid {}", ex, oid);
					// Add to result only a short version of the error, the details will be in subresults
					result.recordFatalError(
							"Couldn't get object with oid '" + oid + "': "+ex.getOperationResultMessage(), ex);
					throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
				}
	}
	
	public PrismObject<?> resolve(PrismReferenceValue refVal, String string, Task task, OperationResult result) throws ObjectNotFoundException {
		return resolve(refVal, string, null, task, result);
	}

	public PrismObject<?> resolve(PrismReferenceValue refVal, String string, GetOperationOptions options, Task task, 
			OperationResult result) throws ObjectNotFoundException {
		String oid = refVal.getOid();
		Class<?> typeClass = ObjectType.class;
		QName typeQName = refVal.getTargetType();
		if (typeQName == null && refVal.getParent() != null && refVal.getParent().getDefinition() != null) {
			PrismReferenceDefinition refDef = (PrismReferenceDefinition) refVal.getParent().getDefinition();
			typeQName = refDef.getTargetTypeName();
		}
		if (typeQName != null) {
			typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
		}
		return ((ObjectType) getObjectSimple((Class)typeClass, oid, options, task, result)).asPrismObject();
	}
	
	public <T extends ObjectType> T getObjectSimple(Class<T> clazz, String oid, GetOperationOptions options, Task task, 
			OperationResult result) throws ObjectNotFoundException {
		try {
			return getObject(clazz, oid, SelectorOptions.createCollection(options), task, result);
		} catch (SystemException ex) {
			throw ex;
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (CommonException ex) {
			LoggingUtils.logException(LOGGER, "Error resolving object with oid {}", ex, oid);
			// Add to result only a short version of the error, the details will be in subresults
			result.recordFatalError(
					"Couldn't get object with oid '" + oid + "': "+ex.getOperationResultMessage(), ex);
			throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
		}
	}
	
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, Collection<SelectorOptions<GetOperationOptions>> options, Task task,
			OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		T objectType = null;
		try {
			PrismObject<T> object = null;
            ObjectTypes.ObjectManager manager = ObjectTypes.getObjectManagerForClass(clazz);
            switch (manager) {
                case PROVISIONING:
                    object = provisioning.getObject(clazz, oid, options, task, result);
                    if (object == null) {
                        throw new SystemException("Got null result from provisioning.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using provisioning implementation "+provisioning.getClass().getName());
                    }
                    break;
                case TASK_MANAGER:
                    object = taskManager.getObject(clazz, oid, options, result);
                    if (object == null) {
                        throw new SystemException("Got null result from taskManager.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using task manager implementation "+taskManager.getClass().getName());
                    }
                    break;
                default:
                    object = cacheRepositoryService.getObject(clazz, oid, options, result);
                    if (object == null) {
                        throw new SystemException("Got null result from repository.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using repository implementation "+cacheRepositoryService.getClass().getName());
                    }
            }
			objectType = object.asObjectable();
			if (!clazz.isInstance(objectType)) {
				throw new ObjectNotFoundException("Bad object type returned for referenced oid '" + oid
						+ "'. Expected '" + clazz + "', but was '"
						+ (objectType == null ? "null" : objectType.getClass()) + "'.");
			}

            if (hookRegistry != null) {
                for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                    hook.invoke(object, options, task, result);
                }
            }
		} catch (SystemException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (RuntimeException ex) {
			LoggingUtils.logException(LOGGER, "Error resolving object with oid {}, expected type was {}.", ex,
					oid, clazz);
			throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
		} finally {
			result.computeStatus();
		}

		return objectType;
	}
	
	public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, OperationResult parentResult) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (ObjectTypes.isClassManagedByProvisioning(type)) {
			provisioning.searchObjectsIterative(type, query, options, handler, parentResult);
		} else {
			cacheRepositoryService.searchObjectsIterative(type, query, handler, options, parentResult);
		}
	}
	
	public <O extends ObjectType> Integer countObjects(Class<O> type, ObjectQuery query, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (ObjectTypes.isClassManagedByProvisioning(type)) {
			return provisioning.countObjects(type, query, parentResult);
		} else {
			return cacheRepositoryService.countObjects(type, query, parentResult);
		}
	}
	
}

/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismValidate;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * Simple version of model service exposing CRUD-like operations. This is common facade for webservice and REST services.
 * It takes care of all the "details" of externalized obejcts such as applying correct definitions and so on.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ModelCrudService {
	
	String CLASS_NAME_WITH_DOT = ModelCrudService.class.getName() + ".";
	String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
	String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
	String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";

	
	private static final Trace LOGGER = TraceManager.getTrace(ModelCrudService.class);
	
	@Autowired(required = true)
	ModelController modelController;
	
	@Autowired(required = true)
	PrismContext prismContext;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	RepositoryService repository;
	
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		return modelController.getObject(clazz, oid, options, task, parentResult);
	}	

	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		return modelController.searchObjects(type, query, options, task, parentResult);
	}

	/**
	 * <p>
	 * Add new object.
	 * </p>
	 * <p>
	 * The OID provided in the input message may be empty. In that case the OID
	 * will be assigned by the implementation of this method and it will be
	 * provided as return value.
	 * </p>
	 * <p>
	 * This operation should fail if such object already exists (if object with
	 * the provided OID already exists).
	 * </p>
	 * <p>
	 * The operation may fail if provided OID is in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * </p>
	 * <p>
	 * Should be atomic. Should not allow creation of two objects with the same
	 * OID (even if created in parallel).
	 * </p>
	 * <p>
	 * The operation may fail if the object to be created does not conform to
	 * the underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * </p>
	 * 
	 * @param object
	 *            object to create
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * @throws ObjectAlreadyExistsException
	 *             object with specified identifiers already exists, cannot add
	 * @throws ObjectNotFoundException
	 *             object required to complete the operation was not found (e.g.
	 *             appropriate connector or resource definition)
	 * @throws SchemaException
	 *             error dealing with resource schema, e.g. created object does
	 *             not conform to schema
	 * @throws ExpressionEvaluationException 
	 * 				evaluation of expression associated with the object has failed
	 * @throws CommunicationException 
	 * @throws ConfigurationException 
	 * @throws PolicyViolationException
	 * 				Policy violation was detected during processing of the object
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	public <T extends ObjectType> String addObject(PrismObject<T> object, Task task,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");

		object.checkConsistence();
		
		T objectType = object.asObjectable();
		// FIXME??
		prismContext.adopt(objectType);
		if (!(objectType instanceof ShadowType)) {
			PrismValidate.notEmpty(objectType.getName(), "Object name must not be null or empty.");
		}

		OperationResult result = parentResult.createSubresult(ADD_OBJECT);
		result.addParams(new String[] { "object" }, object);
		String oid = null;

		// Task task = taskManager.createTaskInstance(); // in the future, this
		// task instance will come from GUI

		Utils.resolveReferences(object, repository, false, prismContext, result);
        
		
		RepositoryCache.enter();
		try {

//			if (LOGGER.isTraceEnabled()) {
//				LOGGER.trace("Entering addObject with {}", object);
//				LOGGER.trace(object.dump());
//			}
			
			ObjectDelta<T> objectDelta = ObjectDelta.createAddDelta(object);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			modelController.executeChanges(deltas, null, task, result);
			
			oid = objectDelta.getOid();

			result.computeStatus();
			result.cleanupResult();

		} catch (ExpressionEvaluationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SchemaException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ConfigurationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}

		return oid;
	}
	
	/**
	 * <p>
	 * Deletes object with specified OID.
	 * </p>
	 * <p>
	 * Must fail if object with specified OID does not exists. Should be atomic.
	 * </p>
	 * 
	 * @param oid
	 *            OID of object to delete
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws ConsistencyViolationException
	 *             sub-operation failed, cannot delete objects as its deletion
	 *             would lead to inconsistent state
	 * @throws CommunicationException 
	 * @throws ConfigurationException 
	 * @throws PolicyViolationException 
	 * 				Policy violation was detected during processing of the object
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	public <T extends ObjectType> void deleteObject(Class<T> clazz, String oid, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, ConsistencyViolationException,
			CommunicationException, SchemaException, ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		Validate.notNull(clazz, "Class must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		OperationResult result = parentResult.createSubresult(DELETE_OBJECT);
		result.addParams(new String[] { "oid" }, oid);

		RepositoryCache.enter();

		try {
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(clazz, ChangeType.DELETE, prismContext);
			objectDelta.setOid(oid);

			LOGGER.trace("Deleting object with oid {}.", new Object[] { oid });
			
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			modelController.executeChanges(deltas, null, task, result);

			result.recordSuccess();

		} catch (ObjectNotFoundException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (CommunicationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw new SystemException(ex.getMessage(), ex);
		} catch (ExpressionEvaluationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			RepositoryCache.exit();
		}
	}
	
	/**
	 * <p>
	 * Modifies object using relative change description.
	 * </p>
	 * <p>
	 * Must fail if user with provided OID does not exists. Must fail if any of
	 * the described changes cannot be applied. Should be atomic.
	 * </p>
	 * <p>
	 * If two or more modify operations are executed in parallel, the operations
	 * should be merged. In case that the operations are in conflict (e.g. one
	 * operation adding a value and the other removing the same value), the
	 * result is not deterministic.
	 * </p>
	 * <p>
	 * The operation may fail if the modified object does not conform to the
	 * underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * </p>
	 * 
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws SchemaException
	 *             resulting object would violate the schema
	 * @throws ExpressionEvaluationException
	 * 				evaluation of expression associated with the object has failed
	 * @throws CommunicationException 
	 * @throws ObjectAlreadyExistsException
	 * 				If the account or another "secondary" object already exists and cannot be created
	 * @throws PolicyViolationException 
	 * 				Policy violation was detected during processing of the object
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {

		Validate.notNull(modifications, "Object modification must not be null.");
		Validate.notEmpty(oid, "Change oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying object with oid {}", oid);
			LOGGER.trace(DebugUtil.debugDump(modifications));
		}

		if (modifications.isEmpty()) {
			LOGGER.warn("Calling modifyObject with empty modificaiton set");
			return;
		}

		ItemDelta.checkConsistence(modifications);
		// TODO: check definitions, but tolerate missing definitions in <attributes>

		OperationResult result = parentResult.createSubresult(MODIFY_OBJECT);
		result.addParams(new String[] { "modifications" }, modifications);

		RepositoryCache.enter();

		try {

			ObjectDelta<T> objectDelta = (ObjectDelta<T>) ObjectDelta.createModifyDelta(oid, modifications, type, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			modelController.executeChanges(deltas, null, task, result);

            result.computeStatus();
			
        } catch (ExpressionEvaluationException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (SchemaException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ConfigurationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
	}

	public PrismObject<UserType> findShadowOwner(String accountOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException {
		return modelController.findShadowOwner(accountOid, task, parentResult);
	}

	public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid, QName objectClass,
			ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {
		return modelController.listResourceObjects(resourceOid, objectClass, paging, task, parentResult);
	}

	public void importFromResource(String resourceOid, QName objectClass, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		modelController.importFromResource(resourceOid, objectClass, task, parentResult);
	}

	public OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException {
		return modelController.testResource(resourceOid, task);
	}
	
}

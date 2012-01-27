/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.repo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.processor.DiffUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
public class RepositoryManagerImpl implements RepositoryManager {

	private static final Trace LOGGER = TraceManager.getTrace(RepositoryManagerImpl.class);
	@Autowired(required = true)
	private transient RepositoryService repositoryService;
	@Autowired(required = true)
	private SchemaRegistry schemaRegistry;
	@Autowired(required = true)
	private TaskManager taskManager;
	@Autowired(required = true)
	AuditService auditService;

	@Override
	public <T extends ObjectType> List<T> listObjects(Class<T> objectType, int offset, int count) {
		Validate.notNull(objectType, "Object type must not be null.");
		LOGGER.debug("Listing objects of type {} paged from {}, count {}.", new Object[] { objectType,
				offset, count });

		List<T> list = null;
		OperationResult result = new OperationResult(LIST_OBJECTS);
		try {
			PagingType paging = PagingTypeFactory.createPaging(offset, count, OrderDirectionType.ASCENDING,
					"name");
			list = repositoryService.listObjects(objectType, paging, result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "List objects of type {} failed", ex, objectType);
			result.recordFatalError("List object failed.", ex);
		}

		printResults(LOGGER, result);

		if (list == null) {
			list = new ArrayList<T>();
		}

		return list;
	}

	@Override
	public List<? extends ObjectType> searchObjects(String name) {
		Validate.notEmpty(name, "Name must not be null.");
		LOGGER.debug("Searching objects with name {}.", new Object[] { name });

		OperationResult result = new OperationResult(SEARCH_OBJECTS);
		List<ObjectType> list = null;
		try {
			QueryType query = new QueryType();
			query.setFilter(ControllerUtil.createQuery(name, null));
			LOGGER.trace(JAXBUtil.silentMarshalWrap(query));
			list = repositoryService.searchObjects(ObjectType.class, query, null, result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't search for object with name {}", ex, name);
			result.recordFatalError("Couldn't search for object '" + name + "'.", ex);
		}

		printResults(LOGGER, result);

		if (list == null) {
			list = new ArrayList<ObjectType>();
		}

		return list;
	}

	@Override
	public ObjectType getObject(String oid) {
		Validate.notEmpty(oid, "Oid must not be null.");
		LOGGER.debug("Getting object with oid {}.", new Object[] { oid });

		OperationResult result = new OperationResult(GET_OBJECT);
		ObjectType object = null;
		try {
			object = repositoryService.getObject(ObjectType.class, oid, new PropertyReferenceListType(),
					result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {}", ex, oid);
			result.recordFatalError("Couldn't get object with oid '" + oid + "'.", ex);
		}

		printResults(LOGGER, result);

		return object;
	}

	@Override
	public boolean saveObject(ObjectType object, String objectAfterChangeXml) {
		Validate.notNull(object, "Object must not be null.");
		LOGGER.debug("Saving object {} (object xml in traces).", new Object[] { object.getName() });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
		}
	
		Task task = taskManager.createTaskInstance(SAVE_OBJECT);
		
		// TODO: !!!!!!!!!!!!!!!!! SET TASK OWNER !!!!!!!!!!!!!!!!!!!!!!!!
		SecurityUtils security = new SecurityUtils();
		PrincipalUser principal = security.getPrincipalUser();
        task.setOwner(principal.getUser());
		
		OperationResult result = task.getResult();
		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.MODIFY_OBJECT,
				AuditEventStage.REQUEST);
		boolean saved = false;
		try {
			ObjectType oldObject = repositoryService.getObject(ObjectType.class, object.getOid(),
					new PropertyReferenceListType(), result);
			if (oldObject != null) {
				String objectBeforeChangeXml = JAXBUtil.marshalWrap(oldObject);
				
				LOGGER.trace("DIFF: object before:\n{}",objectBeforeChangeXml);
				LOGGER.trace("DIFF: object after:\n{}",objectAfterChangeXml);
				
				ObjectDelta<ObjectType> delta = (ObjectDelta<ObjectType>) DiffUtil.diff(objectBeforeChangeXml, objectAfterChangeXml,
						object.getClass(), schemaRegistry.getObjectSchema());
				
				LOGGER.trace("DIFF: diff:\n{}",delta.dump());

				auditRecord.setTarget(oldObject);
				auditRecord.addDelta(delta);

				// ObjectModificationType objectChange =
				// CalculateXmlDiff.calculateChanges(oldObject, object);

				if (delta != null && delta.getOid() != null) {

					auditService.audit(auditRecord, task);
					ObjectModificationType objectModificationType = delta.toObjectModificationType();
					
					// This is direct access to repository, it does not go through model so it won't be auditted otherwise
					// We need to explicitly audit the operation here.
					
					repositoryService.modifyObject(object.getClass(), objectModificationType, result);
				}
				result.recordSuccess();
				saved = true;
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update object {}", ex, object.getName());
			result.recordFatalError("Couldn't update object '" + object.getName() + "'.", ex);
		} finally {
			auditRecord.setEventStage(AuditEventStage.EXECUTION);
			auditRecord.setResult(result);
			auditRecord.clearTimestamp();
			auditService.audit(auditRecord, task);
		}

		printResults(LOGGER, result);

		return saved;
	}

	@Override
	public <T extends ObjectType> boolean deleteObject(Class<T> type, String oid) {
		Validate.notEmpty(oid, "Oid must not be null.");
		LOGGER.debug("Deleting object with oid {}.", new Object[] { oid });

		OperationResult result = new OperationResult(DELETE_OBJECT);
		boolean deleted = false;
		try {
			repositoryService.deleteObject(type, oid, result);
			result.recordSuccess();
			deleted = true;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Delete object with oid {} failed", ex, oid);
			result.recordFatalError("Delete object with oid '" + oid + "' failed.", ex);
		}

		printResults(LOGGER, result);

		return deleted;
	}

	@Override
	public String addObject(ObjectType object) throws ObjectAlreadyExistsException {
		Validate.notNull(object, "Object must not be null.");
		LOGGER.debug("Adding object {} (object xml in traces).", new Object[] { object.getName() });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
		}

		OperationResult result = new OperationResult(ADD_OBJECT);
		String oid = null;
		try {
			oid = repositoryService.addObject(object, result);
			result.recordSuccess();
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError("Object '" + object.getName() + "', oid '" + object.getOid()
					+ "' already exists.", ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Add object {} failed", ex, object.getName());
			result.recordFatalError("Add object '" + object.getName() + "' failed.", ex);
		}

		printResults(LOGGER, result);

		return oid;
	}

	private void printResults(Trace LOGGER, OperationResult result) {
		if (!result.isSuccess()) {
			FacesUtils.addMessage(result);
		}

		LOGGER.debug(result.dump());
	}
}

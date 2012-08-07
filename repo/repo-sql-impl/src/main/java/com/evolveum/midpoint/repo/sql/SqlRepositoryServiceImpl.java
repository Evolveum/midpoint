/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.query.*;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.PessimisticLockException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl extends SqlBaseService implements RepositoryService {

	private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);

	private <T extends ObjectType> PrismObject<T> getObject(Session session, Class<T> type, String oid)
			throws ObjectNotFoundException, SchemaException, DtoTranslationException {
		Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
		query.add(Restrictions.eq("oid", oid));
		query.add(Restrictions.eq("id", 0L));

		RObject object = (RObject) query.uniqueResult();
		if (object == null) {
			throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
					+ "' was not found.", null, oid);
		}

		LOGGER.debug("Transforming data to JAXB type.");
		PrismObject<T> objectType = object.toJAXB(getPrismContext()).asPrismObject();
		validateObjectType(objectType, type);

		return objectType;
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		Validate.notNull(type, "Object type must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		final String operation = "getting";
		int attempt = 1;

		OperationResult subResult = result.createSubresult(GET_OBJECT);
		subResult.addParam("type", type.getName());
		subResult.addParam("oid", oid);

		while (true) {
			try {
				return getObjectAttempt(type, oid, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> PrismObject<T> getObjectAttempt(Class<T> type, String oid, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[] { type.getSimpleName(), oid });

		PrismObject<T> objectType = null;

		Session session = null;
		try {
			session = beginTransaction();

			objectType = getObject(session, type, oid);

			session.getTransaction().commit();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			rollbackTransaction(session, ex, result);
			throw ex;
		} catch (Exception ex) {
			if (ex instanceof SchemaException) {
				throw (SchemaException) ex;
			}
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Get object:\n{}", new Object[] { (objectType != null ? objectType.debugDump(3) : null) });
		}

		return objectType;
	}

	@Deprecated
	@Override
	public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> type, PagingType paging,
			OperationResult result) {
		try {
			return searchObjects(type, null, paging, result);
		} catch (SchemaException ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	@Override
	public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(accountOid, "Oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		final String operation = "listing account shadow owner";
		int attempt = 1;

		OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW);
		subResult.addParam("accountOid", accountOid);

		while (true) {
			try {
				return listAccountShadowOwnerAttempt(accountOid, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(accountOid, operation, attempt, ex, subResult);
			}
		}
	}

	private PrismObject<UserType> listAccountShadowOwnerAttempt(String accountOid, OperationResult result)
			throws ObjectNotFoundException {
		UserType userType = null;
		Session session = null;
		try {
			session = beginTransaction();
			LOGGER.debug("Selecting account shadow owner for account {}.", new Object[] { accountOid });
			Query query = session.createQuery("select user from " + ClassMapper.getHQLType(UserType.class)
					+ " as user left join user.accountRefs as ref where ref.targetOid = :oid");
			query.setString("oid", accountOid);

			List<RUser> users = query.list();
			LOGGER.debug("Found {} users, transforming data to JAXB types.",
					new Object[] { (users != null ? users.size() : 0) });

			if (users == null || users.isEmpty()) {
				// account shadow owner was not found
				return null;
			}

			if (users.size() > 1) {
				LOGGER.warn("Found {} users for account oid {}, returning first user. [interface change needed]",
						new Object[] { users.size(), accountOid });
			}

			RUser user = users.get(0);
			userType = user.toJAXB(getPrismContext());

			session.getTransaction().commit();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (Exception ex) {
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		return userType.asPrismObject();
	}

	private void validateName(PrismObject object) throws SchemaException {
		PrismProperty name = object.findProperty(ObjectType.F_NAME);
		if (name == null || StringUtils.isEmpty((String) name.getRealValue())) {
			throw new SchemaException("Attempt to add object without name.");
		}
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException {
		Validate.notNull(object, "Object must not be null.");
		validateName(object);
		Validate.notNull(result, "Operation result must not be null.");

		OperationResult subResult = result.createSubresult(ADD_OBJECT);
		subResult.addParam("object", object);

		final String operation = "adding";
		int attempt = 1;

		String oid = object.getOid();
		while (true) {
			try {
				return addObjectAttempt(object, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> String addObjectAttempt(PrismObject<T> object, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException {
		LOGGER.debug("Adding object type '{}'", new Object[] { object.getCompileTimeClass().getSimpleName() });

		String oid = null;
		Session session = null;
		try {
			ObjectType objectType = object.asObjectable();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Object\n{}", new Object[] { getPrismContext().silentMarshalObject(objectType) });
			}

			// check name uniqueness (by type)
			session = beginTransaction();
			if (StringUtils.isNotEmpty(objectType.getOid())) {
				LOGGER.debug("Checking oid uniqueness.");
				Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(object.getCompileTimeClass()));
				criteria.add(Restrictions.eq("oid", object.getOid()));
				criteria.setProjection(Projections.rowCount());

				Long count = (Long) criteria.uniqueResult();
				if (count != null && count > 0) {
					throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
							+ "' with oid '" + object.getOid() + "' already exists.");
				}
			}

			LOGGER.debug("Translating JAXB to data type.");
			RObject rObject = createDataObjectFromJAXB(objectType);

			LOGGER.debug("Saving object.");
			RContainerId containerId = (RContainerId) session.save(rObject);
			oid = containerId.getOid();

			if (objectType instanceof OrgType || !objectType.getOrgRef().isEmpty()) {
				objectType.setOid(oid);
				fillHierarchy(objectType, session);
			}

			session.getTransaction().commit();

			LOGGER.debug("Saved object '{}' with oid '{}'", new Object[] {
					object.getCompileTimeClass().getSimpleName(), oid });
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			rollbackTransaction(session, ex, result);
			throw ex;
		} catch (ConstraintViolationException ex) {
			rollbackTransaction(session, ex, result);
			// we don't know if it's only name uniqueness violation, or
			// something else,
			// therefore we're throwing it always as
			// ObjectAlreadyExistsException
			throw new ObjectAlreadyExistsException(ex);
		} catch (Exception ex) {
			if (ex instanceof SchemaException) {
				throw (SchemaException) ex;
			}
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		return oid;
	}

	private <T extends ObjectType> void fillHierarchy(T orgType, Session session) throws SchemaException {

		int depth = 0;

		RObject rOrg = createDataObjectFromJAXB(orgType);

		ROrgClosure closure = new ROrgClosure(rOrg, rOrg, depth);

		// if (checkClosureUniqueness(closure, session)) {
		session.save(closure);
		// }

		for (ObjectReferenceType orgRef : orgType.getOrgRef()) {
			fillTransitiveHierarchy(rOrg, orgRef.getOid(), session);
		}

	}

	private <T extends ObjectType> void fillTransitiveHierarchy(RObject newDescendant, String descendantOid,
			Session session) throws SchemaException {

		Criteria query = session.createCriteria(ROrgClosure.class).createCriteria("descendant", "desc")
				.setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", descendantOid));

		// query.
		List<ROrgClosure> results = query.list();
		for (ROrgClosure o : results) {
			session.save(new ROrgClosure(o.getAncestor(), newDescendant, o.getDepth()+1));
		}

	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notNull(type, "Object type must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		final String operation = "deleting";
		int attempt = 1;

		OperationResult subResult = result.createSubresult(DELETE_OBJECT);
		subResult.addParam("type", type.getName());
		subResult.addParam("oid", oid);

		while (true) {
			try {
				deleteObjectAttempt(type, oid, subResult);
				return;
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
			throws ObjectNotFoundException {
		LOGGER.debug("Deleting object type '{}' with oid '{}'", new Object[] { type.getSimpleName(), oid });

		Session session = null;
		try {
			session = beginTransaction();
			
			Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
			query.add(Restrictions.eq("oid", oid));
			query.add(Restrictions.eq("id", 0L));
			RObject object = (RObject) query.uniqueResult();
			if (object == null) {
				throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
						+ "' was not found.", null, oid);
			}
			
			List<RObject> objectsToRecompute = null;
			if (type.isAssignableFrom(OrgType.class)) {
				objectsToRecompute = deleteTransitiveHierarchy(object, session);
			}
			LOGGER.trace("objectsTORecompute.");
			for (RObject obj : objectsToRecompute){
				LOGGER.trace(ObjectTypeUtil.toShortString(obj.toJAXB(getPrismContext())));
			}
			
			session.delete(object);

			if (objectsToRecompute != null){
				recompute(objectsToRecompute, session);
			}
			
			session.getTransaction().commit();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			rollbackTransaction(session, ex, result);
			throw ex;
		} catch (Exception ex) {
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}
	}

	private void recompute(List<RObject> objectsToRecompute, Session session) throws SchemaException, DtoTranslationException{
		
		LOGGER.trace("Recomputing organization structure closure table after delete.");
		
		for (RObject object : objectsToRecompute) {
			Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(object.toJAXB(getPrismContext()).getClass()));
			query.add(Restrictions.eq("oid", object.getOid()));
			query.add(Restrictions.eq("id", 0L));
			RObject obj = (RObject) query.uniqueResult();
			if (obj == null) {
				// object not found..probably it was just deleted.
				continue;
			}
			deleteAncestors(object, session);
			fillHierarchy(object.toJAXB(getPrismContext()), session);
		}
		LOGGER.trace("Closure table for organization structure recomputed.");
	}
	
	private void deleteAncestors(RObject object, Session session){
		Criteria criteria = session.createCriteria(ROrgClosure.class);
		criteria.add(Restrictions.eq("descendant", object));
		List<ROrgClosure> objectsToDelete = criteria.list();
		
		for (ROrgClosure objectToDelete : objectsToDelete){
			session.delete(objectToDelete);
		}
		
	}
	
	@Deprecated
	@Override
	public void claimTask(String oid, OperationResult result) throws ObjectNotFoundException, ConcurrencyException,
			SchemaException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		OperationResult subResult = result.createSubresult(CLAIM_TASK);
		updateTaskExclusivity(oid, TaskExclusivityStatusType.CLAIMED, subResult);
	}

	@Deprecated
	@Override
	public void releaseTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		OperationResult subResult = result.createSubresult(RELEASE_TASK);
		updateTaskExclusivity(oid, TaskExclusivityStatusType.RELEASED, subResult);
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, QueryType query, OperationResult result) {
		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		LOGGER.debug("Counting objects of type '{}', query (on trace level).", new Object[] { type });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Full query\n{}", new Object[] { (query == null ? "undefined" : getPrismContext()
					.silentMarshalObject(query)) });
		}

		OperationResult subResult = result.createSubresult(COUNT_OBJECTS);
		subResult.addParam("type", type.getName());
		subResult.addParam("query", query);

		final String operation = "counting";
		int attempt = 1;

		while (true) {
			try {
				return countObjectsAttempt(type, query, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> int countObjectsAttempt(Class<T> type, QueryType query, OperationResult result) {
		int count = 0;

		Session session = null;
		try {
			session = beginTransaction();
			LOGGER.debug("Updating query criteria.");
			Criteria criteria;
			if (query != null && query.getFilter() != null) {
				QueryInterpreter interpreter = new QueryInterpreter(session, type, getPrismContext());
				criteria = interpreter.interpret(query.getFilter());
			} else {
				criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
			}
			criteria.setProjection(Projections.rowCount());

			LOGGER.debug("Selecting total count.");
			Long longCount = (Long) criteria.uniqueResult();
			count = longCount.intValue();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (Exception ex) {
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		return count;
	}

	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, QueryType query, PagingType paging,
			OperationResult result) throws SchemaException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}.", new Object[] {
				type.getSimpleName(), (paging == null ? "undefined" : paging.getOffset()),
				(paging == null ? "undefined" : paging.getMaxSize()) });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[] {
					(query == null ? "undefined" : getPrismContext().silentMarshalObject(query)),
					(paging == null ? "undefined" : getPrismContext().silentMarshalObject(paging)) });
		}

		OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);
		subResult.addParam("type", type.getName());
		subResult.addParam("query", query);
		subResult.addParam("paging", paging);

		final String operation = "searching";
		int attempt = 1;
		while (true) {
			try {
				return searchObjectsAttempt(type, query, paging, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> List<PrismObject<T>> searchObjectsAttempt(Class<T> type, QueryType query,
			PagingType paging, OperationResult result) throws SchemaException {

		List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
		Session session = null;
		try {
			session = beginTransaction();
			LOGGER.debug("Updating query criteria.");
			Criteria criteria;
			if (query != null && query.getFilter() != null) {
				QueryInterpreter interpreter = new QueryInterpreter(session, type, getPrismContext());
				criteria = interpreter.interpret(query.getFilter());
			} else {
				criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
			}

			criteria = updatePagingAndSorting(criteria, type, paging);

			List<RObject> objects = criteria.list();
			LOGGER.debug("Found {} objects, translating to JAXB.",
					new Object[] { (objects != null ? objects.size() : 0) });

			for (RObject object : objects) {
				ObjectType objectType = object.toJAXB(getPrismContext());
				PrismObject<T> prismObject = objectType.asPrismObject();
				validateObjectType(prismObject, type);
				list.add(prismObject);
			}

			session.getTransaction().commit();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (Exception ex) {
			if (ex instanceof SchemaException) {
				throw (SchemaException) ex;
			}
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		return list;
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ObjectAlreadyExistsException {
		Validate.notNull(modifications, "Modifications must not be null.");
		Validate.notNull(type, "Object class in delta must not be null.");
		Validate.notEmpty(oid, "Oid must not null or empty.");
		Validate.notNull(result, "Operation result must not be null.");

		OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
		subResult.addParam("type", type.getName());
		subResult.addParam("oid", oid);
		subResult.addParam("modifications", modifications);

		if (modifications.isEmpty()) {
			subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
			return;
		}

		final String operation = "modifying";
		int attempt = 1;

		while (true) {
			try {
				modifyObjectAttempt(type, oid, modifications, subResult);
				return;
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ObjectType> void modifyObjectAttempt(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ObjectAlreadyExistsException {
		LOGGER.debug("Modifying object '{}' with oid '{}'.", new Object[] { type.getSimpleName(), oid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifications: {}", new Object[] { DebugUtil.prettyPrint(modifications) });
		}

		Session session = null;
		try {
			session = beginTransaction();

			// get user
			PrismObject<T> prismObject = getObject(session, type, oid);
			// apply diff
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("OBJECT before:\n{}", new Object[] { prismObject.dump() });
			}
			PropertyDelta.applyTo(modifications, prismObject);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("OBJECT after:\n{}", prismObject.dump());
			}
			// merge and update user
			LOGGER.debug("Translating JAXB to data type.");
			RObject rObject = createDataObjectFromJAXB(prismObject.asObjectable());
			rObject.setVersion(rObject.getVersion() + 1);

			session.merge(rObject);

			recomputeHierarchy(prismObject.asObjectable(), session, modifications);

			session.getTransaction().commit();
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			rollbackTransaction(session, ex, result);
			throw ex;
		} catch (ConstraintViolationException ex) {
			rollbackTransaction(session, ex, result);
			// we don't know if it's only name uniqueness violation, or
			// something else,
			// therefore we're throwing it always as
			// ObjectAlreadyExistsException
			throw new ObjectAlreadyExistsException(ex);
		} catch (Exception ex) {
			if (ex instanceof SchemaException) {
				throw (SchemaException) ex;
			}
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

	}

	private <T extends ObjectType> void recomputeHierarchy(T orgType, Session session,
			Collection<? extends ItemDelta> modifications) throws SchemaException, DtoTranslationException {

		for (ItemDelta delta : modifications) {
			if (delta.getName().equals(OrgType.F_ORG_REF)) {
				// if modifiction is one of the modify or delete, delete old
				// record in org closure table and in the next step fill the
				// closure table with the new records
				if (delta.isReplace() || delta.isDelete()) {
					for (Object orgRefDValue : delta.getValuesToDelete()) {
						if (!(orgRefDValue instanceof PrismReferenceValue)){
							throw new SchemaException(
									"Couldn't modify organization structure hierarchy (adding new records). Expected instance of prism reference value but got "
											+ orgRefDValue);
						}
						
						PrismReferenceValue value = (PrismReferenceValue) orgRefDValue;
						RObject rObjectToModify = createDataObjectFromJAXB(orgType);
						List<RObject> objectsToRecompute = deleteTransitiveHierarchy(rObjectToModify, session);
						refillHierarchy(rObjectToModify, objectsToRecompute, session);
					}
//					List<RObject> objectsToRecompute = deleteFromHierarchy(createDataObjectFromJAXB(orgType), session);
//					recompute(objectsToRecompute, session);
//					fillHierarchy(orgType, session);
				} else{
					//fill closure table with new transitive relations
					for (Object orgRefDValue : delta.getValuesToAdd()) {
						if (!(orgRefDValue instanceof PrismReferenceValue)){
							throw new SchemaException(
									"Couldn't modify organization structure hierarchy (adding new records). Expected instance of prism reference value but got "
											+ orgRefDValue);
						}
						
						PrismReferenceValue value = (PrismReferenceValue) orgRefDValue;
						
						RObject rDescendant = createDataObjectFromJAXB(orgType);
						fillTransitiveHierarchy(rDescendant, value.getOid(), session);
					}
				}
			}
		}
	}

	private List<RObject> deleteTransitiveHierarchy(RObject rObjectToModify, Session session) throws SchemaException, DtoTranslationException {
		
		
		List<RObject> descendants = session.createCriteria(ROrgClosure.class)
				.setProjection(Projections.property("descendant")).add(Restrictions.eq("ancestor", rObjectToModify))
				.list();
		
		List<RObject> ancestors = session
				.createCriteria(ROrgClosure.class)
				.setProjection(Projections.property("ancestor"))
				.createCriteria("ancestor", "anc")
				.add(Restrictions.and(Restrictions.eq("this.descendant", rObjectToModify),
						Restrictions.not(Restrictions.eq("anc.oid", rObjectToModify.getOid())))).list();

		Criteria criteria = session.createCriteria(ROrgClosure.class);
		criteria.add(Restrictions.in("ancestor",ancestors));
		criteria.add(Restrictions.in("descendant", descendants));

		List<ROrgClosure> orgClosure = criteria.list();
		for (ROrgClosure o : orgClosure) {
			LOGGER.trace("deleting from hierarchy: A: {} D:{} depth:{}",
					new Object[] { o.getAncestor().toJAXB(getPrismContext()), o.getDescendant().toJAXB(getPrismContext()), o.getDepth() });
			session.delete(o);
		}
		
		deleteHierarchy(rObjectToModify, session);
		return descendants;
	}
	
	private void refillHierarchy(RObject parent, List<RObject> descendants, Session session) throws SchemaException, DtoTranslationException{
		fillHierarchy(parent.toJAXB(getPrismContext()), session);
		
		for (RObject descentant : descendants) {
			if (!parent.getOid().equals(descentant.getOid())) {
				fillTransitiveHierarchy(descentant, parent.getOid(), session);
			}
		}
		
	}
	
	private void deleteHierarchy(RObject objectToDelete, Session session){
		Criteria criteria = session.createCriteria(ROrgClosure.class).add(
				Restrictions.or(Restrictions.eq("ancestor", objectToDelete), Restrictions.eq("descendant", objectToDelete)));
		
		List<ROrgClosure> orgClosure = criteria.list();
		for (ROrgClosure o : orgClosure) {
			LOGGER.trace("deleting from hierarchy: A: {} D:{} depth:{}",
					new Object[] { o.getAncestor(), o.getDescendant(), o.getDepth() });
			session.delete(o);
		}
		
	}

	private List<RObject> deleteFromHierarchy(RObject object, Session session) throws SchemaException, DtoTranslationException {
		
		LOGGER.trace("Deleting records from organization closure table.");
		
		Criteria criteria = session.createCriteria(ROrgClosure.class);
		List<RObject> descendants = criteria.setProjection(Projections.property("descendant")).add(Restrictions.eq("ancestor", object)).list();
		
		for (RObject desc : descendants) {
			List<ROrgClosure> orgClosure = session.createCriteria(ROrgClosure.class)
					.add(Restrictions.eq("descendant", desc)).list();
			for (ROrgClosure o : orgClosure) {
				session.delete(o);
			}
//			fillHierarchy(desc.toJAXB(getPrismContext()), session);
		}

		criteria = session.createCriteria(ROrgClosure.class).add(
				Restrictions.or(Restrictions.eq("ancestor", object), Restrictions.eq("descendant", object)));

		List<ROrgClosure> orgClosure = criteria.list();
		for (ROrgClosure o : orgClosure) {
			LOGGER.trace("deleting from hierarchy: A: {} D:{} depth:{}",
					new Object[] { o.getAncestor(), o.getDescendant(), o.getDepth() });
			session.delete(o);
		}
		return descendants;
	}

	@Override
	public <T extends ResourceObjectShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
				new Object[] { resourceObjectShadowType.getSimpleName(), resourceOid });
		OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
		subResult.addParam("oid", resourceOid);
		subResult.addParam("resourceObjectShadowType", resourceObjectShadowType);

		final String operation = "listing resource object shadows";
		int attempt = 1;
		while (true) {
			try {
				return listResourceObjectShadowsAttempt(resourceOid, resourceObjectShadowType, subResult);
			} catch (RuntimeException ex) {
				attempt = logOperationAttempt(resourceOid, operation, attempt, ex, subResult);
			}
		}
	}

	private <T extends ResourceObjectShadowType> List<PrismObject<T>> listResourceObjectShadowsAttempt(
			String resourceOid, Class<T> resourceObjectShadowType, OperationResult result)
			throws ObjectNotFoundException {

		List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
		Session session = null;
		try {
			session = beginTransaction();
			Query query = session.createQuery("select shadow from " + ClassMapper.getHQLType(resourceObjectShadowType)
					+ " as shadow left join shadow.resourceRef as ref where ref.oid = :oid");
			query.setString("oid", resourceOid);

			List<RResourceObjectShadow> shadows = query.list();
			LOGGER.debug("Query returned {} shadows, transforming to JAXB types.",
					new Object[] { (shadows != null ? shadows.size() : 0) });

			if (shadows != null) {
				for (RResourceObjectShadow shadow : shadows) {
					ResourceObjectShadowType jaxb = shadow.toJAXB(getPrismContext());
					PrismObject<T> prismObject = jaxb.asPrismObject();
					validateObjectType(prismObject, resourceObjectShadowType);

					list.add(prismObject);
				}
			}
			session.getTransaction().commit();
			LOGGER.debug("Done.");
		} catch (PessimisticLockException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (LockAcquisitionException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session);
			throw ex;
		} catch (Exception ex) {
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}

		return list;
	}

	@Deprecated
	private void updateTaskExclusivity(String oid, TaskExclusivityStatusType newStatus, OperationResult result)
			throws ObjectNotFoundException {

		LOGGER.debug("Updating task '{}' exclusivity to '{}'", new Object[] { oid, newStatus });
		Session session = null;
		try {
			LOGGER.debug("Looking for task.");
			session = beginTransaction();
			Query query = session.createQuery("from " + ClassMapper.getHQLType(TaskType.class)
					+ " as task where task.oid = :oid and task.id = 0");
			query.setString("oid", oid);

			RTask task = (RTask) query.uniqueResult();
			if (task == null) {
				throw new ObjectNotFoundException("Task with oid '" + oid + "' was not found.");
			}
			LOGGER.debug("Task found, updating exclusivity status.");
			task.setExclusivityStatus(RTaskExclusivityStatusType.toRepoType(newStatus));
			session.save(task);

			session.getTransaction().commit();
			LOGGER.debug("Task status updated.");
		} catch (HibernateOptimisticLockingFailureException ex) {
			rollbackTransaction(session, ex, result);
			throw new SystemException(ex.getMessage(), ex);
		} catch (ObjectNotFoundException ex) {
			rollbackTransaction(session, ex, result);
			throw ex;
		} catch (Exception ex) {
			handleGeneralException(ex, session, result);
		} finally {
			cleanupSessionAndResult(session, result);
		}
	}

	private <T extends ObjectType> void validateObjectType(PrismObject<T> prismObject, Class<T> type) {
		if (prismObject == null || !type.isAssignableFrom(prismObject.getCompileTimeClass())) {
			throw new SystemException("Result ('" + prismObject.toDebugName() + "') is not assignable to '"
					+ type.getSimpleName() + "' [really should not happen].");
		}
	}

	private <T extends ObjectType> RObject createDataObjectFromJAXB(T object) throws SchemaException {

		RObject rObject;
		Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
		try {
			rObject = clazz.newInstance();
			Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz, PrismContext.class);
			method.invoke(clazz, object, rObject, getPrismContext());
		} catch (Exception ex) {
			String message = ex.getMessage();
			if (StringUtils.isEmpty(message) && ex.getCause() != null) {
				message = ex.getCause().getMessage();
			}
			throw new SchemaException(message, ex);
		}

		return rObject;
	}

	private <T extends ObjectType> Criteria updatePagingAndSorting(Criteria query, Class<T> type, PagingType paging) {
		if (paging == null) {
			return query;
		}
		if (paging.getOffset() != null) {
			query = query.setFirstResult(paging.getOffset());
		}
		if (paging.getMaxSize() != null) {
			query = query.setMaxResults(paging.getMaxSize());
		}

		if (paging.getOrderDirection() == null && paging.getOrderBy() == null) {
			return query;
		}

		try {
			QueryRegistry registry = QueryRegistry.getInstance();
			PropertyPath path = new XPathHolder(paging.getOrderBy()).toPropertyPath();
			if (path == null || path.size() != 1) {
				LOGGER.warn("Ordering by property path with size not equal 1 is not supported '" + path + "'.");
				return query;
			}
			EntityDefinition definition = registry.findDefinition(ObjectTypes.getObjectType(type).getQName());
			Definition def = definition.findDefinition(path.first().getName());
			if (def == null) {
				LOGGER.warn("Unknown path '" + path + "', couldn't find definition for it, "
						+ "list will not be ordered by it.");
				return query;
			}

			switch (paging.getOrderDirection()) {
			case ASCENDING:
				query = query.addOrder(Order.asc(def.getRealName()));
				break;
			case DESCENDING:
				query = query.addOrder(Order.desc(def.getRealName()));
			}
		} catch (QueryException ex) {
			throw new SystemException(ex.getMessage(), ex);
		}

		return query;
	}
}

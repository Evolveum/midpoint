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
package com.evolveum.midpoint.repo.sql;

import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.sql.data.audit.*;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

	@Autowired
	private BaseHelper baseHelper;

	private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);
	private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;
	
	private static final String QUERY_MAX_RESULT = "setMaxResults"; 
	private static final String QUERY_FIRST_RESULT = "setFirstResult";

	public SqlAuditServiceImpl(SqlRepositoryFactory repositoryFactory) {
		super(repositoryFactory);
	}

	@Override
	public void audit(AuditEventRecord record, Task task) {
		Validate.notNull(record, "Audit event record must not be null.");
		Validate.notNull(task, "Task must not be null.");

		final String operation = "audit";
		int attempt = 1;

		while (true) {
			try {
				auditAttempt(record);
				return;
			} catch (RuntimeException ex) {
				attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
			}
		}
	}

	@Override
	public List<AuditEventRecord> listRecords(String query, Map<String, Object> params) {
		final String operation = "listRecords";
		int attempt = 1;

		while (true) {
			try {
				final List<AuditEventRecord> auditEventRecords = new ArrayList<>();

				AuditResultHandler handler = new AuditResultHandler() {

					@Override
					public boolean handle(AuditEventRecord auditRecord) {
						auditEventRecords.add(auditRecord);
						return true;
					}

					@Override
					public int getProgress() {
						return 0;
					}
				};
				listRecordsIterativeAttempt(query, params, handler);
				return auditEventRecords;
			} catch (RuntimeException ex) {
				attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
			}
		}
	}

	@Override
	public void listRecordsIterative(String query, Map<String, Object> params, AuditResultHandler handler) {
		final String operation = "listRecordsIterative";
		int attempt = 1;

		while (true) {
			try {
				listRecordsIterativeAttempt(query, params, handler);
				return;
			} catch (RuntimeException ex) {
				attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
			}
		}
	}

	@Override
	public void reindexEntry(AuditEventRecord record) {
		final String operation = "reindexEntry";
		int attempt = 1;

		while (true) {
			try {
				reindexEntryAttempt(record);
				return;
			} catch (RuntimeException ex) {
				attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
			}
		}
	}

	private void reindexEntryAttempt(AuditEventRecord record) {
		Session session = null;
		try {
			session = baseHelper.beginTransaction();
			
			RAuditEventRecord reindexed = RAuditEventRecord.toRepo(record, getPrismContext());
			//TODO FIXME temporary hack, merge will eventyually load the object to the session if there isn't one,
			// but in this case we force loading object because of "objectDeltaOperation". There is some problem probably
			// during serializing/deserializing which causes constraint violation on priamry key..
			Object o = session.load(RAuditEventRecord.class, record.getRepoId());
			
			if (o instanceof RAuditEventRecord) {
				RAuditEventRecord rRecord = (RAuditEventRecord) o;
				rRecord.getChangedItems().clear();
				rRecord.getChangedItems().addAll(reindexed.getChangedItems());
				
				session.merge(rRecord);
			}
			
			session.getTransaction().commit();

		} catch (DtoTranslationException ex) {
			baseHelper.handleGeneralCheckedException(ex, session, null);
		} catch (RuntimeException ex) {
			baseHelper.handleGeneralRuntimeException(ex, session, null);
		} finally {
			baseHelper.cleanupSessionAndResult(session, null);
		}

	}

	private void listRecordsIterativeAttempt(String query, Map<String, Object> params,
			AuditResultHandler handler) {
		Session session = null;
		int count = 0;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("List records attempt\n  query: {}\n params:\n{}", query,
					DebugUtil.debugDump(params, 2));
		}

		try {
			session = baseHelper.beginReadOnlyTransaction();
			
			Query q;
			
			if (StringUtils.isBlank(query)) {
				query = "from RAuditEventRecord as aer where 1=1 order by aer.timestamp desc";
				q = session.createQuery(query);
				setParametersToQuery(q, params);
			} else {
				q = session.createQuery(query);
				setParametersToQuery(q, params);
			}
			// q.setResultTransformer(Transformers.aliasToBean(RAuditEventRecord.class));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("List records attempt\n  processed query: {}", q);
			}
			
			ScrollableResults resultList = q.scroll();

			while (resultList.next()) {
				Object o = resultList.get(0);
				if (!(o instanceof RAuditEventRecord)) {
					throw new DtoTranslationException(
							"Unexpected object in result set. Expected audit record, but got "
									+ o.getClass().getSimpleName());
				}
				RAuditEventRecord raudit = (RAuditEventRecord) o;

				AuditEventRecord audit = RAuditEventRecord.fromRepo(raudit, getPrismContext());

				// TODO what if original name (in audit log) differs from the current one (in repo) ?
				audit.setInitiator(resolve(session, raudit.getInitiatorOid(), raudit.getInitiatorName(), RObjectType.USER));
				audit.setTarget(resolve(session, raudit.getTargetOid(), raudit.getTargetName(), raudit.getTargetType()));
				audit.setTargetOwner(resolve(session, raudit.getTargetOwnerOid(), raudit.getTargetOwnerName(), RObjectType.USER));
				count++;
				if (!handler.handle(audit)) {
					LOGGER.trace("Skipping handling of objects after {} was handled. ", audit);
					break;
				}
			}

			session.getTransaction().commit();

		} catch (DtoTranslationException | SchemaException ex) {
			baseHelper.handleGeneralCheckedException(ex, session, null);
		} catch (RuntimeException ex) {
			baseHelper.handleGeneralRuntimeException(ex, session, null);
		} finally {
			baseHelper.cleanupSessionAndResult(session, null);
		}

		LOGGER.trace("List records iterative attempt processed {} records", count);
	}
	
	private void setParametersToQuery(Query q, Map<String, Object> params) {
		if (params == null) {
			return;
		}

		if (params.containsKey("setFirstResult")) {
			q.setFirstResult((int) params.get("setFirstResult"));
			params.remove("setFirstResult");
		}
		if (params.containsKey("setMaxResults")) {
			q.setMaxResults((int) params.get("setMaxResults"));
			params.remove("setMaxResults");
		}
		Set<Entry<String, Object>> paramSet = params.entrySet();
		for (Entry<String, Object> p : paramSet) {
			if (p.getValue() == null) {
				q.setParameter(p.getKey(), null);
				continue;
			}
			
			if (List.class.isAssignableFrom(p.getValue().getClass())){
				q.setParameterList(p.getKey(), convertValues((List)p.getValue()));
			} else { 
				q.setParameter(p.getKey(), toRepoType(p.getValue()));
			}
//			if (XMLGregorianCalendar.class.isAssignableFrom(p.getValue().getClass())) {
//				q.setParameter(p.getKey(), MiscUtil.asDate((XMLGregorianCalendar) p.getValue()));
//			} else if (p.getValue() instanceof AuditEventType) {
//				q.setParameter(p.getKey(), RAuditEventType.toRepo((AuditEventType) p.getValue()));
//			} else if (p.getValue() instanceof AuditEventStage) {
//				q.setParameter(p.getKey(), RAuditEventStage.toRepo((AuditEventStage) p.getValue()));
//			} else {
//				q.setParameter(p.getKey(), p.getValue());
//			}
		}
	}
	
	private List<?> convertValues(List<?> originValues) {
		List<Object> repoValues = new ArrayList<>();
		for (Object value : originValues) {
			repoValues.add(toRepoType(value));
		}
		return repoValues;
	}
	
	private Object toRepoType(Object value){
		if (XMLGregorianCalendar.class.isAssignableFrom(value.getClass())) {
			return MiscUtil.asDate((XMLGregorianCalendar) value);
		} else if (value instanceof AuditEventType) {
			return RAuditEventType.toRepo((AuditEventType) value);
		} else if (value instanceof AuditEventStage) {
			return RAuditEventStage.toRepo((AuditEventStage) value);
		} 
		
		return value;
	}

	private PrismObject resolve(Session session, String oid, String defaultName, RObjectType defaultType) throws SchemaException {
		if (oid == null) {
			return null;
		}
		Query query = session.getNamedQuery("get.object");
		query.setParameter("oid", oid);
		query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());
		GetObjectResult object = (GetObjectResult) query.uniqueResult();

		PrismObject result;
		if (object != null) {
			String xml = RUtil.getXmlFromByteArray(object.getFullObject(), getConfiguration().isUseZip());
			result = getPrismContext().parserFor(xml).compat().parse();
		} else if (defaultType != null) {
			result = getPrismContext().createObject(defaultType.getJaxbClass());
			result.asObjectable().setName(PolyStringType.fromOrig(defaultName != null ? defaultName : oid));
			result.setOid(oid);
		} else {
			result = null;
		}
		return result;
	}

	private void auditAttempt(AuditEventRecord record) {
		Session session = null;
		try {
			session = baseHelper.beginTransaction();

			RAuditEventRecord newRecord = RAuditEventRecord.toRepo(record, getPrismContext());
			session.save(newRecord);

			session.getTransaction().commit();
		} catch (DtoTranslationException ex) {
			baseHelper.handleGeneralCheckedException(ex, session, null);
		} catch (RuntimeException ex) {
			baseHelper.handleGeneralRuntimeException(ex, session, null);
		} finally {
			baseHelper.cleanupSessionAndResult(session, null);
		}
	}

	@Override
	public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
		Validate.notNull(policy, "Cleanup policy must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		cleanupAuditMaxRecords(policy, parentResult);
		cleanupAuditMaxAge(policy, parentResult);
	}

	private void cleanupAuditMaxAge(CleanupPolicyType policy, OperationResult parentResult) {

		final String operation = "deletingMaxAge";

		SqlPerformanceMonitor pm = getPerformanceMonitor();
		long opHandle = pm.registerOperationStart("cleanupAuditMaxAge");
		int attempt = 1;

		if (policy.getMaxAge() == null) {
			return;
		}

		Duration duration = policy.getMaxAge();
		if (duration.getSign() > 0) {
			duration = duration.negate();
		}
		Date minValue = new Date();
		duration.addTo(minValue);

		// factored out because it produces INFO-level message
		Dialect dialect = Dialect.getDialect(baseHelper.getSessionFactoryBean().getHibernateProperties());
		checkTemporaryTablesSupport(dialect);

		long start = System.currentTimeMillis();
		boolean first = true;
		Holder<Integer> totalCountHolder = new Holder<>(0);
		try {
			while (true) {
				try {
					LOGGER.info("{} audit cleanup, deleting up to {} (duration '{}'), batch size {}{}.",
							first ? "Starting" : "Continuing with ", minValue, duration, CLEANUP_AUDIT_BATCH_SIZE,
							first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
					first = false;
					int count;
					do {
						// the following method may restart due to concurrency
						// (or any other) problem - in any iteration
						long batchStart = System.currentTimeMillis();
						LOGGER.debug(
								"Starting audit cleanup batch, deleting up to {} (duration '{}'), batch size {}, up to now deleted {} entries.",
								minValue, duration, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());
						count = batchDeletionAttempt((session, tempTable) -> selectRecordsByMaxAge(session, tempTable, minValue, dialect),
								totalCountHolder, batchStart, dialect, parentResult);
					} while (count > 0);
					return;
				} catch (RuntimeException ex) {
					attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, parentResult);
					pm.registerOperationNewTrial(opHandle, attempt);
				}
			}
		} finally {
			pm.registerOperationFinish(opHandle, attempt);
			LOGGER.info("Audit cleanup based on age finished; deleted {} entries in {} seconds.",
					totalCountHolder.getValue(), (System.currentTimeMillis() - start) / 1000L);
		}
	}

	private void cleanupAuditMaxRecords(CleanupPolicyType policy, OperationResult parentResult) {

		final String operation = "deletingMaxRecords";

		SqlPerformanceMonitor pm = getPerformanceMonitor();
		long opHandle = pm.registerOperationStart("cleanupAuditMaxRecords");
		int attempt = 1;

		if (policy.getMaxRecords() == null) {
			return;
		}

		Integer recordsToKeep = policy.getMaxRecords();

		// factored out because it produces INFO-level message
		Dialect dialect = Dialect.getDialect(baseHelper.getSessionFactoryBean().getHibernateProperties());
		checkTemporaryTablesSupport(dialect);

		long start = System.currentTimeMillis();
		boolean first = true;
		Holder<Integer> totalCountHolder = new Holder<>(0);
		try {
			while (true) {
				try {
					LOGGER.info("{} audit cleanup, keeping at most {} records, batch size {}{}.",
							first ? "Starting" : "Continuing with ", recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE,
							first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
					first = false;
					int count;
					do {
						// the following method may restart due to concurrency
						// (or any other) problem - in any iteration
						long batchStart = System.currentTimeMillis();
						LOGGER.debug(
								"Starting audit cleanup batch, keeping at most {} records, batch size {}, up to now deleted {} entries.",
								recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());
						count = batchDeletionAttempt((session, tempTable) -> selectRecordsByNumberToKeep(session, tempTable, recordsToKeep, dialect),
								totalCountHolder, batchStart, dialect, parentResult);
					} while (count > 0);
					return;
				} catch (RuntimeException ex) {
					attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, parentResult);
					pm.registerOperationNewTrial(opHandle, attempt);
				}
			}
		} finally {
			pm.registerOperationFinish(opHandle, attempt);
			LOGGER.info("Audit cleanup based on record count finished; deleted {} entries in {} seconds.",
					totalCountHolder.getValue(), (System.currentTimeMillis() - start) / 1000L);
		}
	}

	private void checkTemporaryTablesSupport(Dialect dialect) {
		if (!dialect.supportsTemporaryTables()) {
			LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup audit logs.",
					dialect);
			throw new SystemException(
					"Dialect " + dialect + " doesn't support temporary tables, couldn't cleanup audit logs.");
		}
	}

	// deletes one batch of records (using recordsSelector to select records according to particular cleanup policy)
	private int batchDeletionAttempt(BiFunction<Session, String, Integer> recordsSelector, Holder<Integer> totalCountHolder,
			long batchStart, Dialect dialect, OperationResult subResult) {

		Session session = null;
		try {
			session = baseHelper.beginTransaction();

			// create temporary table
			final String tempTable = dialect.generateTemporaryTableName(RAuditEventRecord.TABLE_NAME);
			createTemporaryTable(session, dialect, tempTable);
			LOGGER.trace("Created temporary table '{}'.", tempTable);

			int count = recordsSelector.apply(session, tempTable);
			LOGGER.trace("Inserted {} audit record ids ready for deleting.", count);

			// drop records from m_audit_item, m_audit_event, m_audit_delta, and others
			session.createSQLQuery(createDeleteQuery(RAuditItem.TABLE_NAME, tempTable,
					RAuditItem.COLUMN_RECORD_ID)).executeUpdate();
			session.createSQLQuery(createDeleteQuery(RObjectDeltaOperation.TABLE_NAME, tempTable,
					RObjectDeltaOperation.COLUMN_RECORD_ID)).executeUpdate();
			session.createSQLQuery(createDeleteQuery(RAuditPropertyValue.TABLE_NAME, tempTable,
					RAuditPropertyValue.COLUMN_RECORD_ID)).executeUpdate();
			session.createSQLQuery(createDeleteQuery(RAuditReferenceValue.TABLE_NAME, tempTable,
					RAuditReferenceValue.COLUMN_RECORD_ID)).executeUpdate();
			session.createSQLQuery(createDeleteQuery(RAuditEventRecord.TABLE_NAME, tempTable, "id"))
					.executeUpdate();

			// drop temporary table
			if (dialect.dropTemporaryTableAfterUse()) {
				LOGGER.debug("Dropping temporary table.");
				StringBuilder sb = new StringBuilder();
				sb.append(dialect.getDropTemporaryTableString());
				sb.append(' ').append(tempTable);

				session.createSQLQuery(sb.toString()).executeUpdate();
			}

			session.getTransaction().commit();
			int totalCount = totalCountHolder.getValue() + count;
			totalCountHolder.setValue(totalCount);
			LOGGER.debug("Audit cleanup batch finishing successfully in {} milliseconds; total count = {}",
					System.currentTimeMillis() - batchStart, totalCount);
			return count;
		} catch (RuntimeException ex) {
			LOGGER.debug("Audit cleanup batch finishing with exception in {} milliseconds; exception = {}",
					System.currentTimeMillis() - batchStart, ex.getMessage());
			baseHelper.handleGeneralRuntimeException(ex, session, subResult);
			throw new AssertionError("We shouldn't get here.");
		} finally {
			baseHelper.cleanupSessionAndResult(session, subResult);
		}
	}

	private int selectRecordsByMaxAge(Session session, String tempTable, Date minValue, Dialect dialect) {

		// fill temporary table, we don't need to join task on object on
		// container, oid and id is already in task table
		StringBuilder selectSB = new StringBuilder();
		selectSB.append("select a.id as id from ").append(RAuditEventRecord.TABLE_NAME).append(" a");
		selectSB.append(" where a.").append(RAuditEventRecord.COLUMN_TIMESTAMP).append(" < ###TIME###");
		String selectString = selectSB.toString();

		// batch size
		RowSelection rowSelection = new RowSelection();
		rowSelection.setMaxRows(CLEANUP_AUDIT_BATCH_SIZE);
		LimitHandler limitHandler = dialect.buildLimitHandler(selectString, rowSelection);
		selectString = limitHandler.getProcessedSql();

		// replace ? -> batch size, $ -> ?
		// Sorry for that .... I just don't know how to write this query in HQL,
		// nor I'm not sure if limiting max size in
		// compound insert into ... select ... query via query.setMaxSize()
		// would work - TODO write more nicely if anybody knows how)
		selectString = selectString.replace("?", String.valueOf(CLEANUP_AUDIT_BATCH_SIZE));
		selectString = selectString.replace("###TIME###", "?");

		String queryString = "insert into " + tempTable + " " + selectString;
		LOGGER.trace("Query string = {}", queryString);
		SQLQuery query = session.createSQLQuery(queryString);
		query.setParameter(0, new Timestamp(minValue.getTime()));

		return query.executeUpdate();
	}

	private int selectRecordsByNumberToKeep(Session session, String tempTable, Integer recordsToKeep, Dialect dialect) {
		Number totalAuditRecords = (Number) session.createCriteria(RAuditEventRecord.class)
				.setProjection(Projections.rowCount())
				.uniqueResult();
		int recordsToDelete = totalAuditRecords.intValue() - recordsToKeep;
		if (recordsToDelete <= 0) {
			recordsToDelete = 0;
		} else if (recordsToDelete > CLEANUP_AUDIT_BATCH_SIZE) {
			recordsToDelete = CLEANUP_AUDIT_BATCH_SIZE;
		}
		LOGGER.debug("Total audit records: {}, records to keep: {} => records to delete in this batch: {}",
				totalAuditRecords, recordsToKeep, recordsToDelete);
		if (recordsToDelete == 0) {
			return 0;
		}

		StringBuilder selectSB = new StringBuilder();
		selectSB.append("select a.id as id from ").append(RAuditEventRecord.TABLE_NAME).append(" a");
		selectSB.append(" order by a.").append(RAuditEventRecord.COLUMN_TIMESTAMP).append(" asc");
		String selectString = selectSB.toString();

		// batch size
		RowSelection rowSelection = new RowSelection();
		rowSelection.setMaxRows(recordsToDelete);
		LimitHandler limitHandler = dialect.buildLimitHandler(selectString, rowSelection);
		selectString = limitHandler.getProcessedSql();
		selectString = selectString.replace("?", String.valueOf(recordsToDelete));

		String queryString = "insert into " + tempTable + " " + selectString;
		LOGGER.trace("Query string = {}", queryString);
		SQLQuery query = session.createSQLQuery(queryString);
		return query.executeUpdate();
	}

	/**
	 * This method creates temporary table for cleanup audit method.
	 *
	 * @param session
	 * @param dialect
	 * @param tempTable
	 */
	private void createTemporaryTable(Session session, final Dialect dialect, final String tempTable) {
		session.doWork(connection -> {
			// check if table exists
			try {
				Statement s = connection.createStatement();
				s.execute("select id from " + tempTable + " where id = 1");
				// table already exists
				return;
			} catch (Exception ex) {
				// we expect this on the first time
			}

			StringBuilder sb = new StringBuilder();
			sb.append(dialect.getCreateTemporaryTableString());
			sb.append(' ').append(tempTable).append(" (id ");
			sb.append(dialect.getTypeName(Types.BIGINT));
			sb.append(" not null)");
			sb.append(dialect.getCreateTemporaryTablePostfix());

			Statement s = connection.createStatement();
			s.execute(sb.toString());
		});
	}

	private String createDeleteQuery(String objectTable, String tempTable, String idColumnName) {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(objectTable);
		sb.append(" where ").append(idColumnName).append(" in (select id from ").append(tempTable)
				.append(')');

		return sb.toString();
	}

	public long countObjects(String query, Map<String, Object> params) {
		Session session = null;
		long count = 0;
		try {
			session = baseHelper.beginTransaction();
			session.setFlushMode(FlushMode.MANUAL);
			if (StringUtils.isBlank(query)) {
				query  = "select count (*) from RAuditEventRecord as aer where 1 = 1";
			}
			Query q = session.createQuery(query);

			setParametersToQuery(q, params);
			Number numberCount = (Number) q.uniqueResult();
			count = numberCount != null ? numberCount.intValue() : 0;
		} catch (RuntimeException ex) {
			baseHelper.handleGeneralRuntimeException(ex, session, null);
		} finally {
			baseHelper.cleanupSessionAndResult(session, null);
		}
		return count;
	}

	@Override
	public boolean supportsRetrieval() {
		return true;
	}

}

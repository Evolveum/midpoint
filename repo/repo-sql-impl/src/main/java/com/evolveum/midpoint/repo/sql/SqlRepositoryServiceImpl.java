/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.sql.helpers.*;
import com.evolveum.midpoint.repo.sql.query2.matcher.DefaultMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.PolyStringMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.StringMatcher;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Work;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * @author lazyman
 *
 * Note: don't autowire this class - because of Spring AOP use it couldn't be found by implementation class; only by its interface.
 */
@Repository
public class SqlRepositoryServiceImpl extends SqlBaseService implements RepositoryService {

    public static final String PERFORMANCE_LOG_NAME = SqlRepositoryServiceImpl.class.getName() + ".performance";

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(PERFORMANCE_LOG_NAME);

    private static final int MAX_CONFLICT_WATCHERS = 10;          // just a safeguard (watchers per thread should be at most 1-2)
    public static final int MAX_CONSTRAINT_NAME_LENGTH = 40;
    private static final String IMPLEMENTATION_SHORT_NAME = "SQL";
    private static final String IMPLEMENTATION_DESCRIPTION = "Implementation that stores data in generic relational" +
            " (SQL) databases. It is using ORM (hibernate) on top of JDBC to access the database.";
    private static final String DETAILS_TRANSACTION_ISOLATION = "transactionIsolation";
    private static final String DETAILS_CLIENT_INFO = "clientInfo.";
    private static final String DETAILS_DATA_SOURCE = "dataSource";
    private static final String DETAILS_HIBERNATE_DIALECT = "hibernateDialect";
    private static final String DETAILS_HIBERNATE_HBM_2_DDL = "hibernateHbm2ddl";

    @Autowired private SequenceHelper sequenceHelper;
    @Autowired private ObjectRetriever objectRetriever;
    @Autowired private ObjectUpdater objectUpdater;
    @Autowired private OrgClosureManager closureManager;
    @Autowired private BaseHelper baseHelper;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private MidpointConfiguration midpointConfiguration;

    private final ThreadLocal<List<ConflictWatcherImpl>> conflictWatchersThreadLocal = new ThreadLocal<>();

    private FullTextSearchConfigurationType fullTextSearchConfiguration;

    public SqlRepositoryServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    // public because of testing
    public OrgClosureManager getClosureManager() {
        return closureManager;
    }

    @FunctionalInterface
    public interface ResultSupplier<RV> {
        RV get() throws ObjectNotFoundException, SchemaException;
    }

    @FunctionalInterface
    public interface ResultQueryBasedSupplier<RV> {
        RV get(ObjectQuery query) throws SchemaException;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        InternalMonitor.recordRepositoryRead(type, oid);

        OperationResult subResult = result.createMinorSubresult(GET_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

	    PrismObject<T> object = executeAttempts(oid, "getObject", "getting",
			    subResult, () -> objectRetriever.getObjectAttempt(type, oid, options, subResult)
	    );
	    invokeConflictWatchers((w) -> w.afterGetObject(object));
	    return object;
    }

    private <RV> RV executeAttempts(String oid, String operationName, String operationVerb, OperationResult subResult,
            ResultSupplier<RV> supplier) throws ObjectNotFoundException, SchemaException {
        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operationName);
        int attempt = 1;
        try {
            while (true) {
                try {
                    return supplier.get();
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, operationVerb, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <RV> RV executeAttemptsNoSchemaException(String oid, String operationName, String operationVerb, OperationResult subResult,
            ResultSupplier<RV> supplier) throws ObjectNotFoundException {
        try {
            return executeAttempts(oid, operationName, operationVerb, subResult, supplier);
        } catch (SchemaException e) {
            throw new AssertionError("Should not occur", e);
        }
    }

    private <RV> RV executeAttemptsNoSchemaException(ObjectQuery query, String operationName, String operationVerb, OperationResult subResult,
            Supplier<RV> emptyQueryResultSupplier, ResultQueryBasedSupplier<RV> supplier) {
        try {
            return executeAttempts(query, operationName, operationVerb, subResult, emptyQueryResultSupplier, supplier);
        } catch (SchemaException e) {
            throw new AssertionError("Should not occur", e);
        }
    }

    private <RV> RV executeAttempts(ObjectQuery query, String operationName, String operationVerb, OperationResult subResult,
            Supplier<RV> emptyQueryResultSupplier, ResultQueryBasedSupplier<RV> supplier) throws SchemaException {

        if (query != null) {
            query = simplify(query, subResult);
            if (query == null) {
                return emptyQueryResultSupplier.get();
            }
        }

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operationName);
        int attempt = 1;
        try {
            while (true) {
                try {
                    return supplier.get(query);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operationVerb, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        Validate.notEmpty(shadowOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Searching shadow owner for {}", shadowOid);

        OperationResult subResult = result.createSubresult(SEARCH_SHADOW_OWNER);
        subResult.addParam("shadowOid", shadowOid);

        try {
            return executeAttempts(shadowOid, "searchShadowOwner", "searching shadow owner",
					subResult, () -> objectRetriever.searchShadowOwnerAttempt(shadowOid, options, subResult)
			);
        } catch (ObjectNotFoundException|SchemaException e) {
            throw new AssertionError("Should not occur; exception should have been treated in searchShadowOwnerAttempt.", e);
        }
    }

    @Override
    @Deprecated
    public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(accountOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Selecting account shadow owner for account {}.", new Object[]{accountOid});

        OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW);
        subResult.addParam("accountOid", accountOid);

        try {
            return executeAttempts(accountOid, "listAccountShadowOwner", "listing account shadow owner",
                    subResult, () -> objectRetriever.listAccountShadowOwnerAttempt(accountOid, subResult)
            );
        } catch (ObjectNotFoundException|SchemaException e) {
            throw new AssertionError("Should not occur; exception should have been treated in searchShadowOwnerAttempt.", e);
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        return executeAttempts(query, "searchObjects", "searching", subResult,
                () -> new SearchResultList<>(new ArrayList<PrismObject<T>>(0)),
                (q) -> objectRetriever.searchObjectsAttempt(type, q, options, subResult));
    }

    // utility method that simplifies a query, and checks for trivial cases (minimizing client code for such situation)
    private ObjectQuery simplify(ObjectQuery query, OperationResult subResult) {
        ObjectFilter filter = query.getFilter();
        filter = ObjectQueryUtil.simplify(filter);
        if (filter instanceof NoneFilter) {
			subResult.recordSuccess();
			return null;
		} else {
			query = replaceSimplifiedFilter(query, filter);
		}
        return query;
    }

    @NotNull
	private ObjectQuery replaceSimplifiedFilter(ObjectQuery query, ObjectFilter filter) {
		query = query.cloneEmpty();
		query.setFilter(filter instanceof AllFilter ? null : filter);
		return query;
	}

	@Override
    public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS);
        result.addParam("type", type.getName());
        result.addParam("query", query);

        return executeAttempts(query, "searchContainers", "searching", result,
                () -> new SearchResultList<>(new ArrayList<T>(0)),
                (q) -> objectRetriever.searchContainersAttempt(type, q, options, result));
    }

	@Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Counting containers of type '{}', query (on trace level).", type.getSimpleName());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", query == null ? "undefined" : query.debugDump());
        }

        OperationResult result = parentResult.createSubresult(COUNT_CONTAINERS);
        result.addParam("type", type.getName());
        result.addParam("query", query);

        return executeAttemptsNoSchemaException(query, "countContainers", "counting", result,
                () -> 0,
                (q) -> objectRetriever.countContainersAttempt(type, q, options, result));
    }

    private <T> void logSearchInputParameters(Class<T> type, ObjectQuery query, boolean iterative, Boolean strictlySequential) {
        ObjectPaging paging = query != null ? query.getPaging() : null;
        LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}, iterative {}, strictlySequential {}.",
                type.getSimpleName(), (paging != null ? paging.getOffset() : "undefined"),
                (paging != null ? paging.getMaxSize() : "undefined"), iterative, strictlySequential);

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[]{
                (query == null ? "undefined" : query.debugDump()),
                (paging != null ? paging.debugDump() : "undefined")});

        if (iterative) {
            LOGGER.trace("Iterative search by paging: {}, batch size {}",
                    getConfiguration().isIterativeSearchByPaging(),
                    getConfiguration().getIterativeSearchByPagingBatchSize());
        }
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        validateName(object);
        Validate.notNull(result, "Operation result must not be null.");

        if (options == null) {
            options = new RepoAddOptions();
        }

        LOGGER.debug("Adding object type '{}', overwrite={}, allowUnencryptedValues={}",
                object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                options.isAllowUnencryptedValues());

        if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
            CryptoUtil.checkEncrypted(object);
        }

        if (InternalsConfig.consistencyChecks) {
            object.checkConsistence(ConsistencyCheckScope.THOROUGH);
        } else {
            object.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        if (LOGGER.isTraceEnabled()) {
            // Explicitly log name
            PolyStringType namePolyType = object.asObjectable().getName();
            LOGGER.trace("NAME: {} - {}", namePolyType.getOrig(), namePolyType.getNorm());
        }

        OperationResult subResult = result.createSubresult(ADD_OBJECT);
        subResult.addParam("object", object);
        subResult.addParam("options", options);

        // TODO use executeAttempts
        final String operation = "adding";
        int attempt = 1;

        String proposedOid = object.getOid();
        while (true) {
            try {
                String createdOid = objectUpdater.addObjectAttempt(object, options, subResult);
	            invokeConflictWatchers((w) -> w.afterAddObject(createdOid, object));
	            return createdOid;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(proposedOid, operation, attempt, ex, subResult);
            }
        }
    }

    public void invokeConflictWatchers(Consumer<ConflictWatcherImpl> consumer) {
	    emptyIfNull(conflictWatchersThreadLocal.get()).forEach(consumer);
    }

    private void validateName(PrismObject object) throws SchemaException {
        PrismProperty name = object.findProperty(ObjectType.F_NAME);
        if (name == null || ((PolyString) name.getRealValue()).isEmpty()) {
            throw new SchemaException("Attempt to add object without name.");
        }
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Deleting object type '{}' with oid '{}'", type.getSimpleName(), oid);

        OperationResult subResult = result.createSubresult(DELETE_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        executeAttemptsNoSchemaException(oid, "deleteObject", "deleting",
                subResult, () -> objectUpdater.deleteObjectAttempt(type, oid, subResult)
        );
	    invokeConflictWatchers((w) -> w.afterDeleteObject(oid));
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult result) {
        return countObjects(type, query, null, result);
    }

    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
                Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Counting objects of type '{}', query (on trace level).", type.getSimpleName());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", query == null ? "undefined" : query.debugDump());
        }

        OperationResult subResult = result.createMinorSubresult(COUNT_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        return executeAttemptsNoSchemaException(query, "countObjects", "counting", subResult,
                () -> 0,
                (q) -> objectRetriever.countObjectsAttempt(type, q, options, subResult));
    }
    
    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        modifyObject(type, oid, modifications, null, result);
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
            RepoModifyOptions options, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
	    try {
		    modifyObject(type, oid, modifications, null, options, result);
	    } catch (PreconditionViolationException e) {
		    throw new AssertionError(e);    // with null precondition we couldn't get this exception
	    }
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
            ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);
        subResult.addCollectionOfSerializablesAsParam("modifications", modifications);

        if (modifications.isEmpty() && !RepoModifyOptions.isExecuteIfNoChanges(options)) {
            LOGGER.debug("Modification list is empty, nothing was modified.");
            subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
            return;
        }

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        if (InternalsConfig.consistencyChecks) {
            ItemDelta.checkConsistence(modifications, ConsistencyCheckScope.THOROUGH);
        } else {
            ItemDelta.checkConsistence(modifications, ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        if (LOGGER.isTraceEnabled()) {
            for (ItemDelta modification : modifications) {
                if (modification instanceof PropertyDelta<?>) {
                    PropertyDelta<?> propDelta = (PropertyDelta<?>) modification;
                    if (propDelta.getPath().equivalent(new ItemPath(ObjectType.F_NAME))) {
                        Collection<PrismPropertyValue<PolyString>> values = propDelta.getValues(PolyString.class);
                        for (PrismPropertyValue<PolyString> pval : values) {
                            PolyString value = pval.getValue();
                            LOGGER.trace("NAME delta: {} - {}", value.getOrig(), value.getNorm());
                        }
                    }
                }
            }
        }

        // TODO executeAttempts?
        final String operation = "modifying";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("modifyObject");

        try {
            while (true) {
                try {
                    objectUpdater.modifyObjectAttempt(type, oid, modifications, precondition, options, subResult, this);
	                invokeConflictWatchers((w) -> w.afterModifyObject(oid));
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
            Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
        subResult.addParam("oid", resourceOid);
        subResult.addParam("resourceObjectShadowType", resourceObjectShadowType);

        final String operation = "listing resource object shadows";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("listResourceObjectShadow");

        // TODO executeAttempts
        try {
            while (true) {
                try {
                    return objectRetriever.listResourceObjectShadowsAttempt(resourceOid, resourceObjectShadowType, subResult);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(resourceOid, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public RepositoryDiag getRepositoryDiag() {
        LOGGER.debug("Getting repository diagnostics.");

        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(IMPLEMENTATION_SHORT_NAME);
        diag.setImplementationDescription(IMPLEMENTATION_DESCRIPTION);

        SqlRepositoryConfiguration config = getConfiguration();

        //todo improve, find and use real values (which are used by sessionFactory) MID-1219
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while ((drivers != null && drivers.hasMoreElements())) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString(DETAILS_DATA_SOURCE, config.getDataSource()));
        details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, config.getHibernateDialect()));
        details.add(new LabeledString(DETAILS_HIBERNATE_HBM_2_DDL, config.getHibernateHbm2ddl()));

        readDetailsFromConnection(diag, config);

        Collections.sort(details, new Comparator<LabeledString>() {

            @Override
            public int compare(LabeledString o1, LabeledString o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel());
            }
        });

        return diag;
    }

    private void readDetailsFromConnection(RepositoryDiag diag, final SqlRepositoryConfiguration config) {
        final List<LabeledString> details = diag.getAdditionalDetails();

        Session session = baseHelper.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            session.doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    details.add(new LabeledString(DETAILS_TRANSACTION_ISOLATION,
                            getTransactionIsolation(connection, config)));


                    Properties info = connection.getClientInfo();
                    if (info == null) {
                        return;
                    }

                    for (String name : info.stringPropertyNames()) {
                        details.add(new LabeledString(DETAILS_CLIENT_INFO + name, info.getProperty(name)));
                    }
                }
            });
            session.getTransaction().commit();

			SessionFactory sessionFactory = baseHelper.getSessionFactory();
            if (!(sessionFactory instanceof SessionFactoryImpl)) {
                return;
            }
            SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) sessionFactory;
            // we try to override configuration which was read from sql repo configuration with
            // real configuration from session factory
            String dialect = sessionFactoryImpl.getDialect() != null ? sessionFactoryImpl.getDialect().getClass().getName() : null;
            details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, dialect));
        } catch (Throwable th) {
            //nowhere to report error (no operation result available)
            session.getTransaction().rollback();
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
    }

    private String getTransactionIsolation(Connection connection, SqlRepositoryConfiguration config) {
        String value = config.getTransactionIsolation() != null ?
                config.getTransactionIsolation().name() + "(read from repo configuration)" : null;

        try {
            switch (connection.getTransactionIsolation()) {
                case Connection.TRANSACTION_NONE:
                    value = "TRANSACTION_NONE (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    value = "TRANSACTION_READ_COMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    value = "TRANSACTION_READ_UNCOMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    value = "TRANSACTION_REPEATABLE_READ (read from connection)";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    value = "TRANSACTION_SERIALIZABLE (read from connection)";
                    break;
                default:
                    value = "Unknown value in connection.";
            }
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
        }

        return value;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        // TODO add some SQL-specific self-test methods
        // No self-tests for now
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {
        getClosureManager().checkAndOrRebuild(true, repairIfNecessary, false, false, testResult);
    }

    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(oid, "Object oid must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = parentResult.createMinorSubresult(GET_VERSION);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        // TODO executeAttempts
        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(GET_VERSION);

        final String operation = "getting version";
        int attempt = 1;
        try {
            while (true) {
                try {
                    String rv = objectRetriever.getVersionAttempt(type, oid, subResult);
	                invokeConflictWatchers((w) -> w.afterGetVersion(oid, rv));
                    return rv;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, true, strictlySequential);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS_ITERATIVE);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                subResult.recordSuccess();
                return null;
            } else {
				query = replaceSimplifiedFilter(query, filter);
			}
		}

        if (getConfiguration().isIterativeSearchByPaging()) {
            if (strictlySequential) {
                objectRetriever.searchObjectsIterativeByPagingStrictlySequential(type, query, handler, options, subResult);
            } else {
                objectRetriever.searchObjectsIterativeByPaging(type, query, handler, options, subResult);
            }
            return null;
        }

        /*
         * Here we store OIDs that were already sent to the client during previous attempts.
         */
        Set<String> retrievedOids = new HashSet<>();

//        turned off until resolved 'unfinished operation' warning
//        SqlPerformanceMonitor pm = getPerformanceMonitor();
//        long opHandle = pm.registerOperationStart(SEARCH_OBJECTS_ITERATIVE);

        final String operation = "searching iterative";
        int attempt = 1;
        try {
            while (true) {
                try {
                    objectRetriever.searchObjectsIterativeAttempt(type, query, handler, options, subResult, retrievedOids);
                    return null;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
//                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
//            pm.registerOperationFinish(opHandle, attempt);
        }
        // TODO conflict checking (if needed)
    }

    @Override
    public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids) throws SchemaException {
        Validate.notNull(upperOrgOid, "upperOrgOid must not be null.");
        Validate.notNull(lowerObjectOids, "lowerObjectOids must not be null.");

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Querying for subordination upper {}, lower {}", new Object[]{upperOrgOid, lowerObjectOids});

        if (lowerObjectOids.isEmpty()) {
            // trivial case
            return false;
        }

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("matchObject");
        try {
            while (true) {
                try {
                    return objectRetriever.isAnySubordinateAttempt(upperOrgOid, lowerObjectOids);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(upperOrgOid, "isAnySubordinate", attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }


	@Override
	public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {

        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ADVANCE_SEQUENCE);
        result.addParam("oid", oid);

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Advancing sequence {}", oid);

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("advanceSequence");
        try {
            while (true) {
                try {
                    return sequenceHelper.advanceSequenceAttempt(oid, result);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, "advanceSequence", attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
	}


    @Override
	public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(RETURN_UNUSED_VALUES_TO_SEQUENCE);
        result.addParam("oid", oid);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Returning unused values of {} to sequence {}", unusedValues, oid);
        }
        if (unusedValues == null || unusedValues.isEmpty()) {
            result.recordSuccess();
            return;
        }

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("returnUnusedValuesToSequence");
        try {
            while (true) {
                try {
                    sequenceHelper.returnUnusedValuesToSequenceAttempt(oid, unusedValues, result);
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, "returnUnusedValuesToSequence", attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
	}

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult result) {
        Validate.notNull(request, "Request must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Executing arbitrary query '{}'.", request);

        final String operation = "querying";
        int attempt = 1;

        OperationResult subResult = result.createMinorSubresult(EXECUTE_QUERY_DIAGNOSTICS);
        subResult.addParam("query", request);

        // TODO executeAttempts
        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("executeQueryDiagnostics");

        try {
            while (true) {
                try {
                    return objectRetriever.executeQueryDiagnosticsRequest(request, subResult);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

	@Override
	public <O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector,
			PrismObject<O> object, Trace logger, String logMessagePrefix) throws SchemaException {
		if (objectSelector == null) {
			logger.trace("{} null object specification", logMessagePrefix);
			return false;
		}
		
		SearchFilterType specFilterType = objectSelector.getFilter();
		ObjectReferenceType specOrgRef = objectSelector.getOrgRef();
		QName specTypeQName = objectSelector.getType();     // now it does not matter if it's unqualified
		PrismObjectDefinition<O> objectDefinition = object.getDefinition();
		
		// Type
		if (specTypeQName != null && !QNameUtil.match(specTypeQName, objectDefinition.getTypeName())) {
			logger.trace("{} type mismatch, expected {}, was {}", logMessagePrefix, specTypeQName, objectDefinition.getTypeName());
			return false;
		}
		
		// Filter
		if (specFilterType != null) {
			ObjectFilter specFilter = QueryJaxbConvertor.createObjectFilter(object.getCompileTimeClass(), specFilterType, object.getPrismContext());
            ObjectTypeUtil.normalizeFilter(specFilter);		//  we assume object is already normalized
            if (specFilter != null) {
				ObjectQueryUtil.assertPropertyOnly(specFilter, logMessagePrefix + " filter is not property-only filter");
			}
			try {
				if (!ObjectQuery.match(object, specFilter, matchingRuleRegistry)) {
					logger.trace("{} object OID {}", logMessagePrefix, object.getOid() );
					return false;
				}
			} catch (SchemaException ex) {
				throw new SchemaException(logMessagePrefix + "could not apply for " + object + ": "
						+ ex.getMessage(), ex);
			}
		}
			
		// Org	
		if (specOrgRef != null) {
			if (!isDescendant(object, specOrgRef.getOid())) {
				LOGGER.trace("{} object OID {} (org={})",
						logMessagePrefix, object.getOid(), specOrgRef.getOid());
				return false;
			}			
		}
		
		return true;
	}

	@Override
	public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid) throws SchemaException {
		List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
		List<String> objParentOrgOids = new ArrayList<>(objParentOrgRefs.size());
		for (ObjectReferenceType objParentOrgRef: objParentOrgRefs) {
			objParentOrgOids.add(objParentOrgRef.getOid());
		}
		return isAnySubordinate(orgOid, objParentOrgOids);
	}
	
	@Override
	public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid) throws SchemaException {
		if (object.getOid() == null) {
			return false;
		}
		Collection<String> oidList = new ArrayList<>(1);
		oidList.add(oid);
		return isAnySubordinate(object.getOid(), oidList);
	}

	@Override
	public QName getApproximateSupportedMatchingRule(Class<?> dataType, QName originalMatchingRule) {
		if (String.class.equals(dataType)) {
			return StringMatcher.getApproximateSupportedMatchingRule(originalMatchingRule);
		} else if (PolyString.class.equals(dataType) || PolyStringType.class.equals(dataType)) {
			return PolyStringMatcher.getApproximateSupportedMatchingRule(originalMatchingRule);
		} else {
			return DefaultMatcher.getApproximateSupportedMatchingRule(originalMatchingRule);
		}
	}

	@Override
	public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
		LOGGER.info("Applying full text search configuration ({} entries)",
				fullTextSearch != null ? fullTextSearch.getIndexed().size() : 0);
		fullTextSearchConfiguration = fullTextSearch;
	}

	@Override
	public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
		return fullTextSearchConfiguration;
	}

	@Override
	public void postInit(OperationResult result) throws SchemaException {

		SystemConfigurationType systemConfiguration;
		try {
			systemConfiguration = getObject(SystemConfigurationType.class,
					SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result).asObjectable();
		} catch (ObjectNotFoundException e) {
			// ok, no problem e.g. for tests or initial startup
			LOGGER.debug("System configuration not found, exiting postInit method.");
			return;
		}

		SystemConfigurationHolder.setCurrentConfiguration(systemConfiguration);

		Configuration systemConfigFromFile = midpointConfiguration.getConfiguration(MidpointConfiguration.SYSTEM_CONFIGURATION_SECTION);
		if (systemConfigFromFile != null && systemConfigFromFile
				.getBoolean(LoggingConfigurationManager.SYSTEM_CONFIGURATION_SKIP_REPOSITORY_LOGGING_SETTINGS, false)) {
			LOGGER.warn("Skipping application of repository logging configuration because {}=true", LoggingConfigurationManager.SYSTEM_CONFIGURATION_SKIP_REPOSITORY_LOGGING_SETTINGS);
		} else {
			LoggingConfigurationType loggingConfig = ProfilingConfigurationManager.checkSystemProfilingConfiguration(
					systemConfiguration.asPrismObject());
			if (loggingConfig != null) {
				LoggingConfigurationManager.configure(loggingConfig, systemConfiguration.getVersion(), result);
			}
		}
		applyFullTextSearchConfiguration(systemConfiguration.getFullTextSearch());
        SystemConfigurationTypeUtil.applyOperationResultHandling(systemConfiguration);
	}

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(String oid) {
	    List<ConflictWatcherImpl> watchers = conflictWatchersThreadLocal.get();
	    if (watchers == null) {
	    	conflictWatchersThreadLocal.set(watchers = new ArrayList<>());
	    }
	    if (watchers.size() >= MAX_CONFLICT_WATCHERS) {
	    	throw new IllegalStateException("Conflicts watchers leaking: reached limit of " + MAX_CONFLICT_WATCHERS + ": " + watchers);
	    }
	    ConflictWatcherImpl watcher = new ConflictWatcherImpl(oid);
	    watchers.add(watcher);
	    return watcher;
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {
    	ConflictWatcherImpl watcherImpl = (ConflictWatcherImpl) watcher;
	    List<ConflictWatcherImpl> watchers = conflictWatchersThreadLocal.get();
	    // change these exceptions to logged errors, eventually
        if (watchers == null) {
            throw new IllegalStateException("No conflict watchers registered for current thread; tried to unregister " + watcher);
        } else if (!watchers.remove(watcherImpl)) {     // expecting there's only one
            throw new IllegalStateException("Tried to unregister conflict watcher " + watcher + " that was not registered");
        }
    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
    	if (watcher.hasConflict()) {
    		return true;
	    }
	    try {
		    getVersion(ObjectType.class, watcher.getOid(), result);
	    } catch (ObjectNotFoundException e) {
		    // just ignore this
	    } catch (SchemaException e) {
		    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check conflicts for {}", e, watcher.getOid());
	    }
	    return watcher.hasConflict();
    }
}

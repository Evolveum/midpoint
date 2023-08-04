/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.annotation.PreDestroy;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PSQLException;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MGlobalMetadata;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QGlobalMetadata;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SqaleServiceBase {

    protected final Trace logger = TraceManager.getTrace(getClass());

    /**
     * Name of the repository implementation.
     * While public, the value is often copied because this service class is implementation
     * detail for the rest of the midPoint.
     */
    public static final String REPOSITORY_IMPL_NAME = "Native";

    /**
     * Class name prefix for operation names, including the dot separator.
     * Use with various `RepositoryService.OP_*` constants, not with constants without `OP_`
     * prefix because they already contain class name of the service interface.
     *
     * [NOTE]
     * This distinguishes operation names for audit and repository (e.g. `searchObjects`) - both
     * in operation results and in performance monitoring (which previously didn't include class names).
     * To make things compact enough, simple (short) class name is used.
     */
    protected final String opNamePrefix = getClass().getSimpleName() + '.';

    protected final SqaleRepoContext sqlRepoContext;
    protected final SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection;
    protected SqlPerformanceMonitorImpl performanceMonitor; // set to null in destroy

    public SqaleServiceBase(SqaleRepoContext sqlRepoContext, SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        this.sqlRepoContext = sqlRepoContext;
        this.sqlPerformanceMonitorsCollection = sqlPerformanceMonitorsCollection;

        // monitor initialization and registration
        JdbcRepositoryConfiguration repoConfig = sqlRepoContext.getJdbcRepositoryConfiguration();
        performanceMonitor = new SqlPerformanceMonitorImpl(
                repoConfig.getPerformanceStatisticsLevel(),
                repoConfig.getPerformanceStatisticsFile());
        sqlPerformanceMonitorsCollection.register(performanceMonitor);
    }

    public SqaleRepoContext sqlRepoContext() {
        return sqlRepoContext;
    }

    public SqaleRepositoryConfiguration repositoryConfiguration() {
        return (SqaleRepositoryConfiguration) sqlRepoContext.getJdbcRepositoryConfiguration();
    }

    protected PrismContext prismContext() {
        return sqlRepoContext.prismContext();
    }

    // implements also RepositoryService method (not declared in AuditService)
    public SqlPerformanceMonitorImpl getPerformanceMonitor() {
        return performanceMonitor;
    }

    // region exception handling

    /**
     * Handles exception outside of transaction - this does not handle transactional problems.
     * Returns {@link SystemException}, call with `throw` keyword.
     */
    protected SystemException handledGeneralException(
            @NotNull Throwable ex, @NotNull OperationResult operationResult) {
        recordFatalError(operationResult, ex);
        return ex instanceof SystemException
                ? (SystemException) ex
                : new SystemException(ex.getMessage(), ex);
    }

    protected void recordFatalError(@NotNull OperationResult operationResult, @NotNull Throwable t) {
        String exceptionMessage = t.toString();
        if (t instanceof com.querydsl.core.QueryException) {
            PSQLException psqlException = ExceptionUtil.findCause(t, PSQLException.class);
            // PSQLException message is lost in QueryException and it may be handy
            if (psqlException != null) {
                exceptionMessage += "\n" + psqlException.getMessage();
            }
        }
        logger.debug("Unexpected exception (will be rethrown and handled higher): {}\n"
                + "  OPERATION RESULT: {}", exceptionMessage, operationResult.debugDump());
        operationResult.recordFatalError(t);
    }
    // endregion

    // region search support methods
    protected <T> void logSearchInputParameters(Class<T> type, ObjectQuery query, String operation) {
        ObjectPaging paging = query != null ? query.getPaging() : null;
        logger.debug(
                "{} of type '{}' (full query on trace level), offset {}, limit {}.",
                operation, type.getSimpleName(),
                paging != null ? paging.getOffset() : "undefined",
                paging != null ? paging.getMaxSize() : "undefined");

        logger.trace("Full query\n{}",
                query == null ? "undefined" : query.debugDumpLazily());
    }

    protected ObjectQuery simplifyQuery(ObjectQuery query) {
        if (query != null) {
            // simplify() creates new filter instance which can be modified
            ObjectFilter filter = ObjectQueryUtil.simplify(query.getFilter(), prismContext());
            query = query.cloneWithoutFilter();
            query.setFilter(filter instanceof AllFilter ? null : filter);
        }

        return query;
    }

    protected boolean isNoneQuery(ObjectQuery query) {
        return query != null && query.getFilter() instanceof NoneFilter;
    }
    // endregion

    // region perf monitoring

    /**
     * Registers operation start with specified short operation type name.
     */
    protected <T extends Containerable> long registerOperationStart(
            String kind, PrismContainer<T> object) {
        return registerOperationStart(kind, object.getCompileTimeClass());
    }

    protected <T extends Containerable> long registerOperationStart(String kind, Class<T> type) {
        return performanceMonitor != null
                ? performanceMonitor.registerOperationStart(opNamePrefix + kind, type)
                : -1;
    }

    protected void registerOperationFinish(long opHandle) {
        if (performanceMonitor != null) {
            performanceMonitor.registerOperationFinish(opHandle, 1);
        }
    }

    @PreDestroy
    public void destroy() {
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
            sqlPerformanceMonitorsCollection.deregister(performanceMonitor);
            performanceMonitor = null;
        }
    }
    // endregion

    @NotNull public RepositoryDiag getRepositoryDiag() {
        logger.debug("Getting repository diagnostics.");

        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(REPOSITORY_IMPL_NAME);
        diag.setImplementationDescription(
                "Implementation that stores data in PostgreSQL database using JDBC with Querydsl.");

        JdbcRepositoryConfiguration config = repositoryConfiguration();
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString("dataSource", config.getDataSource()));

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            details.add(new LabeledString("transactionIsolation",
                    getTransactionIsolation(jdbcSession.connection(), config)));

            try {
                Properties info = jdbcSession.connection().getClientInfo();
                if (info != null) {
                    for (String name : info.stringPropertyNames()) {
                        details.add(new LabeledString("clientInfo." + name, info.getProperty(name)));
                    }
                }
            } catch (SQLException e) {
                details.add(new LabeledString("clientInfo-error", e.toString()));
            }

            long startMs = System.currentTimeMillis();
            jdbcSession.executeStatement("select 1");
            details.add(new LabeledString("select-1-round-trip-ms",
                    String.valueOf(System.currentTimeMillis() - startMs)));

            addGlobalMetadataInfo(jdbcSession, details);
        }

        details.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel()));

        return diag;
    }

    private void addGlobalMetadataInfo(JdbcSession jdbcSession, List<LabeledString> details) {
        List<MGlobalMetadata> list = jdbcSession.newQuery()
                .from(QGlobalMetadata.DEFAULT)
                .select(QGlobalMetadata.DEFAULT)
                .fetch();

        for (MGlobalMetadata metadata : list) {
            details.add(new LabeledString(metadata.name, metadata.value));
        }
    }

    private String getTransactionIsolation(
            Connection connection, JdbcRepositoryConfiguration config) {
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
}

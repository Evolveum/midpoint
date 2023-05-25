/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PSQLException;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SqaleServiceBase {

    protected final Trace logger = TraceManager.getTrace(getClass());

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
    // endregion

    // region perf monitoring

    /**
     * Registers operation start with specified short operation type name.
     */
    protected <T extends Containerable> long registerOperationStart(
            String kind, PrismContainer<T> object) {
        return registerOperationStart(kind, object.getCompileTimeClass());
    }

    protected long registerOperationStart(String kind, Class<?> type) {
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
}

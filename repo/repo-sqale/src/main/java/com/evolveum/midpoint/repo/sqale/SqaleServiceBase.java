/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SqaleServiceBase {

    protected final Trace logger = TraceManager.getTrace(SqaleRepositoryService.class);

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

    protected JdbcRepositoryConfiguration repositoryConfiguration() {
        return sqlRepoContext.getJdbcRepositoryConfiguration();
    }

    // region exception handling

    /**
     * Handles exception outside of transaction - this does not handle transactional problems.
     * Returns {@link SystemException}, call with `throw` keyword.
     */
    protected SystemException handledGeneralException(
            @NotNull Throwable ex, OperationResult operationResult) {
        logger.error("General checked exception occurred.", ex);

        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (operationResult != null && isFatalException(ex)) {
            operationResult.recordFatalError(ex);
        }

        return ex instanceof SystemException
                ? (SystemException) ex
                : new SystemException(ex.getMessage(), ex);
    }

    protected void handleGeneralRuntimeException(
            @NotNull RuntimeException ex,
            @NotNull JdbcSession jdbcSession,
            @Nullable OperationResult result) {
        logger.debug("General runtime exception occurred (session {})", jdbcSession.sessionId(), ex);

        if (isFatalException(ex)) {
            if (result != null) {
                result.recordFatalError(ex);
            }
            jdbcSession.rollback();

            if (ex instanceof SystemException) {
                throw ex;
            } else {
                throw new SystemException(ex.getMessage(), ex);
            }
        } else {
            jdbcSession.rollback();
            // this exception will be caught and processed in logOperationAttempt,
            // so it's safe to pass any RuntimeException here
            throw ex;
        }
    }

    /**
     * Rolls back the transaction and throws exception.
     *
     * @throws SystemException wrapping the exception used as parameter
     */
    protected void handleGeneralCheckedException(
            @NotNull Throwable ex,
            @NotNull JdbcSession jdbcSession,
            @Nullable OperationResult result) {
        logger.error("General checked exception occurred (session {})", jdbcSession.sessionId(), ex);

        if (result != null && isFatalException(ex)) {
            result.recordFatalError(ex);
        }
        jdbcSession.rollback();
        throw new SystemException(ex.getMessage(), ex);
    }
    // true means fatal error operation result

    protected boolean isFatalException(Throwable ex) {
        return true; // TODO
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
        // TODO what about class prefix? If not used some audit/repo ops would mingle.
        //  a) we will use prefix (this version); b) we will name ops distinctively
        return performanceMonitor != null
                ? performanceMonitor.registerOperationStart(opNamePrefix + kind, type)
                : -1;
    }

    protected void registerOperationFinish(long opHandle, int attempt) {
        if (performanceMonitor != null) {
            performanceMonitor.registerOperationFinish(opHandle, attempt);
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

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
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

    protected JdbcRepositoryConfiguration repositoryConfiguration() {
        return sqlRepoContext.getJdbcRepositoryConfiguration();
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
            @NotNull Throwable ex, OperationResult operationResult) {
        logger.error("General checked exception occurred.", ex);

        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (operationResult != null) {
            operationResult.recordFatalError(ex);
        }

        return ex instanceof SystemException
                ? (SystemException) ex
                : new SystemException(ex.getMessage(), ex);
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
}

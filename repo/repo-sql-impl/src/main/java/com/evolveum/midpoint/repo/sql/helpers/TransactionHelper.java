/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * @author mederly
 */
@Component
public class TransactionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

    @Autowired
    @Qualifier("repositoryService")
    private RepositoryService repositoryService;

    private SqlRepositoryServiceImpl getSqlRepo() {
        return (SqlRepositoryServiceImpl) repositoryService;
    }

    public Session beginReadOnlyTransaction() {
        return beginTransaction(getSqlRepo().getConfiguration().isUseReadOnlyTransactions());
    }

    public Session beginTransaction() {
        return beginTransaction(false);
    }

    public Session beginTransaction(boolean readOnly) {
        return getSqlRepo().beginTransaction(readOnly);
    }

    public void rollbackTransaction(Session session, Exception ex, OperationResult result, boolean fatal) {
        getSqlRepo().rollbackTransaction(session, ex, result, fatal);
    }

    public void rollbackTransaction(Session session, Exception ex, String message, OperationResult result, boolean fatal) {
        getSqlRepo().rollbackTransaction(session, ex, message, result, fatal);
    }

    public void rollbackTransaction(Session session) {
        getSqlRepo().rollbackTransaction(session);
    }

    public void cleanupSessionAndResult(Session session, OperationResult result) {
        getSqlRepo().cleanupSessionAndResult(session, result);
    }

    public void handleGeneralException(Exception ex, Session session, OperationResult result) {
        getSqlRepo().handleGeneralException(ex, session, result);
    }

    public void handleGeneralRuntimeException(RuntimeException ex, Session session, OperationResult result) {
        getSqlRepo().handleGeneralRuntimeException(ex, session, result);
    }

    public void handleGeneralCheckedException(Exception ex, Session session, OperationResult result) {
        getSqlRepo().handleGeneralCheckedException(ex, session, result);
    }

    public SQLException findSqlException(Throwable ex) {
        return getSqlRepo().findSqlException(ex);
    }
}
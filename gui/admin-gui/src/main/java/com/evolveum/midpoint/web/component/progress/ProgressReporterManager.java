/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ISessionListener;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProgressReporterManager implements ISessionListener {

    private static final Trace LOGGER = TraceManager.getTrace(ProgressReporterManager.class);

    @Autowired
    private MidPointApplication application;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelInteractionService modelInteractionService;
    @Autowired
    private SecurityContextManager securityContextManager;

    private ExecutorService executor = Executors.newCachedThreadPool();

    private Map<Key, ProgressReporter> reporters = new Hashtable<>();

    @Override
    public void onCreated(Session session) {
        // we don't care about created sessions
    }

    @Override
    public void onUnbound(String sessionId) {
        Set<Key> keys = new HashSet();
        keys.addAll(reporters.keySet());

        for (Key key : keys) {
            if (!key.sessionId.equals(sessionId)) {
                continue;
            }

            cleanupReporter(key);
        }
    }

    public ProgressReporter createReporter(WebApplicationConfiguration config) {
        Key key = createReporterIdentifier(UUID.randomUUID().toString());

        ProgressReporter reporter = new ProgressReporter(key.reporterId, application);
        reporter.setRefreshInterval(config.getProgressRefreshInterval());
        reporter.setAsynchronousExecution(config.isProgressReportingEnabled());
        reporter.setAbortEnabled(config.isAbortEnabled());

        reporters.put(key, reporter);

        return reporter;
    }

    public ProgressReporter getReporter(@NotNull String reporterId) {
        Key key = createReporterIdentifier(reporterId);
        return reporters.get(key);
    }

    public void cleanupReporter(@NotNull String reporterId) {
        Key key = createReporterIdentifier(reporterId);

        cleanupReporter(key);
    }

    /**
     * Executes changes on behalf of the parent page. By default, changes are executed asynchronously (in
     * a separate thread). However, when set in the midpoint configuration, changes are executed synchronously.
     *
     * @param deltas  Deltas to be executed.
     * @param options Model execution options.
     * @param task    Task in context of which the changes have to be executed.
     * @param result  Operation result.
     */
    public void executeChanges(String reporterId, Collection<ObjectDelta<? extends ObjectType>> deltas,
                               boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {
        ProgressReporter reporter = getReporter(reporterId);
        if (reporter == null) {
            throw new IllegalStateException("Progress reporter with id '" + reporterId + "' doesn't exist");
        }

        if (reporter.isAsynchronousExecution()) {
            executeChangesAsync(reporter, deltas, previewOnly, options, task, result);
        } else {
            executeChangesSync(reporter, deltas, previewOnly, options, task, result);
        }
    }

    private void executeChangesSync(ProgressReporter reporter, Collection<ObjectDelta<? extends ObjectType>> deltas,
                                    boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {
        try {
            if (previewOnly) {
                ModelContext previewResult = modelInteractionService.previewChanges(deltas, options, task, result);
                reporter.setPreviewResult(previewResult);
            } else {
                modelService.executeChanges(deltas, options, task, result);
            }
            result.computeStatusIfUnknown();
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
            if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
                result.recordFatalError(e.getMessage(), e);
            }
        }
    }

    private void executeChangesAsync(ProgressReporter reporter, Collection<ObjectDelta<? extends ObjectType>> deltas,
                                     boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
        Runnable execution = () -> {
            try {
                LOGGER.debug("Execution start");

                securityContextManager.storeConnectionInformation(connInfo);
                securityContextManager.setupPreAuthenticatedSecurityContext(authentication);
                reporter.recordExecutionStart();

                if (previewOnly) {
                    ModelContext previewResult = modelInteractionService
                            .previewChanges(deltas, options, task, Collections.singleton(reporter), result);
                    reporter.setPreviewResult(previewResult);
                } else {
                    modelService.executeChanges(deltas, options, task, Collections.singleton(reporter), result);
                }
            } catch (CommonException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
                if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
                    result.recordFatalError(e.getMessage(), e);
                }
            } finally {
                LOGGER.debug("Execution finish {}", result);
            }
            reporter.recordExecutionStop();
            reporter.setAsyncOperationResult(result);          // signals that the operation has finished
        };

        result.recordInProgress(); // to disable showing not-final results (why does it work? and why is the result shown otherwise?)

        Future future = executor.submit(execution);
        reporter.setFuture(future);
    }

    private void cleanupReporter(Key key) {
        ProgressReporter reporter = reporters.get(key);
        if (reporter == null) {
            return;
        }

        if (reporter.getFuture() != null) {
            reporter.getFuture().cancel(true);
        }

        reporters.remove(key);
    }

    private Key createReporterIdentifier(String reporterId) {
        RequestCycle rc = RequestCycle.get();
        Request req = rc.getRequest();

        HttpSession session = ((ServletWebRequest) req).getContainerRequest().getSession();
        return new Key(session.getId(), reporterId);
    }

    private static class Key {

        String sessionId;
        String reporterId;

        public Key(String sessionId, String reporterId) {
            this.sessionId = sessionId;
            this.reporterId = reporterId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (sessionId != null ? !sessionId.equals(key.sessionId) : key.sessionId != null) return false;
            return reporterId != null ? reporterId.equals(key.reporterId) : key.reporterId == null;
        }

        @Override
        public int hashCode() {
            int result = sessionId != null ? sessionId.hashCode() : 0;
            result = 31 * result + (reporterId != null ? reporterId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return StringUtils.join(new Object[]{sessionId, reporterId}, "/");
        }
    }
}

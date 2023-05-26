/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ISessionListener;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AsyncWebProcessManagerImpl implements ISessionListener, AsyncWebProcessManager {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncWebProcessManagerImpl.class);

    @Autowired
    private MidPointApplication application;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<Key, AsyncWebProcess<?>> processes = new Hashtable<>();

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    @Override
    public void onCreated(Session session) {
        // we don't care about created sessions
    }

    @Override
    public void onUnbound(String sessionId) {
        LOGGER.trace("Cleaning up processes for session id {}", sessionId);

        Set<Key> keys = new HashSet<>(processes.keySet());
        for (Key key : keys) {
            if (!key.sessionId.equals(sessionId)) {
                continue;
            }

            removeProcess(key);
        }
    }

    @Override
    public <T> AsyncWebProcess<T> createProcess(T data) {
        Key key = createProcessIdentifier(UUID.randomUUID().toString());

        AsyncWebProcess<T> process = new AsyncWebProcess<>(key.processId, application);
        process.setData(data);
        processes.put(key, process);

        return process;
    }

    @Override
    public <T> AsyncWebProcess<T> createProcess() {
        return createProcess(null);
    }

    @Override
    public AsyncWebProcess getProcess(@NotNull String processId) {
        Key key = createProcessIdentifier(processId);
        return processes.get(key);
    }

    @Override
    public boolean removeProcess(@NotNull String processId) {
        Key key = createProcessIdentifier(processId);

        return removeProcess(key);
    }

    @Override
    public void submit(@NotNull String processId, Runnable runnable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextManager secManager = application.getSecurityContextManager();

        submit(processId, new SecurityContextAwareCallable(secManager, auth) {

            @Override
            public Object callWithContextPrepared() {
                runnable.run();

                return null;
            }
        });
    }

    @Override
    public void submit(@NotNull String processId, @NotNull Callable callable) {
        AsyncWebProcess process = getProcess(processId);

        if (process == null) {
            throw new IllegalStateException("Process with id '" + processId + "' doesn't exist");
        }

        Callable<?> securityAware = callable;

        if (!(callable instanceof SecurityContextAwareCallable)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            SecurityContextManager secManager = application.getSecurityContextManager();

            securityAware = new SecurityContextAwareCallable(secManager, auth) {

                @Override
                public Object callWithContextPrepared() throws Exception {
                    return callable.call();
                }
            };
        }

        Future<?> future = executor.submit(securityAware);
        process.setFuture(future);
    }

    private boolean removeProcess(Key key) {
        AsyncWebProcess process = processes.get(key);
        if (process == null) {
            return false;
        }

        if (process.getFuture() != null) {
            process.getFuture().cancel(true);
        }

        return processes.remove(key) != null;
    }

    private Key createProcessIdentifier(String processId) {
        RequestCycle rc = RequestCycle.get();
        Request req = rc.getRequest();

        HttpSession session = ((ServletWebRequest) req).getContainerRequest().getSession();
        return new Key(session.getId(), processId);
    }

    private static class Key {

        String sessionId;
        String processId;

        public Key(String sessionId, String processId) {
            this.sessionId = sessionId;
            this.processId = processId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            Key key = (Key) o;

            return Objects.equals(sessionId, key.sessionId)
                    && Objects.equals(processId, key.processId);
        }

        @Override
        public int hashCode() {
            int result = sessionId != null ? sessionId.hashCode() : 0;
            result = 31 * result + (processId != null ? processId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return StringUtils.join(new Object[] { sessionId, processId }, "/");
        }
    }
}

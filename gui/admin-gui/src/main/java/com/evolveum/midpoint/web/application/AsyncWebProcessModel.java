/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AsyncWebProcessModel<T> implements IModel<AsyncWebProcess<T>> {

    /**
     * Identifier of {@link AsyncWebProcess}.
     */
    private final String id;

    private transient AsyncWebProcess<T> process;

    public AsyncWebProcessModel() {
        this(null);
    }

    public AsyncWebProcessModel(T data) {
        process = createProcess(data);
        id = process.getId();
    }

    public String getId() {
        return id;
    }

    @Override
    public AsyncWebProcess<T> getObject() {
        if (process != null) {
            return process;
        }

        process = loadProcess();
        return process;
    }

    public T getProcessData() {
        return getObject().getData();
    }

    @Override
    public void detach() {
        process = null;
    }

    private AsyncWebProcess<T> createProcess(T data) {
        AsyncWebProcessManager manager = MidPointApplication.get().getAsyncWebProcessManager();
        return manager.createProcess(data);
    }

    private AsyncWebProcess<T> loadProcess() {
        AsyncWebProcessManager manager = MidPointApplication.get().getAsyncWebProcessManager();
        return manager.getProcess(id);
    }
}

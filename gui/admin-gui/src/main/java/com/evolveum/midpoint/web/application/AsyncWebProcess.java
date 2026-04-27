/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import java.util.concurrent.Future;

import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Class that represents asynchronous process in GUI.
 *
 * @author Viliam Repan (lazyman)
 * @see AsyncWebProcessManager
 * @see AsyncWebProcessModel
 */
public class AsyncWebProcess<T> {

    /**
     * Process identifier
     */
    private final String id;

    private final MidPointApplication application;

    /**
     * Input data used for this process.
     * It is not used by the process manager, but it can be used by the process itself.
     */
    private T data;

    private Future future;

    public AsyncWebProcess(String id, MidPointApplication application) {
        this.id = id;
        this.application = application;
    }

    public String getId() {
        return id;
    }

    public Future getFuture() {
        return future;
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    public MidPointApplication getApplication() {
        return application;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isDone() {
        return future != null && future.isDone();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AsyncWebProcess{");
        sb.append("id='").append(id).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

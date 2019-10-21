/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.web.security.MidPointApplication;

import java.util.concurrent.Future;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AsyncWebProcess<T> {

    private String id;
    private MidPointApplication application;

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

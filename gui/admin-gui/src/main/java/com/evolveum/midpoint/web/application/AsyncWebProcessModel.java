/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AsyncWebProcessModel<T> implements IModel<AsyncWebProcess<T>> {

    private transient AsyncWebProcess<T> process;
    private String id;
    private Task task;

    public AsyncWebProcessModel() {
        this(true, null);
    }

    public AsyncWebProcessModel(T data) {
        this(true, data);
    }

    public AsyncWebProcessModel(boolean createProcessEagerly, T data) {
        if (createProcessEagerly) {
            createProcess(data);
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public AsyncWebProcess<T> getObject() {
        if (process != null) {
            return process;
        }

        return createProcess(null);
    }

    public T getProcessData() {
        return getObject().getData();
    }

    @Override
    public void setObject(AsyncWebProcess<T> object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach() {
        process = null;
    }

    private AsyncWebProcess createProcess(T data) {
        AsyncWebProcessManager manager = MidPointApplication.get().getAsyncWebProcessManager();
        if (id != null) {
            process = manager.getProcess(id);

            return process;
        }

        process = manager.createProcess(data);
        id = process.getId();

        return process;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}

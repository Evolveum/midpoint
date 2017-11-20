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

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AsyncWebProcessModel<T> implements IModel<AsyncWebProcess<T>> {

    private transient AsyncWebProcess<T> process;
    private String id;

    public AsyncWebProcessModel() {
        this(true);
    }

    public AsyncWebProcessModel(boolean createProcessEagerly) {
        if (createProcessEagerly) {
            createProcess();
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

        return createProcess();
    }

    @Override
    public void setObject(AsyncWebProcess<T> object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach() {
        process = null;
    }

    private AsyncWebProcess createProcess() {
        AsyncWebProcessManager manager = MidPointApplication.get().getAsyncWebProcessManager();
        if (id != null) {
            process = manager.getProcess(id);

            return process;
        }

        process = manager.createProcess();
        id = process.getId();

        return process;
    }
}

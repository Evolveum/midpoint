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

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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface AsyncWebProcessManager {

    <T> AsyncWebProcess<T> createProcess(T data);

    <T> AsyncWebProcess<T> createProcess();

    AsyncWebProcess getProcess(@NotNull String processId);

    boolean removeProcess(@NotNull String processId);

    void submit(@NotNull String processId, Runnable runnable);

    void submit(@NotNull String processId, @NotNull Callable callable);
}

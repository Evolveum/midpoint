/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

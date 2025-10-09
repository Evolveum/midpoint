/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

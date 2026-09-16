/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import java.util.concurrent.Callable;

import org.jetbrains.annotations.NotNull;

/**
 * Interface that helps with management of asynchrlnous processes in GUI.
 *
 * @author Viliam Repan (lazyman)
 */
public interface AsyncWebProcessManager {

    /**
     * Create process instance reference.
     *
     * @param data Optional input data.
     */
    <T> AsyncWebProcess<T> createProcess(T data);

    <T> AsyncWebProcess<T> getProcess(@NotNull String processId);

    boolean removeProcess(@NotNull String processId);

    void submit(@NotNull String processId, Runnable runnable);

    void submit(@NotNull String processId, @NotNull Callable<?> callable);
}

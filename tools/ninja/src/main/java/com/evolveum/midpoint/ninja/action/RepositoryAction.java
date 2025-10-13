/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action;

/**
 * Base implementation class for action (Ninja command) running against the repository.
 *
 * @param <O> options class
 */
public abstract class RepositoryAction<O, R> extends Action<O, R> {

    public RepositoryAction() {
    }

    public RepositoryAction(boolean partial) {
        super(partial);
    }
}

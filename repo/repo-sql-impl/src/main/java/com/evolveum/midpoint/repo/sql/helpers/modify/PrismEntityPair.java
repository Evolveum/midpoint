/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.PrismValue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PrismEntityPair<T> {

    private PrismValue prism;
    private T repository;

    public PrismEntityPair(PrismValue prism, T repository) {
        this.prism = prism;
        this.repository = repository;
    }

    public PrismValue getPrism() {
        return prism;
    }

    public T getRepository() {
        return repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrismEntityPair that = (PrismEntityPair) o;

        return repository != null ? repository.equals(that.repository) : that.repository == null;
    }

    @Override
    public int hashCode() {
        return repository != null ? repository.hashCode() : 0;
    }
}

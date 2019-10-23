/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.schema.RelationRegistry;

/**
 * @author Viliam Repan (lazyman).
 */
public class MapperContext {

    private RepositoryContext repositoryContext;

    private Object owner;

    private ItemDelta delta;
    private PrismValue value;

    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    public void setRepositoryContext(RepositoryContext repositoryContext) {
        this.repositoryContext = repositoryContext;
    }

    public RepositoryService getRepositoryService() {
        return repositoryContext.repositoryService;
    }

    public PrismContext getPrismContext() {
        return repositoryContext.prismContext;
    }

    public RelationRegistry getRelationRegistry() {
        return repositoryContext.relationRegistry;
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public ItemDelta getDelta() {
        return delta;
    }

    public void setDelta(ItemDelta delta) {
        this.delta = delta;
    }

    public PrismValue getValue() {
        return value;
    }

    public void setValue(PrismValue value) {
        this.value = value;
    }
}

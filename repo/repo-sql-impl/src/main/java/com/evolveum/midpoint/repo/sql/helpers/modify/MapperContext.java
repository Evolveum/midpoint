/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;

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

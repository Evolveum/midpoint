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
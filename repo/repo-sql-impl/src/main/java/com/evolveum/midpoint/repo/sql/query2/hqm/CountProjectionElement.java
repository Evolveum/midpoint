/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author mederly
 */
public class CountProjectionElement extends ProjectionElement {

    @NotNull private final List<String> items;
    private final boolean distinct;

    public CountProjectionElement(@NotNull List<String> items, boolean distinct) {
        this.items = items;
        this.distinct = distinct;
    }

    protected void dumpToHql(StringBuilder sb) {
        sb.append("count(");
        if (distinct) {
            sb.append("distinct ");
        }
        sb.append(StringUtils.join(items, ", ")).append(")");
    }
}

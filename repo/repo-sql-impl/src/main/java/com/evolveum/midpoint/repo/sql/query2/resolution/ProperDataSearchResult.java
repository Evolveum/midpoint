/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class ProperDataSearchResult<T extends JpaDataNodeDefinition> extends DataSearchResult<T> {

    @NotNull private final JpaEntityDefinition entityDefinition;      // entity in which the item was found

    public ProperDataSearchResult(@NotNull JpaEntityDefinition entityDefinition, @NotNull DataSearchResult<T> result) {
        super(result.getLinkDefinition(), result.getRemainder());
        this.entityDefinition = entityDefinition;
    }

    @NotNull
    public JpaEntityDefinition getEntityDefinition() {
        return entityDefinition;
    }

    @Override
    public String toString() {
        return "ProperDefinitionSearchResult{" +
                "entity=" + entityDefinition + ", item=" + getLinkDefinition() + ", remainder=" + getRemainder() + "} ";
    }
}

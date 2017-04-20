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
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class HqlEntityInstance extends HqlDataInstance<JpaEntityDefinition> {

    public HqlEntityInstance(String hqlPath, JpaEntityDefinition jpaDefinition, HqlDataInstance parentPropertyPath) {
        super(hqlPath, jpaDefinition, parentPropertyPath);
    }

    public HqlEntityInstance narrowFor(@NotNull JpaEntityDefinition overridingDefinition) {
        if (overridingDefinition.isAssignableFrom(jpaDefinition)) {
            // nothing to do here
            return this;
        } else if (jpaDefinition.isAssignableFrom(overridingDefinition)) {
            return new HqlEntityInstance(hqlPath, overridingDefinition, parentDataItem);
        } else {
            throw new IllegalStateException("Illegal attempt to narrow entity definition: from " + jpaDefinition +
                    " to " + overridingDefinition + ". These two definitions are not compatible.");
        }
    }

}

/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.resolution;

import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
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

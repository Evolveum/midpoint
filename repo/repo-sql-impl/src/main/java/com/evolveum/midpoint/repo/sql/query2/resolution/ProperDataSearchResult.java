/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.resolution;

import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import org.jetbrains.annotations.NotNull;

/** Data search result with a root entity (from which it starts). */
public class RootedDataSearchResult<TD extends JpaDataNodeDefinition> extends DataSearchResult<TD> {

    /** Entity in which the item was found */
    @NotNull private final JpaEntityDefinition rootEntityDefinition;

    RootedDataSearchResult(@NotNull JpaEntityDefinition rootEntityDefinition, @NotNull DataSearchResult<TD> result) {
        super(result.getLinkDefinition(), result.getRemainder());
        this.rootEntityDefinition = rootEntityDefinition;
    }

    @NotNull
    public JpaEntityDefinition getRootEntityDefinition() {
        return rootEntityDefinition;
    }

    @Override
    public String toString() {
        return "RootedDataSearchResult{" +
                "entity=" + rootEntityDefinition + ", item=" + getLinkDefinition() + ", remainder=" + getRemainder() + "}";
    }
}

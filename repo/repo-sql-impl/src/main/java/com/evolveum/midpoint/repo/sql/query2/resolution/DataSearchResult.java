/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class DataSearchResult<T extends JpaDataNodeDefinition> {
    @NotNull private final JpaLinkDefinition<T> linkDefinition;
    @NotNull private final ItemPath remainder;                             // what has remained unresolved of the original search path

    public DataSearchResult(@NotNull JpaLinkDefinition<T> linkDefinition, @NotNull ItemPath remainder) {
        this.linkDefinition = linkDefinition;
        this.remainder = remainder;
    }

    @NotNull
    public JpaLinkDefinition<T> getLinkDefinition() {
        return linkDefinition;
    }

    @NotNull
    public ItemPath getRemainder() {
        return remainder;
    }

    public boolean isComplete() {
        return remainder.isEmpty();
    }

    public T getTargetDefinition() {
        return linkDefinition.getTargetDefinition();
    }

    @Override
    public String toString() {
        return "DataSearchResult{" +
                "linkDefinition=" + linkDefinition +
                ", remainder=" + remainder +
                '}';
    }
}

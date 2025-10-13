/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.resolution;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaLinkDefinition;

/**
 * @param <TD> target node definition
 */
public class DataSearchResult<TD extends JpaDataNodeDefinition> {

    /** Currently last link in the chain. */
    @NotNull private final JpaLinkDefinition<TD> linkDefinition;

    /** What has remained unresolved of the original search path. */
    @NotNull private final ItemPath remainder;

    public DataSearchResult(@NotNull JpaLinkDefinition<TD> linkDefinition, @NotNull ItemPath remainder) {
        this.linkDefinition = linkDefinition;
        this.remainder = remainder;
    }

    @NotNull
    public JpaLinkDefinition<TD> getLinkDefinition() {
        return linkDefinition;
    }

    @NotNull
    public ItemPath getRemainder() {
        return remainder;
    }

    public boolean isComplete() {
        return remainder.isEmpty();
    }

    public TD getTargetDefinition() {
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

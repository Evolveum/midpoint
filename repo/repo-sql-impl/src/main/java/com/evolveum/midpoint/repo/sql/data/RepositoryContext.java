/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.schema.RelationRegistry;
import org.jetbrains.annotations.NotNull;

public class RepositoryContext {
    @NotNull public final RepositoryService repositoryService;
    @NotNull public final PrismContext prismContext;
    @NotNull public final RelationRegistry relationRegistry;
    @NotNull public final ExtItemDictionary extItemDictionary;
    @NotNull public final SqlRepositoryConfiguration configuration;

    public RepositoryContext(@NotNull RepositoryService repositoryService, @NotNull PrismContext prismContext,
            @NotNull RelationRegistry relationRegistry, @NotNull ExtItemDictionary extItemDictionary,
            @NotNull SqlRepositoryConfiguration configuration) {
        this.repositoryService = repositoryService;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
        this.extItemDictionary = extItemDictionary;
        this.configuration = configuration;
    }
}

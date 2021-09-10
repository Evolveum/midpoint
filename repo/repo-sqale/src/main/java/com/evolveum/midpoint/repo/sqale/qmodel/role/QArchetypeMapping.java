/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

/**
 * Mapping between {@link QArchetype} and {@link ArchetypeType}.
 */
public class QArchetypeMapping
        extends QAbstractRoleMapping<ArchetypeType, QArchetype, MArchetype> {

    public static final String DEFAULT_ALIAS_NAME = "arch";
    private static QArchetypeMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QArchetypeMapping initArchetypeMapping(
            @NotNull SqaleRepoContext repositoryContext) {
        instance = new QArchetypeMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QArchetypeMapping getArchetypeMapping() {
        return Objects.requireNonNull(instance);
    }

    private QArchetypeMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QArchetype.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ArchetypeType.class, QArchetype.class, repositoryContext);
    }

    @Override
    protected QArchetype newAliasInstance(String alias) {
        return new QArchetype(alias);
    }

    @Override
    public MArchetype newRowObject() {
        return new MArchetype();
    }
}

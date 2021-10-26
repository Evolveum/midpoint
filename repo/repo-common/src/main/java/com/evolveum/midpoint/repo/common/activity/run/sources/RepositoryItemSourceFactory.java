/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.sources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

/**
 * Creates an appropriate {@link SearchableItemSource} at the repository level.
 */
@Component
public class RepositoryItemSourceFactory {

    @Autowired private RepoObjectSource repoObjectSource;
    @Autowired private RepoAuditItemSource repoAuditItemSource;
    @Autowired private RepoContainerableItemSource repoContainerableItemSource;

    public <C extends Containerable> SearchableItemSource getItemSourceFor(Class<C> type) {
        if (MiscSchemaUtil.isObjectType(type)) {
            return repoObjectSource;
        } else if (MiscSchemaUtil.isAuditType(type)) {
            return repoAuditItemSource;
        } else {
            return repoContainerableItemSource;
        }
    }
}

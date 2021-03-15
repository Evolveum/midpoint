/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sql.helpers.ObjectUpdater;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import org.hibernate.Session;

/**
 * TODO
 */
class UpdateContext {

    final ObjectDeltaUpdater beans;
    final RepoModifyOptions options;
    final PrismIdentifierGenerator idGenerator;
    final Session session;
    final ObjectUpdater.AttemptContext attemptContext;

    boolean shadowPendingOperationModified;

    UpdateContext(ObjectDeltaUpdater beans, RepoModifyOptions options, PrismIdentifierGenerator idGenerator, Session session,
            ObjectUpdater.AttemptContext attemptContext) {
        this.beans = beans;
        this.options = options;
        this.idGenerator = idGenerator;
        this.session = session;
        this.attemptContext = attemptContext;
    }
}

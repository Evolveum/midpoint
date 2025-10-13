/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.delta.ChangeType;

public interface RepositoryOperationResult {

    ChangeType getChangeType();

}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.delta.ChangeType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public interface RepositoryOperationResult {

    ChangeType getChangeType();

    /**
     * Returns the object class name for the shadow that was created, modified, or deleted;
     * `null` if not known (the repository did not provide the information) or not applicable (object is not a shadow).
     */
    @Nullable QName getShadowObjectClassName();
}

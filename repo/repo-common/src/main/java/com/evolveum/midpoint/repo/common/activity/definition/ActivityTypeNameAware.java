/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/** Any object that needs to be aware of the activity type name. */
public interface ActivityTypeNameAware {

    /**
     * Returns the activity type name, currently the same as the name of the respective configuration item e.g. `c:recomputation`.
     */
    @NotNull QName getActivityTypeName();
}

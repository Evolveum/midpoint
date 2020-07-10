/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RObject;

/**
 * Abstract superclass for all Update classes.
 */
abstract class BaseUpdate {

    final RObject object;
    final ItemDelta<?, ?> delta;
    final UpdateContext ctx;
    final ObjectDeltaUpdater beans;

    BaseUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        this.object = object;
        this.delta = delta;
        this.ctx = ctx;
        this.beans = ctx.beans;
    }
}

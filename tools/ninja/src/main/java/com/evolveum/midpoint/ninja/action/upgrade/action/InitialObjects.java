/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;

public class InitialObjects extends Action<InitialObjectsOptions, ActionResult<InitialObjectsResult>> {

    @Override
    public String getOperationName() {
        return "initial objects";
    }

    @Override
    public ActionResult<InitialObjectsResult> execute() throws Exception {
        // todo implement
        return null;
    }
}

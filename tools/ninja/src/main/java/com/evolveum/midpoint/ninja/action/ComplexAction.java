/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.util.ConsoleFormat;

public abstract class ComplexAction<O, R> extends Action<O, R> {

    protected <O, T> T executeAction(Action<O, T> action, O options) throws Exception {
        action.init(context, options);

        log.info("");
        log.info(ConsoleFormat.formatActionStartMessage(action));
        log.info("");

        return action.execute();
    }
}

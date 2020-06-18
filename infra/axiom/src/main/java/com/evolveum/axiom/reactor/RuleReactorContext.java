/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.Collection;

public abstract class RuleReactorContext<E extends Exception,C, A extends Action<E>, R extends Rule<C,A>> extends BaseReactorContext<E, A>{

    protected abstract Collection<R> rulesFor(C context);

    protected void addActionsFor(C context) {
        Collection<R> rules = rulesFor(context);
        for (R rule : rules) {
            if(rule.applicableTo(context)) {
                addOutstanding(rule.applyTo(context));
            }
        }
    }
}

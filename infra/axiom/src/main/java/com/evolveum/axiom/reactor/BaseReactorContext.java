/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public abstract class BaseReactorContext<E extends Exception, A extends Action<E>> {


    private Collection<A> outstanding = new ArrayList<>();

    public void compute() throws E {
        boolean anyActionCompleted = false;
        do {
            anyActionCompleted = false;
            Collection<A> toCheck = takeOutstanding();
            Iterator<A> iterator = toCheck.iterator();
            while (iterator.hasNext()) {
                A action = iterator.next();
                try {
                    if (action.canApply()) {
                            action.apply();
                    }
                } catch (Exception e) {
                    action.fail(e);
                }
                if(action.successful()) {
                    iterator.remove();
                    anyActionCompleted = true;
                }
            }
            // We add not finished items back to outstanding
            addOutstanding(toCheck);
        } while (anyActionCompleted);

        if (!outstanding.isEmpty()) {
            failOutstanding(outstanding);
        }
    }

    protected void fail(A action, Exception e) throws E {
        action.fail(e);
    }

    protected void failOutstanding(Collection<A> outstanding) throws E {
        E common = createException();
        for (A action : outstanding) {
            if(!action.canApply()) {
                addSuppresed(common, action);

            }
        }
        throw common;
    }

    protected void addOutstanding(A action) {
        outstanding.add(action);
    }

    protected void addOutstanding(Collection<A> action) {
        outstanding.addAll(action);
    }

    protected void addSuppresed(E common, A action) {
        Optional<E> error = action.error();
        if(error.isPresent()) {
            common.addSuppressed(error.get());
        }
    }

    protected abstract E createException();

    private Collection<A> takeOutstanding() {
        Collection<A> ret = outstanding;
        outstanding = new ArrayList<>();
        return ret;
    }


}

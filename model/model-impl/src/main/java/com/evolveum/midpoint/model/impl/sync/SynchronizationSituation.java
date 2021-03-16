/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 *
 * @author lazyman
 *
 */
public class SynchronizationSituation<F extends FocusType> {

    private final F linkedOwner;
    private final F correlatedOwner;
    private final SynchronizationSituationType situation;

    public SynchronizationSituation(F linkedOwner, F correlatedOwner, SynchronizationSituationType situation) {
        Validate.notNull(situation, "Synchronization situation must not be null.");
        this.linkedOwner = linkedOwner;
        this.correlatedOwner = correlatedOwner;
        this.situation = situation;
    }

    public F getLinkedOwner() {
        return linkedOwner;
    }

    public F getCorrelatedOwner() {
        return correlatedOwner;
    }

    public SynchronizationSituationType getSituation() {
        return situation;
    }
}

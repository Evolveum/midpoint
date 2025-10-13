/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync;

import org.apache.commons.lang3.Validate;

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

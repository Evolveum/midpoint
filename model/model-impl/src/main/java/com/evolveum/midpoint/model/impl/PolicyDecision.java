/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl;

/**
 * Describes what the policy "decides" about a specific account.
 *
 * @author Radovan Semancik
 *
 */
public enum PolicyDecision {

    ADD,
    DELETE,
    KEEP,
    UNLINK;

}

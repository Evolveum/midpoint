/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

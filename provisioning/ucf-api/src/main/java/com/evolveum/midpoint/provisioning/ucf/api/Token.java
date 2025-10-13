/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface Token {

    /**
     * May not be human readable. Must be deserializable.
     * @return
     */
    public String serialize();

    /**
     * Must be human readable. May not be deserializable.
     * @return
     */
    public String toString();

}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

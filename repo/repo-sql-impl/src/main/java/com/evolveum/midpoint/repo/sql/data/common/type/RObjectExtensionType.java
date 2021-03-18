/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.type;

/**
 * @author lazyman
 */
public enum RObjectExtensionType {

    /** Type of extension value for all objects except for Shadow. */
    EXTENSION,

    /** Type of extension value for shadow attributes. */
    ATTRIBUTES
}

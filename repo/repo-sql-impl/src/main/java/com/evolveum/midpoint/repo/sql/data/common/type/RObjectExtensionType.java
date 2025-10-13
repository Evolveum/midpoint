/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

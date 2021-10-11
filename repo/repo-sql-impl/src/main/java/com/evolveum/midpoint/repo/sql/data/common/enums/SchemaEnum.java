/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

/**
 * Interface that marks enumerated entities which are used as placeholders for schema
 * enums (e.g. used in common.xsd and others). This simplifies translation between them.
 *
 * @author lazyman
 */
@FunctionalInterface
public interface SchemaEnum<C> {

    C getSchemaValue();
}

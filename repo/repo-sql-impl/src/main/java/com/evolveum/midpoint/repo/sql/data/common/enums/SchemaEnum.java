/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

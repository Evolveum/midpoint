/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.MySQL57InnoDBDialect;

import java.sql.Types;

/**
 * @author lazyman
 */
public class MidPointMySQLDialect extends MySQL57InnoDBDialect {

    public MidPointMySQLDialect() {
        registerColumnType(Types.BOOLEAN, "bit");
    }

    @Override
    public String getTableTypeString() {
        return " DEFAULT CHARACTER SET utf8 COLLATE utf8_bin" + super.getTableTypeString();
    }
}


/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.google.common.base.CaseFormat;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointPhysicalNamingStrategy.class);

    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        String name = identifier.getText();
        if (name.startsWith("m_") || "hibernate_sequence".equals(name)) {
            return identifier;
        }

        name = name.substring(1);
        name = "m_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
        name = RUtil.fixDBSchemaObjectNameLength(name);

        Identifier i = new Identifier(name, identifier.isQuoted());

        LOGGER.trace("toPhysicalTableName {} -> {}", identifier, i);

        return i;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        Identifier i = super.toPhysicalColumnName(name, context);

        LOGGER.trace("toPhysicalColumnName {} -> {}", name, i);

        return i;
    }
}

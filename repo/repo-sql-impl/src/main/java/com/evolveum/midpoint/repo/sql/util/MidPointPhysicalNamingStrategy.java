/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            LOGGER.trace("toPhysicalTableName {} {}", identifier, identifier);
            return identifier;
        }

        name = name.substring(1);
        name = "m_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
        name = RUtil.fixDBSchemaObjectNameLength(name);

        Identifier i = new Identifier(name, identifier.isQuoted());
        LOGGER.trace("toPhysicalTableName {} {}", identifier, i);
        return i;
    }
}

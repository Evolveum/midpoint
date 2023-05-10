/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;

public class DatabaseSchemaStep implements UpgradeStep<Void> {

    private Connection connection;

    @Override
    public Void execute() throws Exception {
        // 1/ initialize DB connection, using midpoint home?
        // 2/ check current state of DB. Is it previous feature release (4.6) or LTS (4.4)
        // 3/ pick proper scripts
        // 4/ execute upgrade scripts

        try {
            init();

            upgrade();
        } finally {
            destroy();
        }

        return null;
    }

    private void init() {
        // todo
    }

    private void upgrade() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        //        populator.addScript(); // todo add scripts
        populator.populate(connection);
    }

    private void destroy() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception ex) {
            // todo handle properly
            ex.printStackTrace();
        }
    }
}

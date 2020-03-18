/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import org.hibernate.Session;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "../../../../../ctx-test-datasource.xml" })
public class DataSourceTest extends BaseSQLRepoTest {

    @Test
    public void testUsingDataSource() {
        Session session = getFactory().openSession();
        session.beginTransaction();

        session.getTransaction().commit();
        session.close();
    }
}

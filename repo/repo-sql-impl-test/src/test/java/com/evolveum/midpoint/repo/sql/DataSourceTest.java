/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Test of data source defined by JNDI - either hardcoded for H2 or, for production DB,
 * using properties in a file located using {@code config} property.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test-datasource.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DataSourceTest extends BaseSQLRepoTest {

    @Test
    public void testUsingDataSource() {
        Session session = getFactory().openSession();
        session.beginTransaction();

        session.getTransaction().commit();
        session.close();
    }
}

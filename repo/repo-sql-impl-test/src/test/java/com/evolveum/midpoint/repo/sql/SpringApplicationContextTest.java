/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class SpringApplicationContextTest extends BaseSQLRepoTest {

    @Autowired
    private LocalSessionFactoryBean sessionFactory;

    @Test
    public void initApplicationContext() {
        assertNotNull(repositoryService);
        assertNotNull(sessionFactory);
    }
}

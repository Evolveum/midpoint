/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.sql.util.MidPointMySQLDialect;
import com.evolveum.midpoint.repo.sql.util.MidPointNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPostgreSQLDialect;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;
import com.evolveum.midpoint.util.ClassPathUtil;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SpringApplicationContextTest extends BaseSQLRepoTest {

    @Autowired
    private LocalSessionFactoryBean sessionFactory;

    @Test
    public void initApplicationContext() throws Exception {
        assertNotNull(repositoryService);

        assertNotNull(sessionFactory);

        createSQLSchema("./target/h2-schema.sql", H2Dialect.class.getName());
        createSQLSchema("./target/sqlserver-schema.sql", UnicodeSQLServer2008Dialect.class.getName());
        createSQLSchema("./target/mysql-schema.sql", MidPointMySQLDialect.class.getName());
        createSQLSchema("./target/oracle-schema.sql", Oracle10gDialect.class.getName());
        createSQLSchema("./target/postgresql-schema.sql", MidPointPostgreSQLDialect.class.getName());
    }

    private void createSQLSchema(String fileName, String dialect) throws Exception {
        org.hibernate.cfg.Configuration configuration = new Configuration();
        configuration.setNamingStrategy(new MidPointNamingStrategy());
        configuration.setProperties(sessionFactory.getHibernateProperties());
        sessionFactory.getHibernateProperties().setProperty("hibernate.dialect", dialect);

        System.out.println("Dialect: " + sessionFactory.getHibernateProperties().getProperty("hibernate.dialect"));

        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.container", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.any", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.embedded", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.enums", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.id", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.other", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.type", configuration);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.audit", configuration);
//        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.poc", configuration);

        configuration.addPackage("com.evolveum.midpoint.repo.sql.type");

        SchemaExport export = new SchemaExport(configuration);
        export.setOutputFile(fileName);
        export.setDelimiter(";");
        export.execute(true, false, false, true);
    }

    private void addAnnotatedClasses(String packageName, Configuration configuration) throws ClassNotFoundException {
        Set<Class> classes = ClassPathUtil.listClasses(packageName);
        for (Class clazz : classes) {
            configuration.addAnnotatedClass(clazz);
        }
    }
}

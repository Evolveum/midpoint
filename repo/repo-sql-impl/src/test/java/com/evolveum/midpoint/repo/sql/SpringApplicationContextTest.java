/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.testing.BaseSQLRepoTest;
import com.evolveum.midpoint.repo.sql.util.MidPointNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../ctx-sql-server-mode-test.xml",
        "../../../../../ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "../../../../../ctx-configuration-sql-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SpringApplicationContextTest extends BaseSQLRepoTest {

    @Autowired
    private LocalSessionFactoryBean sessionFactory;

    @Test
    public void initApplicationContext() throws Exception {
        assertNotNull(repositoryService);

        assertNotNull(sessionFactory);

        org.hibernate.cfg.Configuration configuration = new Configuration();
        configuration.setNamingStrategy(new MidPointNamingStrategy());
        configuration.setProperties(sessionFactory.getHibernateProperties());
        System.out.println("Dialect: " + sessionFactory.getHibernateProperties().getProperty("hibernate.dialect"));

        addAnnotatedClasses("com/evolveum/midpoint/repo/sql/data/common", configuration);
        addAnnotatedClasses("com/evolveum/midpoint/repo/sql/data/audit", configuration);

        configuration.addPackage("com.evolveum.midpoint.repo.sql.type");

        SchemaExport export = new SchemaExport(configuration);
        export.setOutputFile("./target/schema.sql");
        export.setDelimiter(";");
        export.execute(true, false, false, true);
    }

    private void addAnnotatedClasses(String packagePath, Configuration configuration) throws ClassNotFoundException {
        File parent = new File("./src/main/java");
        File dir = new File(parent, packagePath);
        File[] files = dir.listFiles();

        String packageName = packagePath.replaceAll("/", ".") + ".";

        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith("java")) {
                continue;
            }
            String className = packageName + file.getName().substring(0, file.getName().length() - 5);
            configuration.addAnnotatedClass(Class.forName(className));
        }
    }
}

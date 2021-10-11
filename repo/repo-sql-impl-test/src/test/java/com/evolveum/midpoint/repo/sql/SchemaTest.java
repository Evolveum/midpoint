/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.ClassPathUtil;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchemaTest extends AbstractUnitTest {

    @Test
    public void generateSchemas() {
        createSQLSchema("./target/h2-schema.sql", H2Dialect.class.getName());
        createSQLSchema("./target/sqlserver-schema.sql", UnicodeSQLServer2008Dialect.class.getName());
        createSQLSchema("./target/mysql-schema.sql", MidPointMySQLDialect.class.getName());
        createSQLSchema("./target/oracle-schema.sql", MidPointOracleDialect.class.getName());
        createSQLSchema("./target/postgresql-schema.sql", MidPointPostgreSQLDialect.class.getName());
    }

    private void createSQLSchema(String fileName, String dialect) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        MetadataSources metadata = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .applySetting("hibernate.implicit_naming_strategy", new MidPointImplicitNamingStrategy())
                        .applySetting("hibernate.physical_naming_strategy", new MidPointPhysicalNamingStrategy())
                        .applySetting("hibernate.dialect", dialect)
                        .build());

        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.container", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.any", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.embedded", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.enums", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.id", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.other", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.common.type", metadata);
        addAnnotatedClasses("com.evolveum.midpoint.repo.sql.data.audit", metadata);

        metadata.addPackage("com.evolveum.midpoint.repo.sql.type");

        SchemaExport export = new SchemaExport();
        export.setOutputFile(fileName);
        export.setDelimiter(";");
        export.execute(EnumSet.of(TargetType.SCRIPT), SchemaExport.Action.CREATE, metadata.buildMetadata());
    }

    private void addAnnotatedClasses(String packageName, MetadataSources metadata) {
        Set<Class<?>> classes = ClassPathUtil.listClasses(packageName);
        for (Class<?> clazz : classes) {
            metadata.addAnnotatedClass(clazz);
        }
    }
}

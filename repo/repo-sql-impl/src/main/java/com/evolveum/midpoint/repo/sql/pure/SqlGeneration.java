package com.evolveum.midpoint.repo.sql.pure;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.codegen.MetaDataExporter;
import org.h2.Driver;

// TODO MID-6319 must go after done
@Deprecated
public class SqlGeneration {
    public static void main(String[] args) throws SQLException {
        Driver.load();
        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:5437/midpoint", "sa", "");
        MetaDataExporter exporter = new MetaDataExporter();
        exporter.setPackageName("com.myproject.mydomain");
        exporter.setTargetFolder(new File("target/generated-sources/java"));
        exporter.export(conn.getMetaData());
    }

    private void depUnusedFix() {
        SQLTemplates templates = new H2Templates();
        Configuration configuration = new Configuration(templates);
        new SQLQueryFactory(templates, null);
    }
}

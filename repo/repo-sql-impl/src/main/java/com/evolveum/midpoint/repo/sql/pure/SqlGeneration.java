package com.evolveum.midpoint.repo.sql.pure;

import com.myproject.mydomain.QMAuditEvent;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;

// TODO MID-6319 must go after done
@Deprecated
public class SqlGeneration {

    public static void main(String[] args) {
        /* this requires querydsl-sql-codegen
        Driver.load();
        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:5437/midpoint", "sa", "");
        MetaDataExporter exporter = new MetaDataExporter();
        exporter.setPackageName("com.myproject.mydomain");
        exporter.setTargetFolder(new File("target/generated-sources/java"));
        exporter.export(conn.getMetaData());
        */

        System.out.println(QMAuditEvent.mAuditEvent);
        System.out.println(QMAuditEvent.mAuditEvent.getColumns());
        System.out.println(QMAuditEvent.mAuditEvent.getAnnotatedElement());
        System.out.println(QMAuditEvent.mAuditEvent.getForeignKeys());
        System.out.println(QMAuditEvent.mAuditEvent.getInverseForeignKeys());

    }

    private void depUnusedFix() {
        SQLTemplates templates = new H2Templates();
        Configuration configuration = new Configuration(templates);
        new SQLQueryFactory(templates, null);
    }
}

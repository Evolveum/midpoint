/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Common supertype for SQL-based repository-like services.
 */
public abstract class SqlBaseService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlBaseService.class);

    public static final String IMPLEMENTATION_SHORT_NAME = "SQL";

    private static final String IMPLEMENTATION_DESCRIPTION =
            "Implementation that stores data in generic relational (SQL) databases."
                    + " It is using ORM (hibernate) on top of JDBC to access the database.";
    private static final String DETAILS_TRANSACTION_ISOLATION = "transactionIsolation";
    private static final String DETAILS_CLIENT_INFO = "clientInfo.";
    private static final String DETAILS_DATA_SOURCE = "dataSource";
    private static final String DETAILS_HIBERNATE_DIALECT = "hibernateDialect";
    private static final String DETAILS_HIBERNATE_HBM_2_DDL = "hibernateHbm2ddl";

    protected final BaseHelper baseHelper;

    @Autowired private SqlPerformanceMonitorsCollection monitorsCollection;

    private SqlPerformanceMonitorImpl performanceMonitor;

    public SqlBaseService(BaseHelper baseHelper) {
        this.baseHelper = baseHelper;
    }

    public abstract SqlRepositoryConfiguration sqlConfiguration();

    public synchronized SqlPerformanceMonitorImpl getPerformanceMonitor() {
        if (performanceMonitor == null) {
            SqlRepositoryConfiguration config = sqlConfiguration();
            performanceMonitor = new SqlPerformanceMonitorImpl(
                    config.getPerformanceStatisticsLevel(), config.getPerformanceStatisticsFile());
            monitorsCollection.register(performanceMonitor);
        }

        return performanceMonitor;
    }

    public void destroy() {
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
            monitorsCollection.deregister(performanceMonitor);
            performanceMonitor = null;
        }
    }

    public @NotNull RepositoryDiag getRepositoryDiag() {
        LOGGER.debug("Getting repository diagnostics.");

        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(IMPLEMENTATION_SHORT_NAME);
        diag.setImplementationDescription(IMPLEMENTATION_DESCRIPTION);

        SqlRepositoryConfiguration config = sqlConfiguration();

        //todo improve, find and use real values (which are used by sessionFactory) MID-1219
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        diag.setH2(config.getDatabaseType() == SupportedDatabase.H2);

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString(DETAILS_DATA_SOURCE, config.getDataSource()));
        details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, config.getHibernateDialect()));
        details.add(new LabeledString(DETAILS_HIBERNATE_HBM_2_DDL, config.getHibernateHbm2ddl()));

        readDetailsFromConnection(diag, config);

        details.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel()));

        return diag;
    }

    public boolean isGenericNonH2() {
        return sqlConfiguration().getDatabaseType() != SupportedDatabase.H2;
    }

    private void readDetailsFromConnection(RepositoryDiag diag, final SqlRepositoryConfiguration config) {
        final List<LabeledString> details = diag.getAdditionalDetails();

        EntityManager em = baseHelper.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> {
                details.add(new LabeledString(DETAILS_TRANSACTION_ISOLATION,
                        getTransactionIsolation(connection, config)));

                Properties info = connection.getClientInfo();
                if (info == null) {
                    return;
                }

                for (String name : info.stringPropertyNames()) {
                    details.add(new LabeledString(DETAILS_CLIENT_INFO + name, info.getProperty(name)));
                }
            });
            em.getTransaction().commit();

            EntityManagerFactory entityManagerFactory = baseHelper.getEntityManagerFactory();
            // TODO THIS WILL NOT NOT WORK
            //xxx
            if (!(entityManagerFactory instanceof SessionFactoryImpl sessionFactoryImpl)) {
                return;
            }
            // we try to override configuration which was read from sql repo configuration with
            // real configuration from em factory
            Dialect dialect = sessionFactoryImpl.getJdbcServices().getDialect();
            if (dialect != null) {
                for (int i = 0; i < details.size(); i++) {
                    if (details.get(i).getLabel().equals(DETAILS_HIBERNATE_DIALECT)) {
                        details.remove(i);
                        break;
                    }
                }
                String dialectClassName = dialect.getClass().getName();
                details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, dialectClassName));
            }
        } catch (Throwable th) {
            //nowhere to report error (no operation result available)
            em.getTransaction().rollback();
        } finally {
            baseHelper.cleanupManagerAndResult(em, null);
        }
    }

    private String getTransactionIsolation(Connection connection, SqlRepositoryConfiguration config) {
        String value = config.getTransactionIsolation() != null ?
                config.getTransactionIsolation().name() + "(read from repo configuration)" : null;

        try {
            switch (connection.getTransactionIsolation()) {
                case Connection.TRANSACTION_NONE:
                    value = "TRANSACTION_NONE (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    value = "TRANSACTION_READ_COMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    value = "TRANSACTION_READ_UNCOMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    value = "TRANSACTION_REPEATABLE_READ (read from connection)";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    value = "TRANSACTION_SERIALIZABLE (read from connection)";
                    break;
                default:
                    value = "Unknown value in connection.";
            }
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
        }

        return value;
    }
}

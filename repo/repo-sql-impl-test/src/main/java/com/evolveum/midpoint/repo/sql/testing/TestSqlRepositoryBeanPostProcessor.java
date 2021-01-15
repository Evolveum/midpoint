/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;

/**
 * @author lazyman
 */
public class TestSqlRepositoryBeanPostProcessor implements BeanPostProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryBeanPostProcessor.class);
    private static final String TRUNCATE_FUNCTION = "cleanupTestDatabase";
    private static final String TRUNCATE_PROCEDURE = "cleanupTestDatabaseProc";

    @Autowired private ApplicationContext context;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ((bean instanceof SqlRepositoryFactory) || (bean instanceof SessionFactory)
                || (bean instanceof SqlRepositoryServiceImpl)) {
            LOGGER.info("Post process: " + bean.getClass().getName());
        }

        if (!(bean instanceof SessionFactory)) {
            return bean;
        }
        LOGGER.info("Postprocessing session factory - removing everything from database if necessary.");

        TestSqlRepositoryFactory factory = context.getBean(TestSqlRepositoryFactory.class);
        //we'll attempt to drop database objects if configuration contains dropIfExists=true and embedded=false
        SqlRepositoryConfiguration config = factory.getConfiguration();
        if (!config.isDropIfExists() || config.isEmbedded()) {
            LOGGER.info("We're not deleting objects from DB, drop if exists=false or embedded=true.");
            return bean;
        }

        LOGGER.info("Deleting objects from database.");

        SessionFactory sessionFactory = (SessionFactory) bean;
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();

            Query query;
            if (useProcedure(factory.getConfiguration())) {
                LOGGER.info("Using truncate procedure.");
                query = session.createNativeQuery("{ call " + TRUNCATE_PROCEDURE + "() }");
                query.executeUpdate();
            } else {
                LOGGER.info("Using truncate function.");
                query = session.createNativeQuery("select " + TRUNCATE_FUNCTION + "();");
                query.uniqueResult();
            }

            session.getTransaction().commit();
        } catch (Exception ex) {
            LOGGER.error("Couldn't cleanup database, reason: " + ex.getMessage(), ex);

            if (session != null && session.isOpen()) {
                Transaction transaction = session.getTransaction();
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
            }
            throw new BeanInitializationException("Couldn't cleanup database, reason: " + ex.getMessage(), ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

        return bean;
    }

    /**
     * This method decides whether function or procedure (oracle, ms sql server)
     * will be used to cleanup testing database.
     */
    private boolean useProcedure(SqlRepositoryConfiguration config) {
        return StringUtils.containsIgnoreCase(config.getHibernateDialect(), "oracle")
                || StringUtils.containsIgnoreCase(config.getHibernateDialect(), "SQLServer");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}

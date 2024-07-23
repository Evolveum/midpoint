/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.sql.util.RUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class TestSqlRepositoryBeanPostProcessor implements BeanPostProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryBeanPostProcessor.class);

    private static final String TRUNCATE_FUNCTION = "cleanupTestDatabase";
    private static final String TRUNCATE_PROCEDURE = "cleanupTestDatabaseProc";

    @Autowired private SqlRepositoryConfiguration repoConfig;

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName)
            throws BeansException {
        if (!(bean instanceof EntityManagerFactory entityManagerFactory)) {
            return bean;
        }
        LOGGER.info("Postprocessing entity manager factory - removing everything from database if necessary.");

        //we'll attempt to drop database objects if configuration contains dropIfExists=true and embedded=false
        if (!repoConfig.isDropIfExists() || repoConfig.isEmbedded()) {
            LOGGER.info("We're not deleting objects from DB, drop if exists=false or embedded=true.");
            return bean;
        }

        LOGGER.info("Deleting objects from database.");

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            Query query;
            if (useProcedure(repoConfig)) {
                LOGGER.info("Using truncate procedure.");
                query = em.createNativeQuery("{ call " + TRUNCATE_PROCEDURE + "() }");
                query.executeUpdate();
            } else {
                LOGGER.info("Using truncate function.");
                query = em.createNativeQuery("select " + TRUNCATE_FUNCTION + "();");
                RUtil.getSingleResultOrNull(query);
            }

            em.getTransaction().commit();
        } catch (Exception ex) {
            LOGGER.error("Couldn't cleanup database, reason: " + ex.getMessage(), ex);

            if (em != null && em.isOpen()) {
                EntityTransaction transaction = em.getTransaction();
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
            }
            throw new BeanInitializationException("Couldn't cleanup database, reason: " + ex.getMessage(), ex);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
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
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName)
            throws BeansException {
        return bean;
    }
}

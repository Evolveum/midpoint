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

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.testng.AssertJUnit;

/**
 * @author lazyman
 */
public class SQLRepositoryBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static final Trace LOGGER = TraceManager.getTrace(SQLRepositoryBeanPostProcessor.class);
    private static final String TRUNCATE_PROCEDURE = "cleanupTestDatabase";
    private static final String BEAN_SESSION_FACTORY = "sessionFactory";

    private ApplicationContext context;

    public SQLRepositoryBeanPostProcessor() {
        LOGGER.info(">>>>>> SQLRepositoryBeanPostProcessor");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ((bean instanceof SqlRepositoryFactory) || (bean instanceof SessionFactory)
                || (bean instanceof SqlRepositoryServiceImpl)) {
            LOGGER.info(">>>>>> post process: " + bean.getClass().getName());
        }

//        if (1==1)return bean;

        if (!(bean instanceof SessionFactory)) {
            return bean;
        }
        LOGGER.info("Postprocessing test sql repository factory initialization.");

//        SqlRepositoryFactory factory = (SqlRepositoryFactory) bean;
        TestSqlRepositoryFactory factory = context.getBean("sqlRepositoryFactory", TestSqlRepositoryFactory.class);
        //we'll attempt to drop database objects if configuration contains dropIfExists=true and embedded=false
        SqlRepositoryConfiguration config = factory.getSqlConfiguration();
        if (!config.isDropIfExists() || config.isEmbedded()) {
            LOGGER.info("We're not deleting objects from DB, drop if exists=false or embedded=true.");
            return bean;
        }

        LOGGER.info("Deleting objects from database.");

//        SessionFactory sessionFactory = context.getBean(BEAN_SESSION_FACTORY, SessionFactory.class);
        SessionFactory sessionFactory = (SessionFactory) bean;
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();

            Query query = session.createSQLQuery("select " + TRUNCATE_PROCEDURE + "();");
            query.uniqueResult();

            session.getTransaction().commit();
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw new BeanInitializationException("Couldn't delete objects from database, reason: " + ex.getMessage(), ex);
        } finally {
            session.close();
        }

        testDatabase(sessionFactory);

        return bean;
    }

    private void testDatabase(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();
            for (RContainerType type : RContainerType.values()) {
                long count = (Long) session.createQuery("select count(*) from " + type.getClazz().getSimpleName()).uniqueResult();
                LOGGER.info(">>> " + type.getClazz().getSimpleName() + " " + count);
                AssertJUnit.assertEquals(0L, count);
            }
            session.getTransaction().commit();
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw new BeanInitializationException(ex.getMessage(), ex);
        } finally {
            session.close();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}

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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;

/**
 * @author lazyman
 */
public class CompositeDataSource implements ApplicationContextAware {

    private static final Trace LOGGER = TraceManager.getTrace(CompositeDataSource.class);

    private static final String DATA_SOURCE_C3P0 = "c3p0DataSource";
    private static final String DATA_SOURCE_JNDI = "jndiDataSource";

    //for testing, todo remove and fix this by bean aliasing and context xml cleanup...
    private boolean testing;
    private static final String DATA_SOURCE_TEST_C3P0 = "testC3p0DataSource";
    private static final String DATA_SOURCE_TEST_JNDI = "testJndiDataSource";

    private ApplicationContext context;
    private SqlRepositoryConfiguration configuration;

    public SqlRepositoryConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SqlRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    public boolean isTesting() {
        return testing;
    }

    public void setTesting(boolean testing) {
        this.testing = testing;
    }

    public DataSource getDataSource() throws RepositoryServiceFactoryException {
        LOGGER.info("Loading datasource.");
        if (configuration == null) {
            throw new RepositoryServiceFactoryException("SQL configuration is null, couldn't create datasource.");
        }

        String beanName;
        if (StringUtils.isNotEmpty(configuration.getDataSource())) {
            LOGGER.info("JDNI datasource present in configuration, looking for '{}'.",
                    new Object[]{configuration.getDataSource()});
            beanName = isTesting() ? DATA_SOURCE_TEST_JNDI : DATA_SOURCE_JNDI;
        } else {
            LOGGER.info("Constructing default C3P0 datasource with connection pooling.");
            beanName = isTesting() ? DATA_SOURCE_TEST_C3P0 : DATA_SOURCE_C3P0;
        }

        return context.getBean(beanName, DataSource.class);
    }
}

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
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class SqlRepositoryFactory implements RepositoryServiceFactory {

    private Configuration configuration;

    @Override
    public void destroy() throws RepositoryServiceFactoryException {
        //todo implement
    }

    @Override
    public void init() throws RepositoryServiceFactoryException {
        //todo implement
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        return new SqlRepositoryServiceImpl();
    }

    @Override
    public void setConfiguration(Configuration config) {
        Validate.notNull(config, "Configuration must not be null.");
        this.configuration = config;
    }

    @Override
    public String getComponentId() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return configuration;
    }
}

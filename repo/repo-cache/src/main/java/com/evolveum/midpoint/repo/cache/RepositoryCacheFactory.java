/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import org.apache.commons.configuration.Configuration;

/**
 * @author lazyman
 */
public class RepositoryCacheFactory implements RepositoryServiceFactory {

    @Override
    public void destroy() throws RepositoryServiceFactoryException {
    }

    @Override
    public void init(Configuration configuration) throws RepositoryServiceFactoryException {
    }

    @Override
    public void destroyService(RepositoryService service) throws RepositoryServiceFactoryException {
        if (!(service instanceof RepositoryCache)) {
            throw new RepositoryServiceFactoryException("Service '" + service.getClass().getName()
                    + "' is not instance of '" + RepositoryCache.class.getName() + "'.");
        }

        RepositoryCache.destroy();
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        RepositoryCache.init();
        return new RepositoryCache();
    }
}

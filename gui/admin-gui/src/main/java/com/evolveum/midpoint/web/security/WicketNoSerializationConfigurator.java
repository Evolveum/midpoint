/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import org.apache.wicket.Application;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.page.IPageManager;
import org.apache.wicket.page.PageManager;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.pageStore.InMemoryPageStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wicket.no-serialization.enabled", havingValue = "true")
public class WicketNoSerializationConfigurator implements WicketConfigurator {

    @Value("${wicket.no-serialization.max-pages:1}")
    private int maxPages;

    @Override
    public void configure(Application application) {
        application.setPageManagerProvider(new NoSerializationPageManagerProvider(application));
    }

    private class NoSerializationPageManagerProvider extends DefaultPageManagerProvider {

        public NoSerializationPageManagerProvider(Application application) {
            super(application);
        }

        @Override
        public IPageManager get() {
            IPageStore store = newPersistentStore();
            store = newCachingStore(store);
            store = newRequestStore(store);
            return new PageManager(store);
        }

        protected IPageStore newPersistentStore() {
            return new InMemoryPageStore(application.getName(), maxPages);
        }
    }
}

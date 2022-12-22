/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * Used by test code to obtain Spring beans when auto-wiring is not available.
 */
@SuppressWarnings("WeakerAccess")
public class TestSpringBeans {

    /** Context used to obtain Spring beans. */
    private static ApplicationContext context;

    /**
     * Because of some wiring issues experienced, the {@link #context} field is initialized statically
     * from {@link AbstractIntegrationTest#initSystem()}.
     */
    public static void setApplicationContext(@NotNull ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    public static @NotNull ApplicationContext getApplicationContext() {
        return Objects.requireNonNull(context, "Spring application context could not be determined.");
    }

    public static @NotNull <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    public static @NotNull RepositoryService getCacheRepositoryService() {
        return getApplicationContext()
                .getBean("cacheRepositoryService", RepositoryService.class);
    }

    public static @NotNull RepoSimpleObjectResolver getRepoSimpleObjectResolver() {
        return getApplicationContext()
                .getBean(RepoSimpleObjectResolver.class);
    }

    public static @NotNull MidpointConfiguration getMidpointConfiguration() {
        return getBean(MidpointConfiguration.class);
    }

    /**
     * We assume that only a single instance of the object importer exists.
     *
     * TODO What about tests running below AbstractModelIntegrationTest level?
     */
    public static @NotNull ObjectImporter getObjectImporter() {
        return getApplicationContext()
                .getBean(ObjectImporter.class);
    }
}

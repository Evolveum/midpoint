/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;

import com.evolveum.midpoint.task.api.TaskManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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

    public static @NotNull TaskManager getTaskManager() {
        return getApplicationContext()
                .getBean(TaskManager.class);
    }

    public static @NotNull RepoSimpleObjectResolver getRepoSimpleObjectResolver() {
        return getApplicationContext()
                .getBean(RepoSimpleObjectResolver.class);
    }

    public static @NotNull MidpointConfiguration getMidpointConfiguration() {
        return getBean(MidpointConfiguration.class);
    }

    /**
     * We assume that only a single instance of the object importer bean exists.
     * If not found, a simple default implementation is returned.
     */
    public static @NotNull ObjectImporter getObjectImporter() {
        try {
            return getApplicationContext()
                    .getBean(ObjectImporter.class);
        } catch (NoSuchBeanDefinitionException e) {
            return new SimpleObjectImporterImpl(getCacheRepositoryService(), getTaskManager());
        }
    }
}

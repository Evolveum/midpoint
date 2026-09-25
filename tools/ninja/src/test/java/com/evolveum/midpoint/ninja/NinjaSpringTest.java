/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja;

import java.util.List;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryBeanConfig;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;

/**
 * Base class for Ninja tests that need Spring context, e.g. for repository state initialization.
 */
public abstract class NinjaSpringTest extends AbstractSpringTest implements InfraTestMixin, NinjaTestMixin {

    @Qualifier("repositoryService")
    @Autowired
    protected RepositoryService repository;

    @Autowired
    protected DataSource repositoryDataSource;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected PrismContext prismContext;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws Exception {
        setupMidpointHome();
    }

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextBeforeTestClass" })
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        super.springTestContextPrepareTestInstance();

        clearMidpointTestDatabase(applicationContext);

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Override
    public MainResult executeTest(
            @Nullable StreamValidator validateOut, @Nullable StreamValidator validateErr, @NotNull String... args)
            throws Exception {
        try {
            return NinjaTestMixin.super.executeTest(validateOut, validateErr, args);
        } finally {
            restoreSqaleMappings();
        }
    }

    @Override
    public <O, R, A extends Action<O, R>> R executeAction(
            @NotNull Class<A> actionClass, @NotNull O actionOptions, @NotNull List<Object> allOptions,
            @Nullable StreamValidator validateOut, @Nullable StreamValidator validateErr)
            throws Exception {
        try {
            return NinjaTestMixin.super.executeAction(actionClass, actionOptions, allOptions, validateOut,
                    validateErr);
        } finally {
            restoreSqaleMappings();
        }
    }

    private void restoreSqaleMappings() {
        if (repository instanceof SqaleRepositoryService sqaleRepositoryService) {
            SqaleRepositoryBeanConfig.registerMappings(
                    new QueryModelMappingRegistry(), sqaleRepositoryService.sqlRepoContext());
        }
    }
}

/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.AssertJUnit.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests short-lived count caching i n {@link SelectableBeanDataProvider}.
 */
public class SelectableBeanDataProviderCountCacheTest extends AbstractGuiUnitTest {

    private static final ModelInteractionService MODEL_INTERACTION_SERVICE = (ModelInteractionService)
            Proxy.newProxyInstance(
                    ModelInteractionService.class.getClassLoader(),
                    new Class<?>[] { ModelInteractionService.class },
                    (proxy, method, args) -> "getCompiledGuiProfile".equals(method.getName())
                            ? new CompiledGuiProfile()
                            : null);

    private WicketTester wicketTester;

    @BeforeClass
    public void initWicket() {
        wicketTester = new WicketTester();
    }

    @AfterClass(alwaysRun = true)
    public void destroyWicket() {
        if (wicketTester != null) {
            wicketTester.destroy();
            wicketTester = null;
        }
    }

    /**
     * Simulates Wicket asking for provider size repeatedly during one render.
     * The second request should reuse cached count instead of calling model/repository count again.
     */
    @Test
    public void testRepeatedSizeCallsUseCachedCount() {
        TestProvider provider = new TestProvider();
        provider.returnCount = 42;

        assertEquals(42, provider.size());
        assertEquals(42, provider.size());

        assertEquals(1, provider.countCalls);
    }

    /**
     * Verifies invalidation paths for provider state that can affect count semantics.
     * After each state change, the next size request must execute a fresh count.
     */
    @Test
    public void testStateChangesInvalidateCountCache() {
        assertInvalidatesCountCache(provider -> provider.setOptions(GetOperationOptions.createRawCollection()));
        assertInvalidatesCountCache(TestProvider::clearCache);
        assertInvalidatesCountCache(TestProvider::detach);
        assertInvalidatesCountCache(provider -> provider.setCompiledObjectCollectionView(new CompiledObjectCollectionView()));
    }

    /**
     * Preview panels intentionally bypass object counting.
     * Leaving preview mode must not reuse a count captured before preview mode was enabled.
     */
    @Test
    public void testPreviewBypassesCountingAndDoesNotReuseStaleCount() {
        TestProvider provider = new TestProvider();
        provider.returnCount = 42;

        assertEquals(42, provider.size());
        provider.returnCount = 43;
        provider.setForPreview(true);

        assertEquals(Integer.MAX_VALUE, provider.size());
        assertEquals(1, provider.countCalls);

        provider.setForPreview(false);
        assertEquals(43, provider.size());
        assertEquals(2, provider.countCalls);
    }

    /**
     * Guards against recomputing the query inside concrete count implementations.
     * The count operation must use the query instance captured by internalSize(), because it is also the cache key.
     */
    @Test
    public void testCountReceivesQueryBuiltForCurrentInternalSizeCall() {
        TestProvider provider = new TestProvider();
        provider.querySupplier = () -> queryForOid(UUID.randomUUID().toString());

        assertEquals(42, provider.size());

        assertSame(provider.builtQueries.get(0), provider.lastCountQuery);
        assertEquals(1, provider.builtQueries.size());
        assertEquals(1, provider.countCalls);
    }

    private ObjectQuery queryForOid(String oid) {
        return getPrismContext().queryFor(UserType.class)
                .id(oid)
                .build();
    }

    private void assertInvalidatesCountCache(Consumer<TestProvider> invalidator) {
        TestProvider provider = new TestProvider();
        provider.returnCount = 42;

        assertEquals(42, provider.size());
        provider.returnCount = 43;
        invalidator.accept(provider);

        assertEquals(43, provider.size());
        assertEquals(2, provider.countCalls);
    }

    private class TestProvider extends SelectableBeanDataProvider<UserType> {

        private final Class<UserType> type = UserType.class;
        private Supplier<ObjectQuery> querySupplier = () -> queryForOid("00000000-0000-0000-0000-000000000001");
        private int returnCount = 42;
        private int countCalls;
        private ObjectQuery lastCountQuery;
        private final List<ObjectQuery> builtQueries = new ArrayList<>();

        private TestProvider() {
            super(new Label("test"), Model.of((Search<UserType>) null), Set.of(), false);
        }

        @Override
        protected ModelInteractionService getModelInteractionService() {
            return MODEL_INTERACTION_SERVICE;
        }

        @Override
        public ObjectQuery getQuery() {
            ObjectQuery query = querySupplier.get();
            builtQueries.add(query);
            return query;
        }

        @Override
        public Class<UserType> getType() {
            return type;
        }

        @Override
        protected Task createCountTask() {
            return new NullTaskImpl();
        }

        @Override
        protected OperationResult createCountResult(Task task) {
            return new OperationResult("test");
        }

        @Override
        public Iterator<SelectableBean<UserType>> internalIterator(long offset, long pageSize) {
            return List.<SelectableBean<UserType>>of().iterator();
        }

        @Override
        protected List<UserType> searchObjects(Class<UserType> type, ObjectQuery query,
                Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) {
            return List.of();
        }

        @Override
        protected boolean match(UserType selectedValue, UserType foundValue) {
            return false;
        }

        @Override
        protected Integer countObjects(Class<UserType> type, ObjectQuery query,
                Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                Task task, OperationResult result) {
            assertSame(this.type, type);
            countCalls++;
            lastCountQuery = query;
            return returnCount;
        }
    }
}

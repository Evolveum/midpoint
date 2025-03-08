/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.displayCollection;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.repo.sqale.SqaleRepositoryService.REPOSITORY_IMPL_NAME;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import com.evolveum.midpoint.prism.Freezable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.OperationPerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Various low-level tests regarding the repository cache.
 */
@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-repo-cache-test.xml" })
public class TestRepositoryCache extends AbstractSpringTest implements InfraTestMixin {

    private static final String CLASS_DOT = TestRepositoryCache.class.getName() + ".";

    @Autowired RepositoryCache repositoryCache;
    @Autowired GlobalObjectCache globalObjectCache;
    @Autowired GlobalVersionCache globalVersionCache;
    @Autowired GlobalQueryCache globalQueryCache;
    @Autowired PrismContext prismContext;

    @SuppressWarnings("unused") // used when heap dumps are uncommented, see dumpHeap method below
    private final long identifier = System.currentTimeMillis();

    // Nothing for old repo, short class name for new one. TODO inline when old repo goes away
    private String opNamePrefix;
    private boolean isNewRepoUsed;

    private long lastCloneCount;

    @BeforeSuite
    public void setup() {
        SchemaDebugUtil.initializePrettyPrinter();
    }

    @PostConstruct
    public void initialize() throws SchemaException {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
        PrismTestUtil.setPrismContext(prismContext);

        prismContext.setMonitor(new InternalMonitor());

        OperationResult initResult = new OperationResult(CLASS_DOT + "setup");
        repositoryCache.postInit(initResult);

        String implName = repositoryCache.getRepositoryDiag().getImplementationShortName();
        isNewRepoUsed = Objects.equals(implName, REPOSITORY_IMPL_NAME);
        opNamePrefix = isNewRepoUsed
                ? SqaleRepositoryService.class.getSimpleName() + '.'
                : "";
    }

    /** Tests `getObject` operation passing the cache. */
    @Test
    public void test100GetObjectPassingCache() throws CommonException {
        testGetObjectBasic(UserType.class, getTestNameShort(), false);
    }

    /** Tests `getObject` operation using the cache (only the basic features). */
    @Test
    public void test110GetObjectUsingCache() throws CommonException {
        testGetObjectBasic(SystemConfigurationType.class, getTestNameShort(), true);
    }

    /** Tests `searchObjects` operation passing the cache. */
    @Test
    public void test200SearchObjectsPassingCache() throws CommonException {
        testSearchObjectsBasic(UserType.class, 5, false);
    }

    /** Tests `searchObjects` operation using the cache (only the basic features). */
    @Test
    public void test210SearchObjectsUsingCache() throws CommonException {
        testSearchObjectsBasic(ArchetypeType.class, 5, true);
    }

    /** Tests `searchObjectsIterative` operation passing the cache. */
    @Test
    public void test220SearchObjectsIterativePassingCache() throws CommonException {
        testSearchObjectsIterativeBasic(UserType.class, 5, false);
    }

    /** Tests `searchObjectsIterative` operation using the cache (only the basic features). */
    @Test
    public void test230SearchObjectsIterativeUsingCache() throws CommonException {
        testSearchObjectsIterativeBasic(ArchetypeType.class, 5, true);
    }

    /**
     * Searching for objects with the exclude = "." option. The search result should be cached, but individual objects should not.
     *
     * This is to simulate the assignment target search evaluator that tries to search for roles with exclude = "." option.
     * Although the objects resulting from the search cannot be cached (obviously, as they contain no data), their OIDs forming
     * the result itself, can be.
     */
    @Test
    public void test290SearchArchetypesWithExcludeOption() throws CommonException {
        var result = createOperationResult();
        var name = getTestNameShort();
        var description = "description";

        var archetype = new ArchetypeType()
                .name(name)
                .description(description);
        var oid = repositoryCache.addObject(archetype.asPrismObject(), null, result);

        clearCaches();

        var query = prismContext.queryFor(ArchetypeType.class)
                .item(ArchetypeType.F_NAME).eqPoly(name).matchingOrig()
                .build();

        var options = GetOperationOptionsBuilder.create()
                .retrieve(RetrieveOption.EXCLUDE)
                .build();

        when("executing the first search");
        clearStatistics();
        var objects1 = repositoryCache.searchObjects(ArchetypeType.class, query, options, result);

        then("result is OK and there was a repo access");
        displayCollection("objects retrieved", objects1);
        assertObjectOids(objects1, oid);
        assertSearchOperations(1);
        assertThat(objects1.get(0).asObjectable().getDescription())
                .as("description")
                .isIn(null, description);

        when("polluting the search result and executing the second search");
        objects1.get(0).asObjectable().setDescription("garbage");
        clearStatistics();
        var objects2 = repositoryCache.searchObjects(ArchetypeType.class, query, options, result);

        then("result is OK and there was a repo access (the object itself could not be cached)");
        displayCollection("objects retrieved", objects2);
        assertObjectOids(objects2, oid);
        assertSearchOperations(1);
        assertThat(objects2.get(0).asObjectable().getDescription())
                .as("description")
                .isIn(null, description);

        when("retrieving the archetype by 'getObject', polluting the result, and repeating the search");
        var retrieved = repositoryCache.getObject(ArchetypeType.class, oid, null, result);
        retrieved.asObjectable().setDescription("garbage");
        clearStatistics();
        var objects3 = repositoryCache.searchObjects(ArchetypeType.class, query, options, result);

        then("result is OK and there was a NO repo access (the object data are now in the cache)");
        displayCollection("objects retrieved", objects3);
        assertObjectOids(objects3, oid);
        assertSearchOperations(0);
        assertThat(objects3.get(0).asObjectable().getDescription())
                .as("description")
                .isIn(null, description);
    }

    private void assertObjectOids(Collection<? extends PrismObject<?>> objects, String... expectedOids) {
        assertThat(objects).as("objects").hasSize(expectedOids.length);
        assertThat(objects.stream().map(PrismObject::getOid))
                .as("object OIDs")
                .containsExactlyInAnyOrder(expectedOids);
    }

    /**
     * MID-6250
     */
    @Test
    public void test300ModifyInIterativeSearch() throws CommonException {
        given();
        PrismContext prismContext = getPrismContext();
        OperationResult result = createOperationResult();

        clearStatistics();
        clearCaches();

        String name = getTestNameShort();
        String changedDescription = "changed";

        PrismObject<ArchetypeType> archetype = new ArchetypeType()
                .name(name)
                .asPrismObject();
        repositoryCache.addObject(archetype, null, result);

        when();
        ObjectQuery query = prismContext.queryFor(ArchetypeType.class)
                .item(ArchetypeType.F_NAME).eqPoly(name).matchingOrig()
                .build();
        List<ItemDelta<?, ?>> deltas = prismContext.deltaFor(ArchetypeType.class)
                .item(ArchetypeType.F_DESCRIPTION).replace(changedDescription)
                .asItemDeltas();
        AtomicInteger found = new AtomicInteger(0);
        ResultHandler<ArchetypeType> handler = (object, result1) -> {
            try {
                repositoryCache.modifyObject(ArchetypeType.class, object.getOid(), deltas, result1);
            } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                throw new AssertionError(e);
            }
            found.incrementAndGet();
            return true;
        };
        repositoryCache.searchObjectsIterative(ArchetypeType.class, query, handler, null, false, result);
        dumpStatistics();

        assertThat(found.get()).as("objects found").isEqualTo(1);

        PrismObject<ArchetypeType> singleObjectAfter = repositoryCache.getObject(ArchetypeType.class, archetype.getOid(), null, result);
        List<PrismObject<ArchetypeType>> listAfter = repositoryCache.searchObjects(ArchetypeType.class, query, null, result);

        then();
        assertThat(singleObjectAfter.asObjectable().getDescription()).as("description in getObject result (after change)").isEqualTo(changedDescription);

        assertThat(listAfter.size()).as("objects found after").isEqualTo(1);
        assertThat(listAfter.get(0).asObjectable().getDescription()).as("description in searchObjects result (after change)").isEqualTo(changedDescription);
    }

    /**
     * MID-6250
     */
    @Test
    public void test310AddInIterativeSearch() throws CommonException {
        given();
        PrismContext prismContext = getPrismContext();
        OperationResult result = createOperationResult();

        clearStatistics();
        clearCaches();

        String costCenter = "cc_" + getTestNameShort();
        String name1 = getTestNameShort() + ".1";
        String name2 = getTestNameShort() + ".2";

        PrismObject<ArchetypeType> archetype1 = new ArchetypeType()
                .name(name1)
                .costCenter(costCenter)
                .asPrismObject();
        repositoryCache.addObject(archetype1, null, result);

        when();
        ObjectQuery query = prismContext.queryFor(ArchetypeType.class)
                .item(ArchetypeType.F_COST_CENTER).eq(costCenter)
                .build();
        AtomicInteger found = new AtomicInteger(0);
        ResultHandler<ArchetypeType> handler = (object, result1) -> {
            try {
                PrismObject<ArchetypeType> archetype2 = new ArchetypeType()
                        .name(name2)
                        .costCenter(costCenter)
                        .asPrismObject();
                repositoryCache.addObject(archetype2, null, result);
            } catch (SchemaException | ObjectAlreadyExistsException e) {
                throw new AssertionError(e);
            }
            found.incrementAndGet();
            return true;
        };
        repositoryCache.searchObjectsIterative(ArchetypeType.class, query, handler, null, false, result);
        dumpStatistics();

        assertThat(found.get()).as("objects found").isEqualTo(1);

        List<PrismObject<ArchetypeType>> listAfter = repositoryCache.searchObjects(ArchetypeType.class, query, null, result);

        then();
        assertThat(listAfter.size()).as("objects found after").isEqualTo(2);
    }

    /**
     * MID-6250
     */
    @Test
    public void test320SearchObjectsIterativeSlow() throws CommonException {
        OperationResult result = createOperationResult();

        deleteExistingObjects(ArchetypeType.class, result);

        clearStatistics();
        clearCaches();

        generateObjects(ArchetypeType.class, 5, result);

        when();

        SearchResultList<PrismObject<ArchetypeType>> retrieved = new SearchResultList<>();
        AtomicBoolean delayed = new AtomicBoolean(false);
        ResultHandler<ArchetypeType> handler = (object, parentResult) -> {
            retrieved.add(object.clone());
            object.asObjectable().setDescription("garbage: " + Math.random());
            if (!delayed.getAndSet(true)) {
                try {
                    Thread.sleep(1500); // larger than default staleness limit of 1000 ms
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            return true;
        };
        repositoryCache.searchObjectsIterative(ArchetypeType.class, null, handler, null, true, result);

        then();
        dumpStatistics();

        assertObjectIsCached(retrieved.get(0).getOid());
        assertVersionIsCached(retrieved.get(0).getOid());
        for (int i = 1; i < retrieved.size(); i++) {
            assertObjectIsNotCached(retrieved.get(i).getOid());
            assertVersionIsNotCached(retrieved.get(i).getOid());
        }
        assertQueryIsNotCached(ArchetypeType.class, null);

        Map<String, CachePerformanceCollector.CacheData> map = CachePerformanceCollector.INSTANCE.getGlobalPerformanceMap();
        CachePerformanceCollector.CacheData data = map.get("all.ArchetypeType");
        assertThat(data.skippedStaleData.get()).as("stale data counter").isEqualTo(5); // 4 objects + 1 search result
    }

    @Test
    public void test330SearchObjectsOverSize() throws CommonException {
        OperationResult result = createOperationResult();

        deleteExistingObjects(ArchetypeType.class, result);

        clearStatistics();
        clearCaches();

        generateObjects(ArchetypeType.class, 101, result);

        repositoryCache.searchObjects(ArchetypeType.class, null, null, result);
        dumpStatistics();
        assertQueryIsNotCached(ArchetypeType.class, null);

        repositoryCache.searchObjectsIterative(ArchetypeType.class, null, (object, parentResult) -> true, null, true, result);
        dumpStatistics();
        assertQueryIsNotCached(ArchetypeType.class, null);

        Map<String, CachePerformanceCollector.CacheData> map = CachePerformanceCollector.INSTANCE.getGlobalPerformanceMap();
        CachePerformanceCollector.CacheData data = map.get("all.ArchetypeType");
        assertThat(data.overSizedQueries.get()).as("over-sized counter").isEqualTo(2); // search + searchIterative
    }

    /** MID-6003 */
    @Test
    public void test350GetArchetypeWithIncludeOptionNoPhoto() throws CommonException {
        testGetObjectWithSmartIncludeHandling(ArchetypeType.class, a -> {}, true);
    }

    /** MID-6003 */
    @Test
    public void test352GetArchetypeWithIncludeOptionWithPhoto() throws CommonException {
        testGetObjectWithSmartIncludeHandling(ArchetypeType.class, a -> a.setJpegPhoto(new byte[] { 1, 2, 3 }), false);
    }

    // region Testing the effect of various GetOperationOptions on the cache

    @Test
    public void test400UsingSafeOptions() throws CommonException {
        testUsingSafeOptions(null);
        testUsingSafeOptions(b -> b);
        testUsingSafeOptions(b -> b.readOnly());
        testUsingSafeOptions(b -> b.doNotDiscovery());
        testUsingSafeOptions(b -> b.forceRefresh());
        testUsingSafeOptions(b -> b.forceRetry());
        testUsingSafeOptions(b -> b.allowNotFound());
        testUsingSafeOptions(b -> b.executionPhase());
        testUsingSafeOptions(b -> b.noFetch());
        testUsingSafeOptions(b -> b.futurePointInTime());
        testUsingSafeOptions(b -> b.errorReportingMethod(FetchErrorReportingMethodType.EXCEPTION));
        // plus some combinations
        testUsingSafeOptions(b -> b.doNotDiscovery().forceRetry().forceRefresh());
    }

    private void testUsingSafeOptions(Function<GetOperationOptionsBuilder, GetOperationOptionsBuilder> builderFunction)
            throws CommonException {

        // Operations using the correct type and safe options are always served from the cache.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ArchetypeType.class, getOptions(builderFunction),
                true, false, true, true);

        // Results of get/search operations invoked on ObjectType are never cached.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ObjectType.class, getOptions(builderFunction),
                true, false, false, false);

        // Even with pre-populating the cache, the operation cannot be served from the cache, as ObjectType-typed requests
        // are not configured to be served from the cache.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ObjectType.class, getOptions(builderFunction),
                true, true, false, true);
    }

    @Test
    public void test410UsingUnsafeOptions() throws CommonException {
        testUsingUnsafeOptions(b -> b.resolve());
        testUsingUnsafeOptions(b -> b.resolveNames());
        testUsingUnsafeOptions(b -> b.raw());
        testUsingUnsafeOptions(b -> b.tolerateRawData());
        testUsingUnsafeOptions(b -> b.retrieve(new RelationalValueSearchQuery(null)));
        testUsingUnsafeOptions(b -> b.distinct());
        testUsingUnsafeOptions(b -> b.attachDiagData());
        testUsingUnsafeOptions(b -> b.definitionProcessing(DefinitionProcessingOption.FULL));
        testUsingUnsafeOptions(b -> b.iterationMethod(IterationMethodType.FETCH_ALL));
    }

    private void testUsingUnsafeOptions(Function<GetOperationOptionsBuilder, GetOperationOptionsBuilder> builderFunction)
            throws CommonException {

        // Operations with unsafe options are never served from the cache; and their results are not stored into the cache.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ArchetypeType.class, getOptions(builderFunction),
                true, false, false, false);

        // Even if pre-populating the cache, the operation cannot be served from the cache.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ArchetypeType.class, getOptions(builderFunction),
                true, true, false, true);
    }

    @Test
    public void test420UsingZeroStalenessOption() throws CommonException {
        testUsingZeroStalenessOptions(b -> b.staleness(0L));
        // safe options should be safe also here
        testUsingZeroStalenessOptions(b -> b.staleness(0L).readOnly());
        testUsingZeroStalenessOptions(b -> b.staleness(0L).doNotDiscovery());
    }

    private void testUsingZeroStalenessOptions(Function<GetOperationOptionsBuilder, GetOperationOptionsBuilder> builderFunction)
            throws CommonException {

        // Operations with zero staleness are never served from the cache; but their results are stored into the cache.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ArchetypeType.class, getOptions(builderFunction),
                true, false, false, true);

        // Results being pre-populated does not change anything in this case.
        testWithOptions(
                ArchetypeType.class, getTestNameShort(), ArchetypeType.class, getOptions(builderFunction),
                true, true, false, true);
    }

    private static @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions(Function<GetOperationOptionsBuilder, GetOperationOptionsBuilder> builderFunction) {
        return builderFunction != null ?
                builderFunction
                        .apply(GetOperationOptionsBuilder.create())
                        .build() : null;
    }

    private <T extends ObjectType> void testWithOptions(
            Class<T> type, String objectName,
            Class<? extends ObjectType> requestType, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean includeSearchOperations,
            boolean inCachesBefore, boolean servedFromCache, boolean inObjectCacheAfter)
            throws CommonException {

        var result = createOperationResult();
        clearCaches();

        given("an object in the repo");
        var object = getPrismContext().createObject(type);
        object.asObjectable().setName(PolyStringType.fromOrig(objectName));
        var oid = repositoryCache.addObject(object, null, result);

        var scenario = new CachingScenario(
                object, requestType, options, inCachesBefore, servedFromCache, inObjectCacheAfter);

        testOperationWithOptions(scenario, getObjectOperation(), result);
        if (includeSearchOperations) {
            testOperationWithOptions(scenario, searchObjectsOperation(createQueryByName(object)), result);
            testOperationWithOptions(scenario, searchObjectsOperation(createQueryByOid(object)), result);
            testOperationWithOptions(scenario, searchObjectsIterativeOperation(createQueryByName(object)), result);
            testOperationWithOptions(scenario, searchObjectsIterativeOperation(createQueryByOid(object)), result);
        }

        repositoryCache.deleteObject(type, oid, result);
    }

    private void testOperationWithOptions(CachingScenario scenario, RequestedOperation operation, OperationResult result)
            throws CommonException {

        clearCaches();

        var requestType = scenario.requestType;
        var object = scenario.object;
        var options = scenario.options;

        if (scenario.inCachesBefore) {
            when("executing %s on %s to pre-populate the caches".formatted(operation, object));
            operation.execute(object.getCompileTimeClass(), object, null, result);
        } else {
            when("executing %s (%s) on %s with %s the first time".formatted(
                    operation, requestType.getSimpleName(), object, options));
            operation.execute(requestType, object, options, result);
        }

        clearStatistics();

        when("executing %s (%s) on %s with %s the second time".formatted(operation, requestType.getSimpleName(), object, options));
        operation.execute(requestType, object, options, result);

        if (scenario.servedFromCache) {
            then("operation should be served from the cache");
            assertOperations(operation.getOperationName(), 0);
        } else {
            then("operation should be really executed");
            assertOperations(operation.getOperationName(), 1);
        }

        if (scenario.inObjectCacheAfter) {
            assertObjectIsCached(object.getOid());
        } else {
            assertObjectIsNotCached(object.getOid());
        }
    }

    // endregion

    // Must be executed last, because naive deletion of such large number of archetypes fails on OOM
    @Test
    public void test900HeapUsage() throws Exception {
        OperationResult result = new OperationResult("testHeapUsage");

        int size = 2_000_000;
        int count = 400;

        // 100 is the default "step" in paged iterative search, so we can expect we always have 50 objects in memory
        // And "times 4" is the safety margin. It might or might not be sufficient, as System.gc() is not guaranteed to
        // really execute the garbage collection (only suggests JVM to do it).
        long tolerance = (100 * size) * 4;

        showMemory("Initial");
        dumpHeap("initial");

        deleteExistingObjects(ArchetypeType.class, result);
        generateLargeObjects(ArchetypeType.class, size, count, result);
        showMemory("After generation");
        dumpHeap("after-generation");

        long usedBefore = getMemoryUsed();
        AtomicInteger seen = new AtomicInteger();
        AtomicLong usedInLastIteration = new AtomicLong();
        repositoryCache.searchObjectsIterative(ArchetypeType.class, null,
                (object, parentResult) -> {
                    if (seen.incrementAndGet() % 100 == 0) {
                        showMemory("After seeing " + seen.get() + " objects");
                    }
                    if (seen.get() == count) {
                        usedInLastIteration.set(getMemoryUsed());
                        dumpHeap("last-iteration");
                    }
                    return true;
                }, null, true, result);
        showMemory("Final");
        dumpHeap("final");

        long difference = usedInLastIteration.get() - usedBefore;

        long differenceKb = difference / 1024;
        long toleranceKb = tolerance / 1024;
        System.out.printf("Difference: %,d KB (tolerating %,d KB)", differenceKb, toleranceKb);
        if (differenceKb > toleranceKb) {
            fail("Used too much memory during iterative search: " + differenceKb + " KB (accepting up to " + toleranceKb + " KB)");
        }
    }

    private void showMemory(String desc) {
        long used = getMemoryUsed();
        display(String.format("%s: %,d used (%,d KB)%n", desc, used, used / 1024));
    }

    private long getMemoryUsed() {
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @SuppressWarnings({ "unused", "CommentedOutCode" })
    private void dumpHeap(String desc) {
        // Enable when needed
//        try {
//            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
//                    "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
//            display("Generating heap dump: " + desc + "...");
//            mxBean.dumpHeap("heap-" + identifier + "-" + desc + ".hprof", true);
//            display("Done");
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
    }

    /**
     * Creates an object and repeatedly gets it. Then counts the number of repository service invocations.
     *
     * Besides that, alters the objects retrieved (in memory) and verifies that the returned objects are correct
     * i.e. not influenced by alterations of previously returned objects.
     *
     * Checks also the R/O option handling.
     */
    private <T extends ObjectType> void testGetObjectBasic(Class<T> objectClass, String objectName, boolean isCached)
            throws CommonException {

        var result = createOperationResult();
        clearCaches();

        given("an object in the repo");

        PrismObject<T> object = getPrismContext().createObject(objectClass);
        object.asObjectable().setName(PolyStringType.fromOrig(objectName));

        clearStatistics();
        var oid = repositoryCache.addObject(object, null, result);
        assertAddOperations(1);
        assertGetOperations(0);

        when("object is retrieved (null options)");
        clearStatistics();
        var object1 = repositoryCache.getObject(objectClass, oid, null, result);

        then("object is OK");
        displayDumpable("object retrieved", object1);
        assertEquals("Wrong object1", object, object1);
        object1.checkMutable();
        dumpStatistics();
        assertGetOperations(1);
        if (isCached) {
            assertCloneOperations(1); // when putting into the cache (immutable copy)
        } else {
            assertCloneOperations(0);
        }

        when("in-memory representation is corrupted, and object is retrieved again");
        object1.asObjectable().setDescription("garbage");
        clearStatistics();
        var object2 = repositoryCache.getObject(objectClass, oid, null, result);

        then("object is OK");
        displayDumpable("object retrieved", object2);
        assertEquals("Wrong object2", object, object2);
        object2.checkMutable();
        dumpStatistics();
        assertGetOperations(isCached ? 0 : 1);
        if (isCached) {
            assertCloneOperations(1); // when retrieving from the cache (to make it mutable)
        } else {
            assertCloneOperations(0);
        }

        when("in-memory representation is corrupted again, and object is retrieved again");
        object2.asObjectable().setDescription("total garbage");
        clearStatistics();
        var object3 = repositoryCache.getObject(objectClass, oid, null, result);

        then("object is OK");
        displayDumpable("object retrieved", object3);
        assertEquals("Wrong object3", object, object3);
        object3.checkMutable();
        dumpStatistics();
        assertGetOperations(isCached ? 0 : 1);
        if (isCached) {
            assertCloneOperations(1); // when retrieving from the cache (to make it mutable)
        } else {
            assertCloneOperations(0);
        }

        and("object is (or is not) really in the cache");
        assertObjectAndVersionCached(object.getOid(), isCached);

        when("object is retrieved with R/O option");
        clearStatistics();
        var object4 = repositoryCache.getObject(objectClass, oid, readOnly(), result);

        then("object is OK");
        displayDumpable("object retrieved", object4);
        assertEquals("Wrong object4", object, object4);
        // Actually, the contract don't require the returned object to be immutable, but it's a service to clients, so that
        // they don't need to freeze the object themselves. To be reconsidered.
        object4.checkImmutable();
        dumpStatistics();
        assertGetOperations(isCached ? 0 : 1);
        assertCloneOperations(0); // cached -> returned right from the cache; not cached -> 0 as before
    }

    /**
     * Checks the `getObject` behavior with `retrieve=include` option present.
     *
     * . First, the object is created and retrieved with no options. It should put it into the cache.
     * . Then, object is retrieved with `retrieve=include` option. It should be retrieved from the cache, if
     * it does not contain incomplete items and if it's supported by the policy.
     */
    private <T extends ObjectType> void testGetObjectWithSmartIncludeHandling(
            Class<T> objectClass, Consumer<T> objectCustomizer, boolean isCached) throws CommonException {
        clearStatistics();
        clearCaches();

        PrismObject<T> object = getPrismContext().createObject(objectClass);
        object.asObjectable().setName(PolyStringType.fromOrig(String.valueOf(Math.random())));
        objectCustomizer.accept(object.asObjectable());

        OperationResult result = createOperationResult();
        String oid = repositoryCache.addObject(object, null, result);

        PrismObject<T> object1 = repositoryCache.getObject(objectClass, oid, null, result);
        displayDumpable("1st object retrieved", object1);

        PrismObject<T> object2 = repositoryCache.getObject(objectClass, oid, createRetrieveCollection(), result);
        displayDumpable("2nd object retrieved", object2);
        assertEquals("Wrong object2 (compared with the original)", object, object2);
        if (isCached) {
            assertEquals("Wrong object2 (compared to the one retrieved previously)", object1, object2);
        }

        dumpStatistics();
        assertAddOperations(1);
        assertGetOperations(isCached ? 1 : 2);
    }

    private void assertObjectAndVersionCached(String oid, boolean isCached) {
        if (isCached) {
            assertObjectIsCached(oid);
            assertVersionIsCached(oid);
        } else {
            assertObjectIsNotCached(oid);
            assertVersionIsNotCached(oid);
        }
    }

    private <T extends ObjectType> void assertQueryCached(Class<T> type, ObjectQuery query, boolean isCached) {
        if (isCached) {
            assertQueryIsCached(type, query);
        } else {
            assertQueryIsNotCached(type, query);
        }
    }

    private void assertObjectIsCached(String oid) {
        assertThat(globalObjectCache.get(oid))
                .as("cached object value for " + oid)
                .isNotNull();
    }

    private void assertObjectIsNotCached(String oid) {
        assertThat(globalObjectCache.get(oid))
                .as("cached object value for " + oid)
                .isNull();
    }

    private void assertVersionIsCached(String oid) {
        assertThat(globalVersionCache.get(oid))
                .as("cached version value for " + oid)
                .isNotNull();
    }

    private void assertVersionIsNotCached(String oid) {
        assertThat(globalVersionCache.get(oid))
                .as("cached version value for " + oid)
                .isNull();
    }

    private <T extends ObjectType> void assertQueryIsCached(Class<T> type, ObjectQuery query) {
        QueryKey<T> key = new QueryKey<>(type, query);
        assertThat(globalQueryCache.get(key))
                .as("cached version value for " + key)
                .isNotNull();
    }

    private <T extends ObjectType> void assertQueryIsNotCached(Class<T> type, ObjectQuery query) {
        QueryKey<T> key = new QueryKey<>(type, query);
        assertThat(globalQueryCache.get(key))
                .as("cached version value for " + key)
                .isNull();
    }

    private void clearCaches() {
        globalObjectCache.clear();
        globalVersionCache.clear();
        globalQueryCache.clear();
    }

    /**
     * Creates a set of objects and repeatedly searches for it + repeatedly gets the objects.
     * Then counts the number of repository service invocations.
     *
     * Besides that, alters the objects retrieved (in memory) and verifies that the returned objects are correct
     * i.e. not influenced by alterations of previously returned objects.
     */
    private <T extends ObjectType> void testSearchObjectsBasic(Class<T> type, int objectCount, boolean isCached)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

        var result = createOperationResult();
        clearCaches();

        given("objects in the repo");

        deleteExistingObjects(type, result);

        clearStatistics();
        var objects = generateObjects(type, objectCount, result);
        assertAddOperations(objectCount);
        assertSearchOperations(0);
        assertGetOperations(0);

        when("objects are retrieved (null options)");
        clearStatistics();
        var objects1 = repositoryCache.searchObjects(type, null, null, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        objects1.checkMutable(); // not checking individual objects, but the corruption proves they're mutable
        dumpStatistics();
        assertSearchOperations(1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? objectCount : 0); // when putting into the cache

        var referentialList = objects1.deepClone();

        when("in-memory representation is corrupted, and objects are retrieved again");
        objects1.get(0).asObjectable().setDescription("garbage");
        clearStatistics();
        var objects2 = repositoryCache.searchObjects(type, null, null, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        objects2.checkMutable(); // not checking individual objects, but the corruption proves they're mutable
        dumpStatistics();
        assertSearchOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? objectCount : 0); // when retrieving from the cache

        when("in-memory representation is corrupted again, and objects are retrieved again");
        objects2.get(0).asObjectable().setDescription("total garbage");
        clearStatistics();
        var objects3 = repositoryCache.searchObjects(type, null, null, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));
        objects3.checkMutable();
        dumpStatistics();
        assertSearchOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? objectCount : 0); // when retrieving from the cache

        when("in-memory representation is corrupted again, and objects are retrieved again (R/O mode)");
        objects3.get(0).asObjectable().setDescription("total garbage");
        clearStatistics();
        var objects4 = repositoryCache.searchObjects(type, null, readOnly(), result);

        then("objects are OK");
        displayCollection("objects retrieved", objects4);
        assertEquals("Wrong objects4", objects, new HashSet<>(objects4));
        objects4.checkImmutable();
        dumpStatistics();
        assertSearchOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(0);

        getObjectsAfterSearching(type, referentialList, isCached, result);

        then("query and objects are in the cache (iff cached)");
        assertQueryCached(type, null, isCached);
        for (PrismObject<T> object : objects) {
            assertObjectAndVersionCached(object.getOid(), isCached);
        }
    }

    private <T extends ObjectType> void getObjectsAfterSearching(
            Class<T> objectClass, SearchResultList<PrismObject<T>> list, boolean isCached, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        when("objects are retrieved again, using 'getObject' method call");
        clearStatistics();
        for (PrismObject<T> object : list) {
            String oid = object.getOid();

            PrismObject<T> object1 = repositoryCache.getObject(objectClass, oid, null, result);
            displayDumpable("object retrieved", object1);
            assertEquals("Wrong object1", object, object1);
            object1.asObjectable().setDescription("garbage");

            PrismObject<T> object2 = repositoryCache.getObject(objectClass, oid, null, result);
            displayDumpable("2nd object retrieved", object2);
            assertEquals("Wrong object2", object, object2);
            object2.asObjectable().setDescription("total garbage");

            PrismObject<T> object3 = repositoryCache.getObject(objectClass, oid, null, result);
            assertEquals("Wrong object3", object, object3);
            displayDumpable("3rd object retrieved", object3);
        }

        then("operation counts are correct");
        assertGetOperations(isCached ? 0 : 3 * list.size());
    }

    private <T extends ObjectType> void testSearchObjectsIterativeBasic(Class<T> type, int objectCount,
            boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

        var result = createOperationResult();
        clearCaches();

        given("objects in the repo");

        deleteExistingObjects(type, result);

        clearStatistics();
        var objects = generateObjects(type, objectCount, result);
        assertAddOperations(objectCount);
        assertSearchIterativeOperations(0);
        assertGetOperations(0);

        when("objects are retrieved iteratively (null options)");
        clearStatistics();
        var objects1 = searchObjectsIterative(type, null, false, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        dumpStatistics();
        assertSearchIterativeOperations(1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? 2L * objectCount : objectCount); // when modifying + when putting into the cache

        var referentialList = objects1.deepClone();

        when("objects are retrieved again (they were corrupted before)");
        clearStatistics();
        var objects2 = searchObjectsIterative(type, null, false, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        dumpStatistics();
        assertSearchIterativeOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? 2L * objectCount : objectCount); // when modifying + when retrieving from the cache

        when("objects are retrieved again (they were corrupted before)");
        clearStatistics();
        var objects3 = searchObjectsIterative(type, null, false, result);

        then("objects are OK");
        displayCollection("3rd round of objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));
        dumpStatistics();
        assertSearchIterativeOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(isCached ? 2L * objectCount : objectCount); // when modifying + when retrieving from the cache

        when("objects are retrieved again (R/O mode)");
        clearStatistics();
        var objects4 = searchObjectsIterative(type, null, true, result);

        then("objects are OK");
        displayCollection("objects retrieved", objects4);
        assertEquals("Wrong objects4", objects, new HashSet<>(objects4));
        assertImmutableContent(objects4);
        dumpStatistics();
        assertSearchIterativeOperations(isCached ? 0 : 1);
        assertGetOperations(0);
        assertCloneOperations(0); // not modifying (because of R/O), not putting into/retrieving from cache

        getObjectsAfterSearching(type, referentialList, isCached, result);

        then("query and objects are in the cache (iff cached)");
        assertQueryCached(type, null, isCached);
        for (PrismObject<T> object : objects) {
            assertObjectAndVersionCached(object.getOid(), isCached);
        }
    }

    private void assertImmutableContent(Collection<? extends Freezable> collection) {
        collection.forEach(Freezable::checkImmutable);
    }

    private void assertSearchIterativeOperations(int expectedCount) {
        assertOperations(getSearchIterativeOperationName(), expectedCount);
    }

    /**
     * Returns the name of operation for `searchObjectsIterative`.
     *
     * The new repo clearly identifies "page" call, old just calls public `searchObject`.
     */
    private @NotNull String getSearchIterativeOperationName() {
        return isNewRepoUsed ?
                RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE :
                RepositoryService.OP_SEARCH_OBJECTS;
    }

    /** Searches for objects, but also clones them and corrupts the originally returned objects. */
    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsIterative(
            Class<T> type, ObjectQuery query, boolean readOnly, OperationResult result)
            throws SchemaException {
        SearchResultList<PrismObject<T>> objects = new SearchResultList<>();
        var metadata =
                repositoryCache.searchObjectsIterative(
                        type, query,
                        (object, lResult) -> {
                            if (readOnly) {
                                objects.add(object);
                            } else {
                                objects.add(object.clone());
                                object.asObjectable().setDescription("garbage: " + Math.random());
                            }
                            return true;
                        },
                        readOnly ? readOnly() : null,
                        true, result);
        objects.setMetadata(metadata);
        return objects;
    }

    @NotNull
    private <T extends ObjectType> Set<PrismObject<T>> generateObjects(Class<T> objectClass, int count, OperationResult result)
            throws SchemaException,
            ObjectAlreadyExistsException {
        Set<PrismObject<T>> objects = new HashSet<>();
        for (int i = 0; i < count; i++) {
            PrismObject<T> object = getPrismContext().createObject(objectClass);
            object.asObjectable().setName(PolyStringType.fromOrig("T:" + i));
            repositoryCache.addObject(object, null, result);
            objects.add(object);
        }
        return objects;
    }

    private <T extends ObjectType> void generateLargeObjects(Class<T> objectClass, int size, int count, OperationResult result)
            throws SchemaException,
            ObjectAlreadyExistsException {
        for (int i = 0; i < count; i++) {
            PrismObject<T> object = getPrismContext().createObject(objectClass);
            object.asObjectable()
                    .name(PolyStringType.fromOrig("T:" + i))
                    .description(StringUtils.repeat('#', size));
            repositoryCache.addObject(object, null, result);

            if ((i + 1) % 100 == 0) {
                showMemory("After generating " + (i + 1) + " objects");
            }
        }
    }

    private <T extends ObjectType> void deleteExistingObjects(Class<T> objectClass, OperationResult result)
            throws SchemaException,
            ObjectNotFoundException {
        SearchResultList<PrismObject<T>> existingObjects = repositoryCache.searchObjects(objectClass, null, null, result);
        for (PrismObject<T> existingObject : existingObjects) {
            display("Deleting " + existingObject);
            repositoryCache.deleteObject(objectClass, existingObject.getOid(), result);
        }
    }

    private void assertAddOperations(int expectedCount) {
        assertOperations(RepositoryService.OP_ADD_OBJECT, expectedCount);
    }

    private void assertGetOperations(int expectedCount) {
        assertOperations(RepositoryService.OP_GET_OBJECT, expectedCount);
    }

    private void assertSearchOperations(int expectedCount) {
        assertOperations(RepositoryService.OP_SEARCH_OBJECTS, expectedCount);
    }

    private void assertOperations(String operation, int expectedCount) {
        assertEquals("Wrong # of operations: " + operation, expectedCount, getOperationCount(operation));
    }

    private int getOperationCount(String operation) {
        PerformanceInformation performanceInformation =
                repositoryCache.getPerformanceMonitor().getGlobalPerformanceInformation();
        OperationPerformanceInformation opData = performanceInformation.getAllData().get(opNamePrefix + operation);
        return opData != null ? opData.getInvocationCount() : 0;
    }

    private void dumpStatistics() {
        PerformanceInformation performanceInformation = repositoryCache.getPerformanceMonitor().getGlobalPerformanceInformation();
        displayValue("Repository statistics",
                RepositoryPerformanceInformationUtil.format(performanceInformation.toRepositoryPerformanceInformationType()));

        Map<String, CachePerformanceCollector.CacheData> cache = CachePerformanceCollector.INSTANCE.getGlobalPerformanceMap();
        displayValue("Cache performance information (standard)",
                CachePerformanceInformationUtil.format(CachePerformanceInformationUtil.toCachesPerformanceInformationType(cache)));
        displayValue("Cache performance information (extra)",
                CachePerformanceInformationUtil.formatExtra(cache));

        displayValue("clone operations count", getCloneCount());
    }

    private void clearStatistics() {
        repositoryCache.getPerformanceMonitor().clearGlobalPerformanceInformation();
        CachePerformanceCollector.INSTANCE.clear();
        rememberCloneCount();
    }

    private void rememberCloneCount() {
        lastCloneCount = InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
    }

    private long getCloneCount() {
        return InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_CLONE_COUNT) - lastCloneCount;
    }

    private void assertCloneOperations(long expected) {
        assertThat(getCloneCount()).as("clone operations executed").isEqualTo(expected);
    }

    private GetObjectOperation getObjectOperation() {
        return new GetObjectOperation();
    }

    private SearchObjectsOperation searchObjectsOperation(ObjectQuery query) {
        return new SearchObjectsOperation(query);
    }

    private SearchObjectsIterativeOperation searchObjectsIterativeOperation(ObjectQuery query) {
        return new SearchObjectsIterativeOperation(query);
    }

    private interface RequestedOperation {

        <T extends ObjectType> void execute(
                Class<? extends ObjectType> requestedType,
                PrismObject<T> object,
                Collection<SelectorOptions<GetOperationOptions>> options,
                OperationResult result)
                throws CommonException;

        String getOperationName();
    }

    private class GetObjectOperation implements RequestedOperation {

        @Override
        public <T extends ObjectType> void execute(
                Class<? extends ObjectType> requestedType,
                PrismObject<T> object,
                Collection<SelectorOptions<GetOperationOptions>> options,
                OperationResult result)
                throws CommonException {
            repositoryCache.getObject(requestedType, object.getOid(), options, result);
        }

        @Override
        public String getOperationName() {
            return RepositoryService.OP_GET_OBJECT;
        }

        @Override
        public String toString() {
            return "getObject";
        }
    }

    private class SearchObjectsOperation implements RequestedOperation {

        private final ObjectQuery query;

        private SearchObjectsOperation(ObjectQuery query) {
            this.query = query;
        }

        @Override
        public <T extends ObjectType> void execute(
                Class<? extends ObjectType> requestedType,
                PrismObject<T> object,
                Collection<SelectorOptions<GetOperationOptions>> options,
                OperationResult result)
                throws CommonException {
            repositoryCache.searchObjects(requestedType, query, options, result);
        }

        @Override
        public String getOperationName() {
            return RepositoryService.OP_SEARCH_OBJECTS;
        }

        @Override
        public String toString() {
            return "searchObjects(" + query + ")";
        }
    }

    private class SearchObjectsIterativeOperation implements RequestedOperation {

        private final ObjectQuery query;

        private SearchObjectsIterativeOperation(ObjectQuery query) {
            this.query = query;
        }

        @Override
        public <T extends ObjectType> void execute(
                Class<? extends ObjectType> requestedType,
                PrismObject<T> object,
                Collection<SelectorOptions<GetOperationOptions>> options,
                OperationResult result)
                throws CommonException {
            repositoryCache.searchObjectsIterative(
                    requestedType,
                    query,
                    (objectFound, lResult) -> true,
                    options, true, result);
        }

        @Override
        public String getOperationName() {
            return getSearchIterativeOperationName();
        }

        @Override
        public String toString() {
            return "searchObjectsIterative(" + query + ")";
        }
    }


    private static <T extends ObjectType> ObjectQuery createQueryByName(PrismObject<T> object) {
        return getPrismContext().queryFor(object.getCompileTimeClass())
                .item(ObjectType.F_NAME).eqPoly(object.getName().getOrig()).matchingOrig()
                .build();
    }

    private static <T extends ObjectType> ObjectQuery createQueryByOid(PrismObject<T> object) {
        return getPrismContext().queryFor(object.getCompileTimeClass())
                .id(object.getOid())
                .build();
    }

    /** A single-object caching scenario test, varying operations, options, and requested type. */
    private static class CachingScenario {
        /** The object used for the test scenario. */
        private final PrismObject<? extends ObjectType> object;

        /** The type used to invoke the operation. */
        private final Class<? extends ObjectType> requestType;

        /** Options used to invoke the operation. */
        private final Collection<SelectorOptions<GetOperationOptions>> options;

        /** Should be the object/query cached before the operation? (Typically by invoking it using safe options.) */
        private final boolean inCachesBefore;

        /** Should be the (repeated) operation served from the cache? */
        private final boolean servedFromCache;

        /** Is the object expected to be present in the cache after the operation? */
        private final boolean inObjectCacheAfter;

        private CachingScenario(
                PrismObject<? extends ObjectType> object,
                Class<? extends ObjectType> requestType,
                Collection<SelectorOptions<GetOperationOptions>> options,
                boolean inCachesBefore,
                boolean servedFromCache,
                boolean inObjectCacheAfter) {
            this.object = object;
            this.requestType = requestType;
            this.options = options;
            this.inCachesBefore = inCachesBefore;
            this.servedFromCache = servedFromCache;
            this.inObjectCacheAfter = inObjectCacheAfter;
        }
    }
}

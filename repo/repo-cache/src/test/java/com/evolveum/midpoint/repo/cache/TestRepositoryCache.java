/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.displayCollection;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.repo.sqale.SqaleRepositoryService.REPOSITORY_IMPL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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
import com.evolveum.midpoint.repo.cache.global.GlobalCacheObjectValue;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-repo-cache-test.xml" })
public class TestRepositoryCache extends AbstractSpringTest implements InfraTestMixin {

    private static final String CLASS_DOT = TestRepositoryCache.class.getName() + ".";

    @Autowired RepositoryCache repositoryCache;
    @Autowired GlobalObjectCache globalObjectCache;
    @Autowired GlobalVersionCache globalVersionCache;
    @Autowired GlobalQueryCache globalQueryCache;
    @Autowired PrismContext prismContext;

    @SuppressWarnings("unused") // used when heap dumps are uncommented
    private final long identifier = System.currentTimeMillis();

    // Nothing for old repo, short class name for new one. TODO inline when old repo goes away
    private String opNamePrefix;
    private boolean isNewRepoUsed;

    @BeforeSuite
    public void setup() {
        SchemaDebugUtil.initializePrettyPrinter();
    }

    @PostConstruct
    public void initialize() throws SchemaException {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
        PrismTestUtil.setPrismContext(prismContext);

        OperationResult initResult = new OperationResult(CLASS_DOT + "setup");
        repositoryCache.postInit(initResult);

        RepositoryDiag repositoryDiag = repositoryCache.getRepositoryDiag();
        String implName = repositoryDiag != null ? repositoryDiag.getImplementationShortName() : null;
        isNewRepoUsed = Objects.equals(implName, REPOSITORY_IMPL_NAME);
        opNamePrefix = isNewRepoUsed
                ? SqaleRepositoryService.class.getSimpleName() + '.'
                : "";
    }

    @Test
    public void test100GetUser() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testGetUncachedObject(UserType.class);
    }

    @Test
    public void test110GetSystemConfiguration() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testGetCachedObject(SystemConfigurationType.class);
    }

    @Test
    public void test200SearchUsers() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchUncachedObjects(UserType.class);
    }

    @Test
    public void test210SearchArchetypes() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchCachedObjects(ArchetypeType.class);
    }

    @Test
    public void test220SearchUsersIterative() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchUncachedObjectsIterative(UserType.class);
    }

    @Test
    public void test230SearchArchetypesIterative() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchCachedObjectsIterative(ArchetypeType.class);
    }

    /**
     * MID-6250
     */
    @Test
    public void test300ModifyInIterativeSearch() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        given();
        PrismContext prismContext = getPrismContext();
        OperationResult result = createOperationResult();

        clearStatistics();
        clearCaches();

        String name = "testModifyInIterativeSearch";
        String changedDescription = "changed";

        PrismObject<ArchetypeType> archetype = new ArchetypeType(prismContext)
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
    public void test310AddInIterativeSearch() throws SchemaException, ObjectAlreadyExistsException {
        given();
        PrismContext prismContext = getPrismContext();
        OperationResult result = createOperationResult();

        clearStatistics();
        clearCaches();

        String costCenter = "cc_" + getTestNameShort();
        String name1 = getTestNameShort() + ".1";
        String name2 = getTestNameShort() + ".2";

        PrismObject<ArchetypeType> archetype1 = new ArchetypeType(prismContext)
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
                PrismObject<ArchetypeType> archetype2 = new ArchetypeType(prismContext)
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
    public void test320SearchObjectsIterativeSlow() throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
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
    public void test330SearchObjectsOverSize() throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
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

    // Must be executed last, because naive deletion such large number of archetypes fails on OOM
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

    @SuppressWarnings("unused")
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

    private <T extends ObjectType> void testGetUncachedObject(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testGetObject(objectClass, false);
    }

    private <T extends ObjectType> void testGetCachedObject(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testGetObject(objectClass, true);
    }

    private <T extends ObjectType> void testSearchUncachedObjects(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testSearchObjects(objectClass, 5, false);
    }

    private <T extends ObjectType> void testSearchCachedObjects(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testSearchObjects(objectClass, 5, true);
    }

    private <T extends ObjectType> void testSearchUncachedObjectsIterative(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testSearchObjectsIterative(objectClass, 5, false);
    }

    private <T extends ObjectType> void testSearchCachedObjectsIterative(Class<T> objectClass) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        testSearchObjectsIterative(objectClass, 5, true);
    }

    /**
     * Creates an object and repeatedly gets it. Then counts the number of repository service invocations.
     *
     * Besides that, alters the objects retrieved (in memory) and verifies that the returned objects are correct
     * i.e. not influenced by alterations of previously returned objects.
     */
    private <T extends ObjectType> void testGetObject(Class<T> objectClass, boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        clearStatistics();
        clearCaches();

        PrismObject<T> object = getPrismContext().createObject(objectClass);
        object.asObjectable().setName(PolyStringType.fromOrig(String.valueOf(Math.random())));

        OperationResult result = createOperationResult();
        String oid = repositoryCache.addObject(object, null, result);

        PrismObject<T> object1 = repositoryCache.getObject(objectClass, oid, null, result);
        displayDumpable("1st object retrieved", object1);
        assertEquals("Wrong object1", object, object1);
        object1.asObjectable().setDescription("garbage");

        PrismObject<T> object2 = repositoryCache.getObject(objectClass, oid, null, result);
        displayDumpable("2nd object retrieved", object2);
        assertEquals("Wrong object2", object, object2);
        object2.asObjectable().setDescription("total garbage");

        PrismObject<T> object3 = repositoryCache.getObject(objectClass, oid, null, result);
        assertEquals("Wrong object3", object, object3);
        displayDumpable("3rd object retrieved", object3);

        dumpStatistics();
        assertAddOperations(1);
        assertGetOperations(isCached ? 1 : 3);

        assertObjectAndVersionCached(object.getOid(), isCached);
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
        GlobalCacheObjectValue<ObjectType> value = globalObjectCache.get(oid);
        assertThat(value).as("cached object value for " + oid).isNotNull();
    }

    private void assertObjectIsNotCached(String oid) {
        GlobalCacheObjectValue<ObjectType> value = globalObjectCache.get(oid);
        assertThat(value).as("cached object value for " + oid).isNull();
    }

    private void assertVersionIsCached(String oid) {
        String value = globalVersionCache.get(oid);
        assertThat(value).as("cached version value for " + oid).isNotNull();
    }

    private void assertVersionIsNotCached(String oid) {
        String value = globalVersionCache.get(oid);
        assertThat(value).as("cached version value for " + oid).isNull();
    }

    private <T extends ObjectType> void assertQueryIsCached(Class<T> type, ObjectQuery query) {
        QueryKey<T> key = new QueryKey<>(type, query);
        SearchResultList<PrismObject<ObjectType>> value = globalQueryCache.get(key);
        assertThat(value).as("cached version value for " + key).isNotNull();
    }

    private <T extends ObjectType> void assertQueryIsNotCached(Class<T> type, ObjectQuery query) {
        QueryKey<T> key = new QueryKey<>(type, query);
        SearchResultList<PrismObject<ObjectType>> value = globalQueryCache.get(key);
        assertThat(value).as("cached version value for " + key).isNull();
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
    private <T extends ObjectType> void testSearchObjects(Class<T> type, int objectCount, boolean isCached)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

        OperationResult result = createOperationResult();

        deleteExistingObjects(type, result);

        clearStatistics();
        clearCaches();

        Set<PrismObject<T>> objects = generateObjects(type, objectCount, result);

        SearchResultList<PrismObject<T>> objects1 = repositoryCache.searchObjects(type, null, null, result);
        SearchResultList<PrismObject<T>> referentialList = objects1.deepClone();

        displayCollection("1st round of objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        objects1.get(0).asObjectable().setDescription("garbage");

        SearchResultList<PrismObject<T>> objects2 = repositoryCache.searchObjects(type, null, null, result);
        displayCollection("2nd round of objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        objects2.get(0).asObjectable().setDescription("total garbage");

        SearchResultList<PrismObject<T>> objects3 = repositoryCache.searchObjects(type, null, null, result);
        displayCollection("3rd round of objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));

        getObjectsAfterSearching(type, referentialList, result);

        dumpStatistics();
        assertAddOperations(objectCount);
        assertOperations(RepositoryService.OP_SEARCH_OBJECTS, isCached ? 1 : 3);
        assertOperations(RepositoryService.OP_GET_OBJECT, isCached ? 0 : 3 * objectCount);

        assertQueryCached(type, null, isCached);
        for (PrismObject<T> object : objects) {
            assertObjectAndVersionCached(object.getOid(), isCached);
        }
    }

    private <T extends ObjectType> void getObjectsAfterSearching(Class<T> objectClass, SearchResultList<PrismObject<T>> list,
            OperationResult result) throws ObjectNotFoundException, SchemaException {

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
    }

    private <T extends ObjectType> void testSearchObjectsIterative(Class<T> type, int objectCount,
            boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        deleteExistingObjects(type, result);

        clearStatistics();
        clearCaches();

        Set<PrismObject<T>> objects = generateObjects(type, objectCount, result);

        SearchResultList<PrismObject<T>> objects1 = searchObjectsIterative(type, null, null, result);
        SearchResultList<PrismObject<T>> referentialList = objects1.deepClone();

        displayCollection("1st round of objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        objects1.get(0).asObjectable().setDescription("garbage");

        SearchResultList<PrismObject<T>> objects2 = searchObjectsIterative(type, null, null, result);
        displayCollection("2nd round of objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        objects2.get(0).asObjectable().setDescription("total garbage");

        SearchResultList<PrismObject<T>> objects3 = searchObjectsIterative(type, null, null, result);
        displayCollection("3rd round of objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));

        getObjectsAfterSearching(type, referentialList, result);

        dumpStatistics();
        assertAddOperations(objectCount);
        assertOperations(
                // new repo clearly identifies "page" call, old just calls public searchObject
                isNewRepoUsed ? RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE
                        : RepositoryService.OP_SEARCH_OBJECTS,
                isCached ? 1 : 3);
        assertOperations(RepositoryService.OP_GET_OBJECT, isCached ? 0 : 3 * objectCount);

        assertQueryCached(type, null, isCached);
        for (PrismObject<T> object : objects) {
            assertObjectAndVersionCached(object.getOid(), isCached);
        }
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsIterative(Class<T> objectClass, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        SearchResultList<PrismObject<T>> objects = new SearchResultList<>();
        ResultHandler<T> handler = (object, parentResult) -> {
            objects.add(object.clone());
            object.asObjectable().setDescription("garbage: " + Math.random());
            return true;
        };
        SearchResultMetadata metadata = repositoryCache
                .searchObjectsIterative(objectClass, query, handler, options, true, result);
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
    }

    private void clearStatistics() {
        repositoryCache.getPerformanceMonitor().clearGlobalPerformanceInformation();
        CachePerformanceCollector.INSTANCE.clear();
    }
}

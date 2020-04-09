/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.displayCollection;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.OperationPerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-repo-cache-test.xml" })
public class TestRepositoryCache extends AbstractSpringTest implements InfraTestMixin {

    private static final String CLASS_DOT = TestRepositoryCache.class.getName() + ".";

    @Autowired RepositoryCache repositoryCache;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @PostConstruct
    public void initialize() throws SchemaException {
        OperationResult initResult = new OperationResult(CLASS_DOT + "setup");
        repositoryCache.postInit(initResult);
    }

    @Test
    public void testGetUser() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testGetObject(UserType.class, false);
    }

    @Test
    public void testGetSystemConfiguration() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testGetObject(SystemConfigurationType.class, true);
    }

    @Test
    public void testSearchUsers() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchObjects(UserType.class, 5, false);
    }

    @Test
    public void testSearchArchetypes() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchObjects(ArchetypeType.class, 5, true);
    }

    @Test
    public void testSearchUsersIterative() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchObjectsIterative(UserType.class, 5, false);
    }

    @Test
    public void testSearchArchetypesIterative() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        testSearchObjectsIterative(ArchetypeType.class, 5, true);
    }

    private <T extends ObjectType> void testGetObject(Class<T> objectClass, boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        clearStatistics();

        PrismObject<T> object = getPrismContext().createObject(objectClass);
        object.asObjectable().setName(PolyStringType.fromOrig(String.valueOf(Math.random())));

        OperationResult result = new OperationResult("testGetObject");
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
    }

    private <T extends ObjectType> void testSearchObjects(Class<T> objectClass, int count, boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = new OperationResult("testSearchObjects");

        deleteExistingObjects(objectClass, result);

        clearStatistics();

        Set<PrismObject<T>> objects = generateObjects(objectClass, count, result);

        SearchResultList<PrismObject<T>> objects1 = repositoryCache.searchObjects(objectClass, null, null, result);
        displayCollection("1st round of objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        objects1.get(0).asObjectable().setDescription("garbage");

        SearchResultList<PrismObject<T>> objects2 = repositoryCache.searchObjects(objectClass, null, null, result);
        displayCollection("2nd round of objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        objects2.get(0).asObjectable().setDescription("total garbage");

        SearchResultList<PrismObject<T>> objects3 = repositoryCache.searchObjects(objectClass, null, null, result);
        displayCollection("3rd round of objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));

        dumpStatistics();
        assertAddOperations(count);
        assertOperations(RepositoryService.OP_SEARCH_OBJECTS, isCached ? 1 : 3);
    }

    private <T extends ObjectType> void testSearchObjectsIterative(Class<T> objectClass, int count, boolean isCached) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = new OperationResult("testSearchObjectsIterative");

        deleteExistingObjects(objectClass, result);

        clearStatistics();

        Set<PrismObject<T>> objects = generateObjects(objectClass, count, result);

        SearchResultList<PrismObject<T>> objects1 = searchObjectsIterative(objectClass, null, null, result);
        displayCollection("1st round of objects retrieved", objects1);
        assertEquals("Wrong objects1", objects, new HashSet<>(objects1));
        objects1.get(0).asObjectable().setDescription("garbage");

        SearchResultList<PrismObject<T>> objects2 = searchObjectsIterative(objectClass, null, null, result);
        displayCollection("2nd round of objects retrieved", objects2);
        assertEquals("Wrong objects2", objects, new HashSet<>(objects2));
        objects2.get(0).asObjectable().setDescription("total garbage");

        SearchResultList<PrismObject<T>> objects3 = searchObjectsIterative(objectClass, null, null, result);
        displayCollection("3rd round of objects retrieved", objects3);
        assertEquals("Wrong objects3", objects, new HashSet<>(objects3));

        dumpStatistics();
        assertAddOperations(count);
        assertOperations(RepositoryService.OP_SEARCH_OBJECTS, isCached ? 1 : 3);
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
        PerformanceInformation performanceInformation = repositoryCache.getPerformanceMonitor().getGlobalPerformanceInformation();
        OperationPerformanceInformation opData = performanceInformation.getAllData().get(operation);
        return opData != null ? opData.getInvocationCount() : 0;
    }

    private void dumpStatistics() {
        PerformanceInformation performanceInformation = repositoryCache.getPerformanceMonitor().getGlobalPerformanceInformation();
        displayValue("Repository statistics", RepositoryPerformanceInformationUtil.format(performanceInformation.toRepositoryPerformanceInformationType()));
    }

    private void clearStatistics() {
        repositoryCache.getPerformanceMonitor().clearGlobalPerformanceInformation();
    }
}

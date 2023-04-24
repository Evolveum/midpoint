/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Currently not a part of automated test suite.
 */
@ContextConfiguration(locations = { "classpath:ctx-repo-cache-test.xml" })
public class CacheInvalidationPerformanceTest extends AbstractSpringTest implements InfraTestMixin {

    private static final String CLASS_DOT = CacheInvalidationPerformanceTest.class.getName() + ".";

    @Autowired RepositoryCache repositoryCache;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @PostConstruct
    public void initialize() throws SchemaException, ObjectAlreadyExistsException {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
        OperationResult initResult = new OperationResult(CLASS_DOT + "setup");
        repositoryCache.postInit(initResult);
    }

    @Test
    public void test100InvalidationPerformance() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        final int CACHED_SEARCHES = 40000;

        given();
        OperationResult result = createOperationResult();

        // create the archetype - we should create reasonably sized object, as
        ArchetypeType archetype = new ArchetypeType(getPrismContext())
                .name("name-initial")
                .displayName("some display name")
                .locality("some locality")
                .costCenter("some cost center")
                .beginActivation()
                    .administrativeStatus(ActivationStatusType.ENABLED)
                .end();
        repositoryCache.addObject(archetype.asPrismObject(), null, result);

        modifyArchetypeName(archetype, "Initial modification duration", 50, result);

        // fill-in cache with queries
        for (int i = 0; i < CACHED_SEARCHES; i++) {
            ObjectQuery query = getPrismContext().queryFor(ArchetypeType.class)
                    .item(ArchetypeType.F_NAME).eq(PolyString.fromOrig("name-" + i)).matchingOrig()
                    .or().item(ArchetypeType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ARCHIVED)
                    .or().item(ArchetypeType.F_COST_CENTER).eq("cc100").matchingCaseIgnore()
                    .build();
            repositoryCache.searchObjects(ArchetypeType.class, query, null, result);
        }

        repositoryCache.dumpContent();
        Collection<SingleCacheStateInformationType> stateInformation = repositoryCache.getStateInformation();
        displayValue("cache state information", DebugUtil.debugDump(stateInformation));

        when();
        modifyArchetypeName(archetype, "Modification duration with cache", 50, result);

        then();
    }

    private void modifyArchetypeName(ArchetypeType archetype, String label, int iterations, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            List<ItemDelta<?, ?>> itemDeltas = getPrismContext().deltaFor(ArchetypeType.class)
                    .item(ArchetypeType.F_NAME)
                    .replace(PolyString.fromOrig(RandomStringUtils.randomAlphabetic(10)))
                    .asItemDeltas();
            repositoryCache.modifyObject(ArchetypeType.class, archetype.getOid(), itemDeltas, result);
        }
        long duration = System.currentTimeMillis() - start;
        double average = (double) duration / iterations;
        displayValue(label, String.format("%,.2f ms", average));
    }
}

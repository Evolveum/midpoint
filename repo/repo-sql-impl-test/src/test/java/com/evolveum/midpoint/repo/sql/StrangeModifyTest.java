/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Modify test examining strange (borderline) cases e.g. adding already existing value etc.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class StrangeModifyTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/modify");

    private static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
    private static final String USER_BOB_OID = "16c5c6c5-a5dc-4fcd-80a6-3c296d65a31c";

    @Override
    public void initSystem() throws Exception {
        super.initSystem();

        OperationResult result = new OperationResult("initSystem");

        // This is an experimental feature, so it needs to be explicitly enabled. This will be eliminated later,
        // when we make it enabled by default.
        sqlRepositoryService.getConfiguration().setEnableIndexOnlyItems(true);
        InternalsConfig.encryptionChecks = false;

        PrismObject<UserType> userBob = prismContext.parseObject(USER_BOB_FILE);
        repositoryService.addObject(userBob, null, result);
    }

    RepoModifyOptions getModifyOptions() {
        return null;
    }

    /**
     * Adds existing name value. Nothing special.
     */
    @Test
    public void testAddExistingNameSame() throws Exception {
        given();
        OperationResult result = createOperationResult();

        PrismObject<UserType> bobBefore = getBob("before", result);
        PrismPropertyValue<Object> nameBefore = bobBefore.findProperty(UserType.F_NAME).getValue().clone();

        List<ItemDelta<?, ?>> itemDeltas = deltaFor(UserType.class)
                .item(UserType.F_NAME).add(nameBefore)
                .asItemDeltas();

        when();
        StateAfter after = executeChanges(bobBefore, itemDeltas, result);

        then();
        after.display();
    }

    private StateAfter executeChanges(PrismObject<UserType> before, List<ItemDelta<?, ?>> itemDeltas, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        StateAfter stateAfter = new StateAfter();

        List<ItemDelta<?, ?>> deltasCloned = CloneUtil.cloneCollectionMembers(itemDeltas);
        PrismObject<UserType> workingCopy = before.clone();
        ItemDeltaCollectionsUtil.applyTo(deltasCloned, workingCopy);
        stateAfter.inMemory = workingCopy;

        repositoryService.modifyObject(UserType.class, before.getOid(), itemDeltas, getModifyOptions(), result);
        stateAfter.inRepo = repositoryService.getObject(UserType.class, before.getOid(), null, result);

        return stateAfter;
    }

    private PrismObject<UserType> getBob(String message, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<UserType> bob = repositoryService.getObject(UserType.class, USER_BOB_OID, null, result);
        displayValue(message, bob);
        return bob;
    }

    private class StateAfter {
        private PrismObject<UserType> inMemory;
        private PrismObject<UserType> inRepo;

        private void display() {
            displayValue("After (memory)", inMemory);
            displayValue("After (repo)", inRepo);
        }
    }
}

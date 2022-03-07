/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;

public class SequenceFunctionalTest extends SqaleRepoBaseTest {

    @Test
    public void test010ReturningValues() throws Exception {
        OperationResult result = createOperationResult();
        String oid = repositoryService.addObject(
                new SequenceType()
                        .name("Sequence 0-9, 5 unused values, wrap around")
                        .counter(0L)
                        .maxCounter(9L)
                        .allowRewind(true)
                        .maxUnusedValues(5)
                        .asPrismObject(),
                null, result);

        assertEquals(repositoryService.advanceSequence(oid, result), 0L);
        assertEquals(repositoryService.advanceSequence(oid, result), 1L);
        assertEquals(repositoryService.advanceSequence(oid, result), 2L);
        assertEquals(repositoryService.advanceSequence(oid, result), 3L);
        assertEquals(repositoryService.advanceSequence(oid, result), 4L);
        repositoryService.returnUnusedValuesToSequence(oid, Arrays.asList(2L, 4L), result);
        assertEquals(repositoryService.advanceSequence(oid, result), 2L);
        assertEquals(repositoryService.advanceSequence(oid, result), 4L);
        assertEquals(repositoryService.advanceSequence(oid, result), 5L);
        assertEquals(repositoryService.advanceSequence(oid, result), 6L);
        repositoryService.returnUnusedValuesToSequence(oid, null, result);
        repositoryService.returnUnusedValuesToSequence(oid, new ArrayList<>(), result);
        repositoryService.returnUnusedValuesToSequence(oid, Collections.singletonList(6L), result);
        assertEquals(repositoryService.advanceSequence(oid, result), 6L);
        repositoryService.returnUnusedValuesToSequence(oid,
                Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L), result); // only 0-4 will be returned
        assertEquals(repositoryService.advanceSequence(oid, result), 0L);
        assertEquals(repositoryService.advanceSequence(oid, result), 1L);
        assertEquals(repositoryService.advanceSequence(oid, result), 2L);
        assertEquals(repositoryService.advanceSequence(oid, result), 3L);
        assertEquals(repositoryService.advanceSequence(oid, result), 4L);
        assertEquals(repositoryService.advanceSequence(oid, result), 7L);
        assertEquals(repositoryService.advanceSequence(oid, result), 8L);
        assertEquals(repositoryService.advanceSequence(oid, result), 9L);
        assertEquals(repositoryService.advanceSequence(oid, result), 0L);
        assertEquals(repositoryService.advanceSequence(oid, result), 1L);
        assertEquals(repositoryService.advanceSequence(oid, result), 2L);
    }

    @Test
    public void test020ReachingLimit() throws Exception {
        OperationResult result = createOperationResult();
        String oid = repositoryService.addObject(
                new SequenceType()
                        .name("Sequence 0-9")
                        .counter(0L)
                        .maxCounter(9L)
                        .asPrismObject(),
                null, result);

        assertEquals(repositoryService.advanceSequence(oid, result), 0L);
        assertEquals(repositoryService.advanceSequence(oid, result), 1L);
        assertEquals(repositoryService.advanceSequence(oid, result), 2L);
        assertEquals(repositoryService.advanceSequence(oid, result), 3L);
        assertEquals(repositoryService.advanceSequence(oid, result), 4L);
        assertEquals(repositoryService.advanceSequence(oid, result), 5L);
        assertEquals(repositoryService.advanceSequence(oid, result), 6L);
        assertEquals(repositoryService.advanceSequence(oid, result), 7L);
        assertEquals(repositoryService.advanceSequence(oid, result), 8L);
        assertEquals(repositoryService.advanceSequence(oid, result), 9L);
        Assertions.assertThatThrownBy(() -> repositoryService.advanceSequence(oid, result))
                .isInstanceOf(SystemException.class);
    }
}

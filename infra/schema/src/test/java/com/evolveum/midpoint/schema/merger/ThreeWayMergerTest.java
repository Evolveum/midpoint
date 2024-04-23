/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.io.File;
import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.merger.threeway.MergeResult;
import com.evolveum.midpoint.schema.merger.threeway.ThreeWayMerger;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class ThreeWayMergerTest extends AbstractSchemaTest {

    private static final File TEST_DIRECTORY = new File("./src/test/resources/merger/3way");

    @Test
    public void test10RoleChanges() throws Exception {
        Request<RoleType> request = prepareRequest("role");

        ThreeWayMerger<RoleType> merger = new ThreeWayMerger<>();
        MergeResult result = merger.computeChanges(request.left(), request.base(), request.right());

        new AssertMergeFragment(result)
                .assertSize(3)
                        .assertHasConflict();
    }

    private <O extends ObjectType> Request<O> prepareRequest(String filename)
            throws SchemaException, IOException {

        File baseFile = new File(TEST_DIRECTORY, filename + "-base.xml");
        File leftFile = new File(TEST_DIRECTORY, filename + "-left.xml");
        File rightFile = new File(TEST_DIRECTORY, filename + "-right.xml");

        PrismContext ctx = PrismTestUtil.getPrismContext();

        PrismObject<O> base = ctx.parseObject(baseFile);
        PrismObject<O> left = ctx.parseObject(leftFile);
        PrismObject<O> right = ctx.parseObject(rightFile);

        return new Request<>(baseFile, leftFile, rightFile, base, left, right);
    }

    private record Request<O extends ObjectType>(
            File baseFile,
            File leftFile,
            File rightFile,
            PrismObject<O> base,
            PrismObject<O> left,
            PrismObject<O> right) {
    }
}

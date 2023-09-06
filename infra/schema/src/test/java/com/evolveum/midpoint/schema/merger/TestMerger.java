/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.merger.object.LookupTableMergeOperation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class TestMerger extends AbstractSchemaTest {

    private static final File TEST_ROOT_DIR = new File("./src/test/resources/merger");

    @Test
    public void testLookupTableMergeOperation() throws Exception {
        PrismObject<LookupTableType> source = getPrismContext().parseObject(new File(TEST_ROOT_DIR, "lookup-table-source.xml"));
        PrismObject<LookupTableType> target = getPrismContext().parseObject(new File(TEST_ROOT_DIR, "lookup-table-target.xml"));
        PrismObject<LookupTableType> result = getPrismContext().parseObject(new File(TEST_ROOT_DIR, "lookup-table-result.xml"));

        LookupTableMergeOperation operation = new LookupTableMergeOperation(target.asObjectable(), source.asObjectable());
        operation.execute();

        System.out.println("Merged object:\n" + target.debugDump());
        // TODO

        Assertions.assertThat(target)
                        .matches(t -> t.equivalent(result));
    }
}

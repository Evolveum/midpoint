/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.performance;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestFilterPerformance extends AbstractSchemaPerformanceTest {

    @Test
    public void test100UseFilter() throws Exception {
        PrismObject<UserType> jack = getJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("orig", "norm")
                .or().item(ArchetypeType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ARCHIVED)
                .or().item(ArchetypeType.F_COST_CENTER).eq("cc100").matchingCaseIgnore()
                .buildFilter();

        measure("filter.match.jack",
                "Filter (name = poly(orig,norm) OR (activation/administrativeStatus EXISTS) OR cost_center = cc100)",
                () -> { filter.match(jack.asObjectable().asPrismContainerValue(), matchingRuleRegistry); return true; });
    }
}

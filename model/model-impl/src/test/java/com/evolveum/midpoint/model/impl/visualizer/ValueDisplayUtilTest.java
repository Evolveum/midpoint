/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ValueDisplayUtilTest extends AbstractInternalModelIntegrationTest {

    @Test
    public void test010DisplayConstruction() {
        ConstructionType construction = new ConstructionType();
        construction.description("some description");
        construction.resourceRef("123", SchemaConstants.ORG_DEFAULT);

        PrismPropertyValue ppv = new PrismPropertyValueImpl(construction);

        PolyString ps = ValueDisplayUtil.toStringValue(ppv);

        String value = localizationService.translate(ps);
        System.out.println(value);
    }
}

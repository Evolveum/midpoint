/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;

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

        AssertJUnit.assertEquals("resource object on 123: some description", value);
    }

    @Test
    public void test020ResourceAttributeDefinitionType() {
        ResourceAttributeDefinitionType def = new ResourceAttributeDefinitionType();
        def.ref(new ItemPathType(ItemPath.create(UserType.F_ASSIGNMENT)));
        def.outbound(new MappingType()
                .strength(MappingStrengthType.WEAK)
                .expression(
                        new ExpressionType()
                                .expressionEvaluator(new ObjectFactory().createScript(new ScriptExpressionEvaluatorType()
                                        .code("some code here")))
                                .expressionEvaluator(new ObjectFactory().createAsIs(new AsIsExpressionEvaluatorType()))
                )
        );

        PrismPropertyValue ppv = new PrismPropertyValueImpl(def);

        PolyString ps = ValueDisplayUtil.toStringValue(ppv);

        String value = localizationService.translate(ps);
        AssertJUnit.assertEquals("assignment = (a complex expression), (a complex expression), ", value);
    }
}

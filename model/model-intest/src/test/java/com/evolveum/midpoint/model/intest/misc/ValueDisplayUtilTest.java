/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.misc;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ValueDisplayUtilTest extends AbstractEmptyModelIntegrationTest {

    @Test
    public void test010DisplayConstruction() {
        ConstructionType construction = new ConstructionType();
        construction.description("some description");
        construction.resourceRef("123", SchemaConstants.ORG_DEFAULT);

        PrismPropertyValue ppv = new PrismPropertyValueImpl(construction);

        LocalizableMessage msg = ValueDisplayUtil.toStringValue(ppv);

        String value = localizationService.translate(msg);

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

        LocalizableMessage msg = ValueDisplayUtil.toStringValue(ppv);

        String value = localizationService.translate(msg);
        AssertJUnit.assertEquals("assignment = (a complex expression), (a complex expression)", value);
    }

    @Test
    public void test030Calendar() {
        XMLGregorianCalendar cal = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        PrismPropertyValue ppv = new PrismPropertyValueImpl(cal);

        LocalizableMessage msg = ValueDisplayUtil.toStringValue(ppv);

        Locale locale = Locale.GERMAN;

        String value = localizationService.translate(msg, locale);

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
        String expected = df.format(cal.toGregorianCalendar().getTime());

        AssertJUnit.assertEquals(expected, value);
    }

    @Test
    public void test035Boolean() {
        PrismPropertyValue ppv = new PrismPropertyValueImpl(true);

        LocalizableMessage msg = ValueDisplayUtil.toStringValue(ppv);
        AssertJUnit.assertNotNull(msg);
        AssertJUnit.assertEquals("Boolean.true", ((SingleLocalizableMessage) msg).getKey());

        String value = localizationService.translate(msg);

        AssertJUnit.assertEquals("true", value);
    }
}

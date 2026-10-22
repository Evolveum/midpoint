/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.web.util.ExpressionUtil.ExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Tests recognition of the expression evaluator type from the expression bean.
 */
public class ExpressionUtilTest extends AbstractGuiUnitTest {

    private static final Object[][] EVALUATORS = {
            { SchemaConstantsGenerated.C_VALUE, ExpressionEvaluatorType.LITERAL },
            { SchemaConstantsGenerated.C_AS_IS, ExpressionEvaluatorType.AS_IS },
            { SchemaConstantsGenerated.C_PATH, ExpressionEvaluatorType.PATH },
            { SchemaConstantsGenerated.C_SCRIPT, ExpressionEvaluatorType.SCRIPT },
            { SchemaConstantsGenerated.C_GENERATE, ExpressionEvaluatorType.GENERATE },
            { SchemaConstantsGenerated.C_ASSOCIATION_FROM_LINK, ExpressionEvaluatorType.ASSOCIATION_FROM_LINK },
            { SchemaConstantsGenerated.C_SHADOW_OWNER_REFERENCE_SEARCH, ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH },
            { SchemaConstantsGenerated.C_FILTER, ExpressionEvaluatorType.FILTER },
            { SchemaConstantsGenerated.C_NULL, ExpressionEvaluatorType.NULL }
    };

    @DataProvider
    public Object[][] evaluators() {
        return EVALUATORS;
    }

    @Test(dataProvider = "evaluators")
    public void testEvaluatorIsRecognized(QName elementName, ExpressionEvaluatorType expectedType) {
        ExpressionType expression = new ExpressionType();
        expression.expressionEvaluator(new JAXBElement<>(elementName, Object.class, null));

        assertEquals(ExpressionUtil.getExpressionType(expression), expectedType);
    }

    @Test
    public void testAllEvaluatorTypesAreTested() {
        Set<ExpressionEvaluatorType> testedTypes = Arrays.stream(EVALUATORS)
                .map(evaluator -> (ExpressionEvaluatorType) evaluator[1])
                .collect(Collectors.toSet());

        assertEquals(testedTypes, EnumSet.allOf(ExpressionEvaluatorType.class),
                "Every evaluator type has to be tested, add the new one to EVALUATORS (and ExpressionUtil.EVALUATOR_ELEMENTS)");
    }

    @Test
    public void testEmptyExpressionIsNotRecognized() {
        assertNull(ExpressionUtil.getExpressionType(new ExpressionType()));
    }
}

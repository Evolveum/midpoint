/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import java.io.File;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestModelExpressionsMel extends AbstractModelExpressionsTest {

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "mel");
    }

    @Test
    public void testGetUserByOid() throws Exception {
        // MEL does not have getUserByOid() function (it is not secure).
    }


    @Test
    public void testLibHello0Simple() throws Exception {
        PrismContainerValue<Containerable> customPcv = createCustomValue();

        assertExecuteScriptExpressionString(VariablesMap.create(prismContext), "Hello world!");
    }

    @Test
    public void testLibHello1Simple() throws Exception {
        PrismContainerValue<Containerable> customPcv = createCustomValue();

        VariablesMap variables = VariablesMap.create(prismContext,
                "foo", "Foobar", PrimitiveType.STRING);

        assertExecuteScriptExpressionString(variables, "Hello Foobar");
    }

    @Test
    public void testGetObjectTypeString() throws Exception {
        assertExecuteScriptExpressionString(
                createVariables(
                        "type", UserType.COMPLEX_TYPE.getLocalPart(), PrimitiveType.STRING,
                        "oid", CHEF_OID, PrimitiveType.STRING
                ),
                "get-object",
                CHEF_NAME);
    }

    @Test
    public void testGetObjectTypeQNameNs() throws Exception {
        assertExecuteScriptExpressionString(
                createVariables(
                        "type", UserType.COMPLEX_TYPE, PrimitiveType.QNAME,
                        "oid", CHEF_OID, PrimitiveType.STRING
                ),
                "get-object",
                CHEF_NAME);
    }

    @Test
    public void testGetObjectTypeQNameNoNs() throws Exception {
        assertExecuteScriptExpressionString(
                createVariables(
                        "type", new QName(UserType.COMPLEX_TYPE.getLocalPart()), PrimitiveType.QNAME,
                        "oid", CHEF_OID, PrimitiveType.STRING
                ),
                "get-object",
                CHEF_NAME);
    }

    @Test
    public void testSearchObjectsGuybrush() throws Exception {
        assertExecuteScriptExpressionStringList(
                createFocusProjectionResourceVariables(
                        "type", UserType.COMPLEX_TYPE, PrimitiveType.QNAME,
                        "filter", "name = '"+USER_GUYBRUSH_USERNAME+"'", PrimitiveType.STRING
                ),
                "search-objects",
                USER_GUYBRUSH_USERNAME);
    }

    @Test
    public void testSearchObjectsAllUsers() throws Exception {
        assertExecuteScriptExpressionStringList(
                createFocusProjectionResourceVariables(
                        "type", UserType.COMPLEX_TYPE.getLocalPart(), PrimitiveType.STRING,
                        "filter", null, PrimitiveType.STRING
                ),
                "search-objects",
                USER_ADMINISTRATOR_USERNAME,
                USER_JACK_USERNAME,
                USER_BARBOSSA_USERNAME,
                USER_GUYBRUSH_USERNAME,
                USER_ELAINE_USERNAME,
                "cheese", "cheese jr.", "chef", "barkeeper", "carla", "lechuck", "bob");
    }

    @Test
    public void testSearchShadowOwnerNone() throws Exception {
        assertExecuteScriptExpressionString(
                createFocusProjectionResourceVariables(
                        "oid", "00000000-0b40-11f1-9c0d-cbf71939ee01", PrimitiveType.STRING
                ),
                "search-shadow-owner",
                null);
    }

    @Test
    public void testSearchShadowOwnerGuybrush() throws Exception {
        assertExecuteScriptExpressionString(
                createFocusProjectionResourceVariables(
                        "oid", ACCOUNT_SHADOW_GUYBRUSH_OID, PrimitiveType.STRING
                ),
                "search-shadow-owner",
                USER_GUYBRUSH_USERNAME);
    }

}

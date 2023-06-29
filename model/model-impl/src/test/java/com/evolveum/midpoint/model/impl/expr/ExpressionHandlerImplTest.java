/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ExpressionHandlerImplTest extends AbstractSpringTest
        implements InfraTestMixin {

    private static final File TEST_FOLDER = new File("./src/test/resources/expr");
    private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");

    @Autowired
    private ExpressionHandler expressionHandler;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        // just something to fill into c:actor expression variable (using random OID)
        MidPointPrincipal principal = new MidPointPrincipal(new UserType().oid("9f5d66bc-d45c-4d30-97b5-11285a11a2e3"));
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
        securityContext.setAuthentication(authentication);
    }

    // This test is wrong. Maybe wrong place.
    // But the problem is, that the account here contains raw values. It does not have
    // the definition applied. Therefore the equals() in groovy won't work.
    @Test(enabled = false)
    public void testConfirmUser() throws Exception {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(
                TEST_FOLDER, "account-xpath-evaluation.xml"));

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_FOLDER, "user-new.xml"));

        //TODO:  "$c:user/c:givenName/t:orig replaced with "$c:user/c:givenName
        ExpressionType expression = PrismTestUtil.parseAtomicValue(
                "<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "<script>\n"
                        + "<trace>true</trace>\n"
                        + "<code>"
                        + "basic.getAttributeValues(projection, \"givenName\")?.contains(user.getGivenName().getOrig())"
                        + "</code>"
                        + "</script>"
                        + "</object>", ExpressionType.COMPLEX_TYPE);

        OperationResult result = createOperationResult();
        boolean confirmed = expressionHandler.evaluateConfirmationExpression(
                user.asObjectable(), account.asObjectable(), expression, null, result);
        logger.info(result.debugDump());

        assertTrue("Wrong expression result (expected true)", confirmed);
    }

    @Test
    public void testEvaluateExpression() throws Exception {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(TEST_FOLDER, "account.xml"));
        ShadowType accountType = account.asObjectable();

        // Needed for the expression evaluation.
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "resource-dummy.xml"));
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.asReferenceValue().setObject(resource);
        accountType.setResourceRef(resourceRef);

        ExpressionType expression = PrismTestUtil.parseAtomicValue(
                "<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "  <path>declare namespace icfs=\"http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3\";\n"
                        + "    $account/attributes/icfs:name</path>\n"
                        + "</object>",
                ExpressionType.COMPLEX_TYPE);
        logger.debug("Expression: {}", SchemaDebugUtil.prettyPrint(expression));

        OperationResult result = createOperationResult();
        String name = expressionHandler.evaluateExpression(accountType, expression, "test expression", null, result);
        logger.info(result.debugDump());

        assertEquals("Wrong expression result", "hbarbossa", name);
    }
}

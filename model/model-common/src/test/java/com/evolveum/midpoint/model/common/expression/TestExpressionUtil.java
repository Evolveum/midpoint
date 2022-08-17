/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;

import com.evolveum.midpoint.repo.common.DirectoryFileObjectResolver;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestExpressionUtil extends AbstractModelCommonTest {

    public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    public static final File USER_JACK_FILE = new File(MidPointTestConstants.OBJECTS_DIR, USER_JACK_OID + ".xml");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testResolvePathStringProperty() throws Exception {
        // GIVEN

        // WHEN
        PrismProperty<String> resolvedProperty = resolvePath("$user/description", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved property value", "jack", resolvedProperty.getRealValue());
    }

    @Test
    public void testResolvePathPolyStringProperty() throws Exception {
        // GIVEN

        // WHEN
        PrismProperty<PolyString> resolvedProperty = resolvePath("$user/fullName", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved property value", PrismTestUtil.createPolyString("Jack Sparrow"),
                resolvedProperty.getRealValue());
    }

    @Test
    public void testResolvePathPolyStringOrig() throws Exception {
        // GIVEN

        // WHEN
        String resolved = resolvePath("$user/fullName/t:orig", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved property value", "Jack Sparrow", resolved);
    }

    @Test
    public void testResolvePathPolyStringNorm() throws Exception {
        // GIVEN

        // WHEN
        String resolved = resolvePath("$user/fullName/t:norm", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved property value", "jack sparrow", resolved);
    }

    @Test
    public void testResolvePathPolyStringOdo() throws Exception {
        // GIVEN

        // WHEN
        ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
        assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

        assertTrue("Wrong residual path: "+idi.getResidualPath(),
                idi.getResidualPath() == null || idi.getResidualPath().isEmpty());

    }

    @Test
    public void testResolvePathPolyStringOdoOrig() throws Exception {
        // GIVEN

        // WHEN
        ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName/t:orig", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
        assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

        PrismAsserts.assertPathEquivalent("Wrong residual path", PolyString.F_ORIG, idi.getResidualPath());
    }

    @Test
    public void testResolvePathPolyStringOdoNorm() throws Exception {
        // GIVEN

        // WHEN
        ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName/t:norm", getTestNameShort());

        // THEN
        assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
        assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
                ((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

        PrismAsserts.assertPathEquivalent("Wrong residual path", PolyString.F_NORM, idi.getResidualPath());

    }

    private <T> T resolvePath(String path, final String exprShortDesc)
            throws SchemaException, ObjectNotFoundException, IOException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        VariablesMap variables = createVariables();
        return resolvePath(path, variables, exprShortDesc);
    }

    private <T> T resolvePathOdo(String path, final String exprShortDesc)
            throws SchemaException, ObjectNotFoundException, IOException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        VariablesMap variables = createVariablesOdo();
        return resolvePath(path, variables, exprShortDesc);
    }

    private <T> T resolvePath(String path, VariablesMap variables, String exprShortDesc)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createOperationResult();
        ItemPath itemPath = toItemPath(path);

        // WHEN
        ObjectResolver objectResolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
        Object resolved = ExpressionUtil.resolvePathGetValue(
                itemPath,
                variables,
                false,
                null,
                objectResolver,
                exprShortDesc,
                new NullTaskImpl(),
                result);

        // THEN
        IntegrationTestTools.display("Resolved", resolved);

        //noinspection unchecked
        return (T) resolved;
    }

    private VariablesMap createVariables() throws SchemaException, IOException {
        VariablesMap variables = new VariablesMap();
        PrismObject<UserType> user = createUser();
        variables.addVariableDefinition(ExpressionConstants.VAR_USER, user, user.getDefinition());
        variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, user, user.getDefinition());
        return variables;
    }

    private VariablesMap createVariablesOdo() throws SchemaException, IOException {
        VariablesMap variables = new VariablesMap();
        PrismObject<UserType> userOld = createUser();
        ObjectDelta<UserType> delta = PrismTestUtil.getPrismContext().deltaFactory().object().createModificationReplaceProperty(UserType.class,
                userOld.getOid(), UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        ObjectDeltaObject<UserType> odo = new ObjectDeltaObject<>(userOld, delta, null, userOld.getDefinition());
        odo.recompute();
        variables.addVariableDefinition(ExpressionConstants.VAR_USER, odo, odo.getDefinition());
        variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, odo, odo.getDefinition());
        return variables;
    }

    private PrismObject<UserType> createUser() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(USER_JACK_FILE);
    }

    // the path can contain t:xyz elements
    private UniformItemPath toItemPath(String stringPath) {
        String xml = "<path " +
                "xmlns='"+SchemaConstants.NS_C+"' " +
                "xmlns:t='"+SchemaConstants.NS_TYPES+"'>" +
                stringPath + "</path>";
        Document doc = DOMUtil.parseDocument(xml);
        Element element = DOMUtil.getFirstChildElement(doc);
        return ItemPathParserTemp.parseFromElement(element);
    }
}

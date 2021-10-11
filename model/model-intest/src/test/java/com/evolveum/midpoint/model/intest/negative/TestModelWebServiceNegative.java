/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.negative;

import java.io.File;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.SchemaViolationFaultType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelWebServiceNegative extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/crud");

    /**
     * First tests are positive, to make sure that this method works.
     */
    @Test
    public void test100ModifyAccountExplicitType() throws Exception {
        // GIVEN
        ObjectDeltaType objectChange = createShadowReplaceChange(ACCOUNT_SHADOW_GUYBRUSH_OID,
                "attributes/"+DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "foo", DOMUtil.XSD_STRING);
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(objectChange);

        // WHEN
        modelWeb.executeChanges(deltaList, null);

        // THEN

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "foo");
    }

    /**
     * First tests are positive, to make sure that this method works.
     */
    @Test
    public void test110ModifyAccountImplicitType() throws Exception {
        // GIVEN
        ObjectDeltaType objectChange = createShadowReplaceChange(ACCOUNT_SHADOW_GUYBRUSH_OID,
                "attributes/"+DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "bar", null);
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(objectChange);

        // WHEN
        modelWeb.executeChanges(deltaList, null);

        // THEN

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "bar");
    }

    @Test
    public void test200ModifyAccountWrongExplicitType() {
        given();
        ObjectDeltaType objectChange = createShadowReplaceChange(ACCOUNT_SHADOW_GUYBRUSH_OID,
                "attributes/"+DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "42", DOMUtil.XSD_INT);
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(objectChange);

        expect();
        assertExecuteChangesFailure(deltaList, null, SchemaViolationFaultType.class, "Expected", "but got class");
    }


    private void assertExecuteChangesFailure(
            ObjectDeltaListType deltaList, ModelExecuteOptionsType options,
            Class<? extends FaultType> expectedFaultTypeClass, String... messagePatterns) {

        try {
            modelWeb.executeChanges(deltaList, options);

            AssertJUnit.fail("Unexpected success");
        } catch (FaultMessage f) {
            FaultType faultInfo = f.getFaultInfo();
            if (expectedFaultTypeClass.isAssignableFrom(faultInfo.getClass())) {
                // This is expected
                String message = f.getMessage();
                for (String pattern: messagePatterns) {
                    if (!message.contains(pattern)) {
                        AssertJUnit.fail("Exception message does not contain pattern '"+pattern+"': "+message);
                    }
                }
            } else {
                AssertJUnit.fail("Expected fault type of "+expectedFaultTypeClass+" but got "+faultInfo.getClass());
            }
        }
    }

    // TODO: more negative tests

    private ObjectDeltaType createShadowReplaceChange(String oid, String path, final String value, QName type) {
        ObjectDeltaType objectChange = new ObjectDeltaType();
        objectChange.setOid(oid);
        objectChange.setChangeType(ChangeTypeType.MODIFY);
        objectChange.setObjectType(ObjectTypes.SHADOW.getTypeQName());
        ItemDeltaType itemDeltaType = new ItemDeltaType();
        itemDeltaType.setModificationType(ModificationTypeType.REPLACE);
        ItemPathType itemPath = prismContext.itemPathParser().asItemPathType(path);
        itemDeltaType.setPath(itemPath);
        ValueParser<String> valueParser = new ValueParser<String>() {
            @Override
            public String parse(QName typeName, XNodeProcessorEvaluationMode mode) {
                return value;
            }

            @Override
            public boolean canParseAs(QName typeName) {
                return true;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public String getStringValue() {
                return value;
            }

            @Override
            public Map<String, String> getPotentiallyRelevantNamespaces() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ValueParser<String> freeze() {
                return this;
            }
        };
        PrimitiveXNode<String> xnode = prismContext.xnodeFactory().primitive(valueParser, type, type != null);
        RawType rawValue = new RawType(xnode, prismContext);
        itemDeltaType.getValue().add(rawValue);
        objectChange.getItemDelta().add(itemDeltaType);
        return objectChange;
    }
}

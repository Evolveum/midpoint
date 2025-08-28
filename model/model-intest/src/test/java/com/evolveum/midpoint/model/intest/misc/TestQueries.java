package com.evolveum.midpoint.model.intest.misc;

import java.io.File;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinitionResolver;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestQueries extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/misc");

    private static final String DOT_ATTRIBUTE_NAME = "custom.attribute";

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "resource-dummy-queries.xml",
            "8f82e457-6c6e-42d7-a433-1a346b1899ee",
            "resource-dummy",
            TestQueries::populateWithSchema);

    private DummyResourceContoller dummyResourceCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        DummyAccount account = new DummyAccount("jdoe");
        account.addAttributeValue(DummyAccount.ATTR_FULLNAME_NAME, "John Doe");
        account.addAttributeValue(DOT_ATTRIBUTE_NAME, "dot.value1");
        dummyResourceCtl.getDummyResource().addAccount(account);
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();
        controller.addAttrDef(controller.getAccountObjectClass(), DOT_ATTRIBUTE_NAME, String.class, false, false);
    }

    @Test
    public void testDotAttributeQuery() throws Exception {
        final PrismObjectDefinition<ShadowType> shadowDef = dummyResourceCtl.getRefinedAccountDefinition().getPrismObjectDefinition();

        final ItemDefinitionResolver resolver =
                new Resource.ResourceItemDefinitionResolver(dummyResourceCtl.getRefinedAccountDefinition());

        final String fullNameValue = "John Doe";
        String fullNameQueryStr = "attributes/fullname = '" + fullNameValue + "'";
        ObjectQuery fullNameReal = TypedQuery.parse(ShadowType.class, shadowDef, fullNameQueryStr).toObjectQuery();

        ObjectQuery fullNameExpected = PrismTestUtil.getPrismContext().queryFor(ShadowType.class, resolver)
                .item(dummyResourceCtl.getAttributeFullnamePath()).eq(fullNameValue)
                .build();

        Assertions.assertThat(fullNameExpected.equivalent(fullNameReal)).isTrue();

        final String dotValue = "dot.value1";
        String dotAttributeQueryStr = "attributes/ri:" + DOT_ATTRIBUTE_NAME + " = '" + dotValue + "'";
        ObjectQuery dotAttributeReal = TypedQuery.parse(ShadowType.class, shadowDef, dotAttributeQueryStr).toObjectQuery();

        ObjectQuery dotAttributeExpected = PrismTestUtil.getPrismContext().queryFor(ShadowType.class, resolver)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(SchemaConstants.NS_RI, DOT_ATTRIBUTE_NAME))).eq(dotValue)
                .build();

        Assertions.assertThat(dotAttributeExpected.equivalent(dotAttributeReal)).isTrue();
    }
}


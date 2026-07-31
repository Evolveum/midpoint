/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.lens.projector.projection.outbounds;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
public class OutboundMappingProcessingTest extends AbstractEmptyInternalModelTest {
    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "mapping/outboundProcessing");

    @Autowired
    private MappingFactory mappingFactory;
    @Autowired
    private MappingEvaluator mappingEvaluator;

    private ResourceType testResource;
    private ShadowType testShadow;
    private OutboundMappingProcessing outboundProcessing;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        this.testResource = createMinimalResource();
        this.testShadow = new ShadowType();
        this.testShadow.setKind(ShadowKindType.ACCOUNT);
        this.testShadow.setIntent("default");
    }

    @BeforeMethod
    public void instantiateSut() {
        this.outboundProcessing = new OutboundMappingProcessing(this.mappingFactory, this.mappingEvaluator,
                this.clock);
    }

    @Test
    public void mappingUsesAsIsExpression_mappingIsEvaluated_sourceValueShouldBeAppliedToTarget() throws Exception {
        given("Mapping is of an As Is type with 'full name' as a source property.");
        final MappingType mappingBean = new MappingType()
                .name("Test Outbound Mapping")
                .source(new VariableBindingDefinitionType()
                        .path(new ItemPathType(UserType.F_FULL_NAME)))
                .expression(new ExpressionType().expressionEvaluator(
                        new ObjectFactory().createAsIs(new AsIsExpressionEvaluatorType())));

        and("'name' as a target attribute");
        final ItemPath targetPath = ItemPath.create("name");
        // Technically, this is needed only to set the parent to the mapping. Without the parent it will fail with
        // error that it is not part of an object.
        this.testResource.getSchemaHandling().getObjectType().get(0).attribute(new ResourceAttributeDefinitionType()
                .ref(targetPath.toBean())
                .outbound(mappingBean));

        final UserType userType = new UserType();
        userType.setName(new PolyStringType("JLemon"));
        userType.setFullName(new PolyStringType("John Lemon"));

        and("Shadow contains different name then the user");
        this.testShadow.setName(new PolyStringType("test-shadow"));

        when("Mapping is evaluated.");
        final Collection<ItemDelta<?, ?>> deltas = outboundProcessing.executeToDeltas(
                targetPath,
                mappingBean,
                this.testShadow,
                this.testResource,
                userType,
                new SystemConfigurationType(),
                new NullTaskImpl(),
                new OperationResult("Mapping evaluation")
        );

        then("Delta should have one modification of the shadow name attribute.");
        assertEquals(deltas.size(), 1, "Should return exactly one delta");
        final ItemDelta<?, ?> delta = deltas.iterator().next();
        assertEquals(delta.getPath(), targetPath, "Delta path should match target");
        final String mappedAttributeValue = delta.getNewValues().iterator().next().getRealValue();
        and("Mapped value should correspond with the mapping source property.");
        assertEquals(mappedAttributeValue, "John Lemon", "Delta values should match the values from focus");
    }

    @Test
    public void mappingUsesScriptExpression_mappingIsEvaluated_expressionResultShouldBeAppliedToTarget() throws Exception {
        given("Mapping is of an Script Expression type with 'givenName' and 'familyName' as a source properties.");
        final MappingType mappingBean = new MappingType()
                .name("Test Outbound Mapping")
                .source(new VariableBindingDefinitionType()
                        .path(new ItemPathType(UserType.F_GIVEN_NAME)))
                .source(new VariableBindingDefinitionType()
                        .path(new ItemPathType(UserType.F_FAMILY_NAME)))
                .expression(new ExpressionType().expressionEvaluator(
                        new ObjectFactory().createScript(new ScriptExpressionEvaluatorType()
                                .code("givenName + '-' + familyName")
                                .language("mel"))));

        and("'name' as a target attribute");
        final ItemPath targetPath = ItemPath.create("name");
        // Technically, this is needed only to set the parent to the mapping. Without the parent it will fail with
        // error that it is not part of an object.
        this.testResource.getSchemaHandling().getObjectType().get(0).attribute(new ResourceAttributeDefinitionType()
                .ref(targetPath.toBean())
                .outbound(mappingBean));

        final UserType userType = new UserType();
        userType.setGivenName(new PolyStringType("John"));
        userType.setFamilyName(new PolyStringType("Lemon"));

        and("Shadow contains different name then the user");
        this.testShadow.setName(new PolyStringType("test-shadow"));

        when("Mapping is evaluated.");
        final Collection<ItemDelta<?, ?>> deltas = outboundProcessing.executeToDeltas(
                targetPath,
                mappingBean,
                this.testShadow,
                this.testResource,
                userType,
                new SystemConfigurationType(),
                new NullTaskImpl(),
                new OperationResult("Mapping evaluation")
        );

        then("Delta should have one modification of the shadow name attribute.");
        assertEquals(deltas.size(), 1, "Should return exactly one delta");
        final ItemDelta<?, ?> delta = deltas.iterator().next();
        assertEquals(delta.getPath(), targetPath, "Delta path should match target");
        final String mappedAttributeValue = delta.getNewValues().iterator().next().getRealValue();
        and("Mapped value should correspond with the mapping source property.");
        assertEquals(mappedAttributeValue, "John-Lemon", "Delta values should match the values from focus");
    }

    /**
     * Create a minimal resource with schema handling for ACCOUNT/DEFAULT object type.
     * The schema includes a testAttribute that can be used as a mapping target.
     */
    private static ResourceType createMinimalResource() throws SchemaException, IOException, ConfigurationException {
        // Parse a minimal resource XML from the test resources
        final File resourceFile = new File(TEST_DIR, "resource-minimal.xml");
        if (resourceFile.exists()) {
            final PrismObject<ResourceType> parsedResource = PrismTestUtil.parseObject(resourceFile);
            return parsedResource.asObjectable();
        }
        throw new ConfigurationException("Resource file " + resourceFile.getPath() + " does not exist.");
    }
}

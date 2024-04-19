/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorConfigurationOptions.CompleteSchemaProvider;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

import com.evolveum.midpoint.task.api.test.NullTaskImpl;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Simple UCF tests over dummy resource.
 *
 * This is an UCF test. It should not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public abstract class AbstractUcfDummyTest extends AbstractSpringTest
        implements InfraTestMixin {

    protected static final File RESOURCE_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "resource-dummy.xml");
    private static final File CONNECTOR_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "connector-dummy.xml");
    static final String ACCOUNT_JACK_USERNAME = "jack";

    ConnectorFactory connectorFactory;
    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceBean;
    ConnectorType connectorBean;
    protected ConnectorInstance cc;
    protected BareResourceSchema resourceSchema;
    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;

    @Autowired protected ConnectorFactory connectorFactoryIcfImpl;
    @Autowired protected PrismContext prismContext;
    @Autowired protected LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @BeforeClass
    public void setup() throws Exception {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.setResource(resource);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();

        connectorFactory = connectorFactoryIcfImpl;

        resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_FILE);
        resourceBean = resource.asObjectable();

        PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(CONNECTOR_DUMMY_FILE);
        connectorBean = connector.asObjectable();
    }

    @SuppressWarnings("SameParameterValue")
    void assertPropertyDefinition(
            PrismContainer<?> container, String propName, QName xsdType, int minOccurs, int maxOccurs) {
        QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    protected static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

    @SuppressWarnings("SameParameterValue")
    void assertContainerDefinition(
            PrismContainer<?> container, String contName, QName xsdType, int minOccurs, int maxOccurs) {
        QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
        PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }

    UcfExecutionContext createExecutionContext() {
        return createExecutionContext(resourceBean);
    }

    UcfExecutionContext createExecutionContext(ResourceType resource) {
        return new UcfExecutionContext(lightweightIdentifierGenerator, resource, NullTaskImpl.INSTANCE);
    }

    protected void configure(
            @NotNull ConnectorConfigurationType configuration,
            @Nullable List<QName> generateObjectClasses,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {
        displayDumpable("Using configuration", configuration);
        cc.configure(
                configuration.asPrismContainerValue(),
                new ConnectorConfigurationOptions()
                        .generateObjectClasses(generateObjectClasses)
                        .completeSchemaProvider(CompleteSchemaProvider.forResource(resourceBean)),
                result);
    }

}

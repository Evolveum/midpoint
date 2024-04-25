/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isAbstract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import static com.evolveum.midpoint.prism.Containerable.asPrismContainerValue;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ConfigurationPropertiesType;
import com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ResultsHandlerConfigurationType;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
@SuppressWarnings("UnusedReturnValue") // It's often the case for asserters returned.
public class ResourceAsserter<RA> extends PrismObjectAsserter<ResourceType, RA> {

    public ResourceAsserter(PrismObject<ResourceType> resource) {
        super(resource);
    }

    public ResourceAsserter(PrismObject<ResourceType> resource, String details) {
        super(resource, details);
    }

    public ResourceAsserter(PrismObject<ResourceType> resource, RA returnAsserter, String details) {
        super(resource, returnAsserter, details);
    }

    public static ResourceAsserter<Void> forResource(PrismObject<ResourceType> resource) {
        return new ResourceAsserter<>(resource);
    }

    public static ResourceAsserter<Void> forResource(PrismObject<ResourceType> resource, String details) {
        return new ResourceAsserter<>(resource, details);
    }

    @Override
    public ResourceAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public ResourceAsserter<RA> assertHasSchema() {
        Element schemaElement = ResourceTypeUtil.getResourceXsdSchemaElement(getObject());
        assertNotNull("No schema in " + desc(), schemaElement);
        return this;
    }

    public ResourceAsserter<RA> assertHasNoSchema() {
        Element schemaElement = ResourceTypeUtil.getResourceXsdSchemaElement(getObject());
        assertNull("Schema present in " + desc(), schemaElement);
        return this;
    }

    @Override
    public ResourceAsserter<RA> display() {
        super.display();
        return this;
    }

    @Override
    public ResourceAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public ResourceAsserter<RA> displayXml() throws SchemaException {
        super.displayXml();
        return this;
    }

    @Override
    public ResourceAsserter<RA> displayXml(String message) throws SchemaException {
        super.displayXml(message);
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public ResourceAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    public PrismContainerValueAsserter<OperationalStateType, ResourceAsserter<RA>> operationalState() {
        PrismContainerValue<OperationalStateType> operationalState = asPrismContainerValue(getObject().asObjectable().getOperationalState());
        PrismContainerValueAsserter<OperationalStateType, ResourceAsserter<RA>> asserter =
                new PrismContainerValueAsserter<>(operationalState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismContainerAsserter<OperationalStateType, ResourceAsserter<RA>> operationalStateHistory() {
        PrismContainer<OperationalStateType> operationalStateHistory =
                getObject().findContainer(ResourceType.F_OPERATIONAL_STATE_HISTORY);
        PrismContainerAsserter<OperationalStateType, ResourceAsserter<RA>> asserter =
                new PrismContainerAsserter<>(operationalStateHistory, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ResourceAsserter<RA> assertNotAbstract() {
        assertThat(isAbstract(getObjectable()))
                .withFailMessage("Resource is abstract although it should not be")
                .isFalse();
        return this;
    }

    public PrismContainerValueAsserter<ConfigurationPropertiesType, ResourceAsserter<RA>> configurationProperties()
            throws ConfigurationException {
        return configurationProperties(null);
    }

    /**
     * Although configuration properties are not part of the static schema, they are a standard part of ConnId
     * connector configuration. So we allow accessing them directly.
     */
    public PrismContainerValueAsserter<ConfigurationPropertiesType, ResourceAsserter<RA>> configurationProperties(
            @Nullable String connectorName) throws ConfigurationException {
        PrismContainerValue<ConfigurationPropertiesType> properties =
                Objects.requireNonNull(
                        ResourceTypeUtil.getConfigurationProperties(getObjectable(), connectorName),
                        "no configuration properties");
        PrismContainerValueAsserter<ConfigurationPropertiesType, ResourceAsserter<RA>> asserter =
                new PrismContainerValueAsserter<>(properties, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismContainerValueAsserter<ResultsHandlerConfigurationType, ResourceAsserter<RA>> resultsHandlerConfiguration()
            throws ConfigurationException {
        return resultsHandlerConfiguration(null);
    }

    /**
     * Similar to {@link #configurationProperties(String)}, results handler configuration is a standard part of ConnId config.
     */
    public PrismContainerValueAsserter<ResultsHandlerConfigurationType, ResourceAsserter<RA>> resultsHandlerConfiguration(
            @Nullable String connectorName) throws ConfigurationException {
        PrismContainerValue<ResultsHandlerConfigurationType> handlerConfig =
                Objects.requireNonNull(
                        ResourceTypeUtil.getResultsHandlerConfiguration(getObjectable(), connectorName),
                        "no configuration properties");
        PrismContainerValueAsserter<ResultsHandlerConfigurationType, ResourceAsserter<RA>> asserter =
                new PrismContainerValueAsserter<>(handlerConfig, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ResourceAsserter<RA> assertConnectorRefIgnoringMetadata(ObjectReferenceType expected) {
        assertThat(stripMetadata(getObjectable().getConnectorRef()))
                .as("connectorRef")
                .isEqualTo(stripMetadata(expected));
        return this;
    }

    private ObjectReferenceType stripMetadata(ObjectReferenceType ref) {
        if (ref != null && ref.asReferenceValue().hasValueMetadata()) {
            ObjectReferenceType clone = ref.clone();
            clone.asReferenceValue().getValueMetadata().clear();
            return clone;
        } else {
            return ref;
        }
    }

    public ResourceAsserter<RA> assertConnectorRef(ObjectReferenceType expected) {
        assertThat(getObjectable().getConnectorRef())
                .as("connectorRef")
                .isEqualTo(expected);
        return this;
    }

    public ResourceAsserter<RA> assertAdditionalConnectorsCount(int expected) {
        assertThat(getObjectable().getAdditionalConnector())
                .as("additional connectors configurations")
                .hasSize(expected);
        return this;
    }

    public ResourceAsserter<RA> assertGeneratedClasses(QName... expected) {
        assertThat(getGeneratedClasses())
                .as("generated classes")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    private @NotNull Collection<QName> getGeneratedClasses() {
        XmlSchemaType schema = getObjectable().getSchema();
        if (schema == null) {
            return List.of();
        }
        SchemaGenerationConstraintsType constraints = schema.getGenerationConstraints();
        return constraints != null ? constraints.getGenerateObjectClass() : List.of();
    }

    // FIXME fix for expected == 0
    public ResourceAsserter<RA> assertConfiguredCapabilities(int expected) {
        assertThat(CapabilityUtil.size(getConfiguredCapabilities()))
                .as("number of configured capabilities")
                .isEqualTo(expected);
        return this;
    }

    public PrismContainerValueAsserter<CapabilityType, ResourceAsserter<RA>> configuredCapability(
            Class<? extends CapabilityType> type) throws ConfigurationException {
        CapabilityType capability = CapabilityUtil.getCapability(getConfiguredCapabilities(), type);
        assertThat(capability).withFailMessage(() -> "no capability of " + type).isNotNull();
        //noinspection unchecked,rawtypes,ConstantConditions
        PrismContainerValueAsserter<CapabilityType, ResourceAsserter<RA>> asserter =
                new PrismContainerValueAsserter<>(capability.asPrismContainerValue(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private CapabilityCollectionType getConfiguredCapabilities() {
        CapabilitiesType capabilities =
                MiscUtil.requireNonNull(
                        getObjectable().getCapabilities(),
                        () -> new AssertionError("no capabilities"));
        return MiscUtil.requireNonNull(
                capabilities.getConfigured(),
                () -> new AssertionError("no configured capabilities"));
    }
}

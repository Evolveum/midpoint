/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.Containerable.asPrismContainerValue;

import static org.testng.AssertJUnit.assertNull;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author Radovan Semancik
 */
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
        Element schemaElement = ResourceTypeUtil.getResourceXsdSchema(getObject());
        assertNotNull("No schema in " + desc(), schemaElement);
        return this;
    }

    public ResourceAsserter<RA> assertHasNoSchema() {
        Element schemaElement = ResourceTypeUtil.getResourceXsdSchema(getObject());
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
}

/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author mederly
 */
public class TestImmutable extends AbstractSchemaTest {

    @Test
    public void test010SimpleProperty() {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismPropertyDefinition<String> displayNamePPD = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_DISPLAY_NAME);
        PrismProperty<String> displayNamePP = displayNamePPD.instantiate();
        displayNamePP.setRealValue("Big red ball");
        displayNamePP.freeze();

        // THEN
        try {
            displayNamePP.setRealValue("Small black cube");
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        try {
            displayNamePP.getValue().setValue("Green point");
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }
    }

    @Test
    public void test020DateProperty() {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismPropertyDefinition<XMLGregorianCalendar> datePPD = prismContext.definitionFactory().createPropertyDefinition(
                new QName(SchemaConstants.NS_C, "dateTime"), DOMUtil.XSD_DATETIME);
        PrismProperty<XMLGregorianCalendar> datePP = datePPD.instantiate();
        Date now = new Date();
        Date yesterday = new Date(now.getTime() - 86400000L);
        datePP.setRealValue(XmlTypeConverter.createXMLGregorianCalendar(now));
        datePP.freeze();

        // THEN
        try {
            datePP.setRealValue(XmlTypeConverter.createXMLGregorianCalendar(yesterday));
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        try {
            datePP.getValue().setValue(XmlTypeConverter.createXMLGregorianCalendar(yesterday));
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        // Testing that returned objects are in fact clones (disabled as not implemented yet)
//        XMLGregorianCalendar realValue = datePP.getAnyRealValue();
//        int hourPlus1 = (realValue.getHour() + 1) % 24;
//        realValue.setHour(hourPlus1);
//        assertEquals("Date was changed even if it should not", XmlTypeConverter.createXMLGregorianCalendar(now), datePP.getAnyRealValue());
    }

    @Test
    public void test030Reference() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismReferenceDefinition refPRD = prismContext.definitionFactory().createReferenceDefinition(new QName(SchemaConstants.NS_C, "ref"), ObjectReferenceType.COMPLEX_TYPE);
        PrismReference refPR = refPRD.instantiate();
        refPR.add(ObjectTypeUtil.createObjectRef("oid1", ObjectTypes.USER).asReferenceValue());
        refPR.freeze();

        // THEN
        try {
            refPR.replace(ObjectTypeUtil.createObjectRef("oid2", ObjectTypes.USER).asReferenceValue());
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        try {
            refPR.getValue().setOid("oid2");
            AssertJUnit.fail("Value was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }
    }

    @Test
    public void test100Resource() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<ResourceType> resource = prismContext.createObject(ResourceType.class);
        resource.setOid("oid1");
        resource.asObjectable().setName(PolyStringType.fromOrig("resource1"));

        SchemaHandlingType schemaHandling = new SchemaHandlingType(prismContext);
        ResourceObjectTypeDefinitionType objectTypeDef = new ResourceObjectTypeDefinitionType(prismContext);
        objectTypeDef.setDefault(true);
        IterationSpecificationType iterationSpecificationType = new IterationSpecificationType();
        iterationSpecificationType.setMaxIterations(100);
        objectTypeDef.setIteration(iterationSpecificationType);
        schemaHandling.getObjectType().add(objectTypeDef);
        resource.asObjectable().setSchemaHandling(schemaHandling);

        ResourceBusinessConfigurationType businessConfiguration = new ResourceBusinessConfigurationType(prismContext);
        businessConfiguration.setAdministrativeState(ResourceAdministrativeStateType.ENABLED);
        resource.asObjectable().setBusiness(businessConfiguration);

        resource.freeze();

        System.out.println("Resource: " + resource.debugDump());

        // THEN

        // standard property
        try {
            resource.asObjectable().setName(PolyStringType.fromOrig("resource2"));
            AssertJUnit.fail("Value of name was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        // OID
        try {
            resource.setOid("oid2");
            AssertJUnit.fail("Value of OID was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        // values in sub-container
        try {
            resource.asObjectable().getBusiness().setAdministrativeState(ResourceAdministrativeStateType.DISABLED);
            AssertJUnit.fail("Value of resource administrative status was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        try {
            resource.asObjectable().getSchemaHandling().getObjectType().get(0).setDefault(false);
            AssertJUnit.fail("Value of schemaHandling/[1]/default was changed when immutable!");
        } catch (RuntimeException e) {
            System.out.println("Got (expected) exception of " + e);
        }

        // value in a bean (not implemented yet)
//        try {
//            resource.asObjectable().getSchemaHandling().getObjectType().get(0).getIteration().setMaxIterations(500);
//            AssertJUnit.fail("Value of maxIterations was changed when immutable!");
//        } catch (RuntimeException e) {
//            System.out.println("Got (expected) exception of " + e);
//        }
    }

}

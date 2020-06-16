/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Optional;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Tests the value metadata handling. Currently the only "handling" is creation of metadata mock-up.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValueMetadata extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/metadata");

    private static final TestResource<UserType> USER_ALICE = new TestResource<>(TEST_DIR, "user-alice.xml", "9fc389be-5b47-4e9d-90b5-33fffd87b3ca");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(USER_ALICE, initTask, initResult);
    }

    @Test
    public void test010TestLiveMetadata() throws SchemaException {
        given();
        UserType mark = new UserType(prismContext)
                .name("mark");
        PrismPropertyValue<PolyString> nameValue = mark.asPrismObject()
                .findProperty(UserType.F_NAME)
                .getValue(PolyString.class);
        nameValue.createLiveMetadata();

        when();
        Optional<ValueMetadata> metadata = nameValue.valueMetadata();
        assertThat(metadata).isPresent();

        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        ValueMetadataType realMetadataValue = (ValueMetadataType) metadata.get().asContainerable();
        realMetadataValue.setProvisioning(new ProvisioningMetadataType(prismContext));
        realMetadataValue.getProvisioning().setLastProvisioningTimestamp(now);

        then();
        Optional<ValueMetadata> metadataAfter = nameValue.valueMetadata();
        assertThat(metadataAfter).isPresent();

        ValueMetadataType realMetadataValueAfter = (ValueMetadataType) metadataAfter.get().asContainerable();
        assertThat(realMetadataValueAfter.getProvisioning().getLastProvisioningTimestamp()).isEqualTo(now);
    }

    @Test
    public void test100CheckValueMetadata() throws Exception {
        given();

        when();
        PrismObject<UserType> alice = getUser(USER_ALICE.oid);

        Optional<ValueMetadata> objectMetadata = alice.getValue().valueMetadata();
        Optional<ValueMetadata> nameMetadata = alice.findItem(UserType.F_NAME).getValue().valueMetadata();
        Optional<ValueMetadata> givenNameMetadata = alice.findItem(UserType.F_GIVEN_NAME).getValue().valueMetadata();
        Optional<ValueMetadata> familyNameMetadata = alice.findItem(UserType.F_FAMILY_NAME).getValue().valueMetadata();
        Optional<ValueMetadata> fullNameMetadata = alice.findItem(UserType.F_FULL_NAME).getValue().valueMetadata();
        Optional<ValueMetadata> developmentMetadata = alice.findProperty(UserType.F_ORGANIZATIONAL_UNIT)
                .getAnyValue(ppv -> "Development".equals(PolyString.getOrig((PolyString) ppv.getRealValue())))
                .valueMetadata();
        //noinspection unchecked
        PrismContainerValue<AssignmentType> assignment111 = (PrismContainerValue<AssignmentType>) alice.find(ItemPath.create(UserType.F_ASSIGNMENT, 111L));
        Optional<ValueMetadata> assignmentMetadata = assignment111.valueMetadata();
        Optional<ValueMetadata> manualSubtypeMetadata = assignment111.findProperty(AssignmentType.F_SUBTYPE)
                .getAnyValue(ppv -> "manual".equals(ppv.getRealValue()))
                .valueMetadata();
        Optional<ValueMetadata> assignmentAdminStatusMetadata =
                assignment111.findProperty(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS))
                        .getValue().valueMetadata();
        then();

        display("alice after", alice);

        assertThat(objectMetadata).as("object metadata").isPresent();
        displayDumpable("object metadata", objectMetadata.get());
        assertThat(cast(objectMetadata).getProcess()).as("process metadata")
                .isNotNull()
                .extracting(ProcessMetadataType::getRequestTimestamp).as("request timestamp").isNotNull();

        assertThat(nameMetadata).as("name metadata").isPresent();
        displayDumpable("name metadata", nameMetadata.get());
        assertThat(cast(nameMetadata).getTransformation())
                .as("name transformation metadata")
                .isNotNull()
                .extracting(TransformationMetadataType::getSource)
                .asList().hasSize(1);
        assertThat(cast(nameMetadata).getTransformation().getSource().get(0).getKind())
                .as("name transformation source kind")
                .isEqualTo("http://midpoint.evolveum.com/data-provenance/source#resource");

        assertThat(givenNameMetadata).as("given name metadata").isPresent();
        displayDumpable("given name metadata", givenNameMetadata.get());

        assertThat(familyNameMetadata).as("family name metadata").isPresent();
        displayDumpable("family name metadata", familyNameMetadata.get());

        assertThat(fullNameMetadata).as("full name metadata").isPresent();
        displayDumpable("full name metadata", fullNameMetadata.get());

        assertThat(developmentMetadata).as("Development OU metadata").isPresent();
        displayDumpable("Development OU metadata", developmentMetadata.get());

        assertThat(assignmentMetadata).as("assignment[111] metadata").isPresent();
        displayDumpable("assignment[111] metadata", assignmentMetadata.get());

        assertThat(manualSubtypeMetadata).as("assignment[111] subtype of 'manual' metadata").isPresent();
        displayDumpable("assignment[111] subtype of 'manual' metadata", manualSubtypeMetadata.get());

        assertThat(assignmentAdminStatusMetadata).as("assignment[111] admin status metadata").isPresent();
        displayDumpable("assignment[111] admin status metadata", assignmentAdminStatusMetadata.get());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ValueMetadataType cast(Optional<ValueMetadata> metadata) {
        //noinspection OptionalGetWithoutIsPresent
        return (ValueMetadataType) (metadata.get().asContainerable());
    }
}

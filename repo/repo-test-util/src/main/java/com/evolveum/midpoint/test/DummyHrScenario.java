/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.icf.dummy.resource.DummyObjectClass;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.ri;
import static com.evolveum.midpoint.test.ObjectClassName.legacyCustom;

/** Represents the HR scenario residing on given dummy resource. */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyHrScenario extends AbstractDummyScenario {

    public final Person person = new Person();
    public final Contract contract = new Contract();
    public final OrgUnit orgUnit = new OrgUnit();

    public final PersonContract personContract = new PersonContract();
    public final ContractOrgUnit contractOrgUnit = new ContractOrgUnit();

    private DummyHrScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyHrScenario on(DummyResourceContoller controller) {
        return new DummyHrScenario(controller);
    }

    public DummyHrScenario initialize() {
        person.initialize();
        contract.initialize();
        orgUnit.initialize();
        personContract.initialize();
        contractOrgUnit.initialize();
        return this;
    }

    public class Person extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyCustom("person");

        public static class AttributeNames {
            public static final AttrName FIRST_NAME = ri("firstName");
            public static final AttrName LAST_NAME = ri("lastName");
            public static final AttrName TITLE = ri("title");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT = AssocName.ri("contract");
        }

        void initialize() {
            var oc = new DummyObjectClass();
            controller.addAttrDef(oc, AttributeNames.FIRST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LAST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.TITLE.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull String getNativeObjectClassName() {
            return OBJECT_CLASS_NAME.local();
        }
    }

    public class Contract extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyCustom("contract");

        public static class AttributeNames {
            public static final AttrName VALID_FROM = ri("validFrom");
            public static final AttrName VALID_TO = ri("validTo");
        }

        public static class LinkNames {
            public static final AssocName ORG = AssocName.ri("org");
        }

        void initialize() {
            var oc = new DummyObjectClass();
            controller.addAttrDef(oc, AttributeNames.VALID_FROM.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, AttributeNames.VALID_TO.local(), ZonedDateTime.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull String getNativeObjectClassName() {
            return OBJECT_CLASS_NAME.local();
        }
    }

    public class OrgUnit extends ScenarioObjectClass {

        // for simplicity, we are not reusing standard "org" class
        public static final ObjectClassName OBJECT_CLASS_NAME = legacyCustom("orgUnit");

        public static class AttributeNames {
            public static final AttrName DESCRIPTION = ri("description");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT = AssocName.ri("contract");
        }

        void initialize() {
            var oc = new DummyObjectClass();
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull String getNativeObjectClassName() {
            return OBJECT_CLASS_NAME.local();
        }
    }

    public class PersonContract extends ScenarioLinkClass {

        public static final String NAME = "personContract";

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME)
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Person.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Person.LinkNames.CONTRACT.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(true)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Contract.OBJECT_CLASS_NAME.local())
                                    .withMinOccurs(1)
                                    .withMaxOccurs(1)
                                    // invisible on contract side
                                    .build())
                            .build());
        }

        @Override
        public @NotNull String getLinkClassName() {
            return NAME;
        }
    }

    public class ContractOrgUnit extends ScenarioLinkClass {

        public static final String NAME = "contractOrg";

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME)
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Contract.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Contract.LinkNames.ORG.local())
                                    .withMinOccurs(1)
                                    .withMaxOccurs(1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(false)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(OrgUnit.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(OrgUnit.LinkNames.CONTRACT.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(false)
                                    .withExpandedByDefault(false)
                                    .build())
                            .build());
        }

        @Override
        public @NotNull String getLinkClassName() {
            return NAME;
        }
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.dummys;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.*;
import static com.evolveum.midpoint.test.ObjectClassName.custom;

import java.time.ZonedDateTime;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.*;

/**
 * Represents the HR scenario residing on given dummy resource. Its richer than the default one
 * in {@link DummyHrScenario}, enhanced by e.g. cost centers.
 */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyHrScenarioExtended extends AbstractDummyScenario {

    public final Person person = new Person();
    public final Contract contract = new Contract();
    public final OrgUnit orgUnit = new OrgUnit();
    public final CostCenter costCenter = new CostCenter();

    public final PersonContract personContract = new PersonContract();
    public final ContractOrgUnit contractOrgUnit = new ContractOrgUnit();
    public final ContractCostCenter contractCostCenter = new ContractCostCenter();

    private DummyHrScenarioExtended(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyHrScenarioExtended on(DummyResourceContoller controller) {
        return new DummyHrScenarioExtended(controller);
    }

    public DummyHrScenarioExtended initialize() {
        person.initialize();
        contract.initialize();
        orgUnit.initialize();
        costCenter.initialize();
        personContract.initialize();
        contractOrgUnit.initialize();
        contractCostCenter.initialize();
        return this;
    }

    public class Person extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("person");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName FIRST_NAME = ri("firstName");
            public static final AttrName LAST_NAME = ri("lastName");
            public static final AttrName TITLE = ri("title");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT = AssocName.ri("contract");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.addAttrDef(oc, AttributeNames.FIRST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LAST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.TITLE.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Contract extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("contract");

        public static class AttributeNames {
            public static final AttrName VALID_FROM = ri("validFrom");
            public static final AttrName VALID_TO = ri("validTo");
            public static final AttrName NOTE = ri("note");
            public static final AttrName RESPONSIBILITY = ri("responsibility");
        }

        public static class LinkNames {
            public static final AssocName ORG = AssocName.ri("org");
            public static final AssocName COST_CENTER = AssocName.ri("costCenter");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, AttributeNames.VALID_FROM.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, AttributeNames.VALID_TO.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, AttributeNames.NOTE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.RESPONSIBILITY.local(), String.class, false, true);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class OrgUnit extends ScenarioObjectClass {

        // for simplicity, we are not reusing standard "org" class
        public static final ObjectClassName OBJECT_CLASS_NAME = custom("orgUnit");

        public static class AttributeNames {
            public static final AttrName DESCRIPTION = ri("description");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT = AssocName.ri("contract");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class CostCenter extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("costCenter");

        public static class AttributeNames {
            public static final AttrName DESCRIPTION = ri("description");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT = AssocName.ri("contract");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class PersonContract extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("personContract");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Person.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Person.LinkNames.CONTRACT.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(true)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Contract.OBJECT_CLASS_NAME.local())
                                    .withMinOccurs(1) // necessary for cascading deletion operations
                                    .withMaxOccurs(1)
                                    .build())
                            .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }

    public class ContractOrgUnit extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("contractOrg");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
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
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }

    public class ContractCostCenter extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("contractCostCenter");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Contract.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Contract.LinkNames.COST_CENTER.local())
                                    .withMinOccurs(1)
                                    .withMaxOccurs(1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(false)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(CostCenter.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(CostCenter.LinkNames.CONTRACT.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(false)
                                    .withExpandedByDefault(false)
                                    .build())
                            .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }
}

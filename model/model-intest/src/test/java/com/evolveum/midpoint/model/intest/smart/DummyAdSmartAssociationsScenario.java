/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.*;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.icfsName;
import static com.evolveum.midpoint.test.AttrName.ri;
import static com.evolveum.midpoint.test.ObjectClassName.custom;

/** Represents multiple associations AD scenario residing on given dummy resource. */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyAdSmartAssociationsScenario extends AbstractDummyScenario {

    //subject classes
    public final Account account = new Account();
    public final Person person = new Person();

    //object classes
    public final Group group = new Group();
    public final Contract contract = new Contract();
    public final OrgUnit orgUnit = new OrgUnit();

    public final Expert expert = new Expert();


    //link classes
    // association person - contract
    public final PersonContract personContract = new PersonContract();

    // association account - groups (both direction)
    public final AccountGroup accountGroup = new AccountGroup();

    // association org - contract
    public final ContractOrgUnit contractOrgUnit = new ContractOrgUnit();

    public final ExpertAccount expertAccount = new ExpertAccount();


    private DummyAdSmartAssociationsScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyAdSmartAssociationsScenario on(DummyResourceContoller controller) {
        return new DummyAdSmartAssociationsScenario(controller);
    }

    public DummyAdSmartAssociationsScenario initialize() {
        //subject classes
        account.initialize();

        person.initialize();

        //object classes
        group.initialize();

        contract.initialize();
        orgUnit.initialize();

        expert.initialize();

        //link classes
        accountGroup.initialize();

        personContract.initialize();
        contractOrgUnit.initialize();

        expertAccount.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("account");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName DESCRIPTION = ri("description");
            public static final AttrName TYPE = ri("type");
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
        }

        void initialize() {
            // Account class already exists
            controller.addAttrDef(
                    controller.getAccountObjectClass(), AttributeNames.DESCRIPTION.local(),
                    String.class, false, false);
            controller.addAttrDef(
                    controller.getAccountObjectClass(), AttributeNames.TYPE.local(),
                    String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Group extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("group");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName TYPE = ri("type");
        }

        public static class LinkNames {
            public static final AssocName _MEMBER = AssocName.ri("_member"); // avoiding collision with legacy "member"
            // GROUP attribute is the same as in Account
        }

        void initialize() {
            // Group class already exists
            controller.addAttrDef(
                    controller.getGroupObjectClass(), AttributeNames.TYPE.local(), String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class AccountGroup extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("groupMembership");

        void initialize() {
            controller.addLinkClassDefinition(aLinkClassDefinition()
                    .withName(NAME.local())
                    .withFirstParticipant(aParticipant()
                            .withObjectClassNames(Account.OBJECT_CLASS_NAME.local(), Group.OBJECT_CLASS_NAME.local())
                            .withLinkAttributeName(Account.LinkNames.GROUP.local())
                            .withMaxOccurs(-1)
                            .withReturnedByDefault(true)
                            .build())
                    .withSecondParticipant(aParticipant()
                            .withObjectClassNames(Group.OBJECT_CLASS_NAME.local())
//                            .withLinkAttributeName(Group.LinkNames._MEMBER.local()) // ignored
                            .withMaxOccurs(-1)
                            .build())
                    .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
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
            controller.addAttrDef(oc, Person.AttributeNames.FIRST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, Person.AttributeNames.LAST_NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, Person.AttributeNames.TITLE.local(), String.class, false, false);
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
            public static final AttrName NAME = icfsName();
            public static final AttrName VALID_FROM = ri("validFrom");
            public static final AttrName VALID_TO = ri("validTo");
            public static final AttrName NOTE = ri("note");
        }

        public static class LinkNames {
            public static final AssocName ORG = AssocName.ri("org");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, Contract.AttributeNames.VALID_FROM.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, Contract.AttributeNames.VALID_TO.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, Contract.AttributeNames.NOTE.local(), String.class, false, false);
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

    public class OrgUnit extends ScenarioObjectClass {

        // for simplicity, we are not reusing standard "org" class
        public static final ObjectClassName OBJECT_CLASS_NAME = custom("orgUnit");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName DESCRIPTION = ri("description");
        }

        public static class LinkNames {
            public static final AssocName CONTRACT_ORG = AssocName.ri("contractOrg");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.addAttrDef(oc, OrgUnit.AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
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
                                    .withLinkAttributeName(OrgUnit.LinkNames.CONTRACT_ORG.local())
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

    public class Expert extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("expert");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName VALID_FROM = ri("validFrom");
            public static final AttrName VALID_TO = ri("validTo");
            public static final AttrName NOTE = ri("note");
        }

        public static class LinkNames {
            public static final AssocName EXPERT_REF = AssocName.ri("expertReference");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, Expert.AttributeNames.VALID_FROM.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, Expert.AttributeNames.VALID_TO.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, Expert.AttributeNames.NOTE.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class ExpertAccount extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("expertAccount");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Account.OBJECT_CLASS_NAME.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Expert.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Expert.LinkNames.EXPERT_REF.local())
                                    .withMaxOccurs(-1)
                                    .build())
                            .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.ObjectClassName.*;

import com.evolveum.icf.dummy.resource.DummyAccount;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the default scenario residing on given dummy resource.
 *
 * Assumes non-legacy schema when used by midPoint resource.
 */
@SuppressWarnings("WeakerAccess")
public class DummyDefaultScenario extends AbstractDummyScenario {

    private DummyDefaultScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public final Account account = new Account();
    public final Group group = new Group();
    public final GroupMembership groupMembership = new GroupMembership();
    public final AccountPrivilege accountPrivilege = new AccountPrivilege();

    public static DummyDefaultScenario on(DummyResourceContoller controller) {
        return new DummyDefaultScenario(controller);
    }

    public DummyDefaultScenario initialize() {
        account.initialize();
        // there is nothing to initialize for groups and privileges
        groupMembership.initialize();
        accountPrivilege.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("account");

        public static class AttributeNames {
            public static final AttrName FULLNAME = AttrName.ri(DummyAccount.ATTR_FULLNAME_NAME);
            public static final AttrName DESCRIPTION = AttrName.ri(DummyAccount.ATTR_DESCRIPTION_NAME);
            public static final AttrName INTERESTS = AttrName.ri(DummyAccount.ATTR_INTERESTS_NAME);
            public static final AttrName INTERNAL_ID = AttrName.ri(DummyAccount.ATTR_INTERNAL_ID);
            // privileges are not present here, as they are represented by reference attributes
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
            public static final AssocName PRIV = AssocName.ri("priv");
        }

        void initialize() {
            var oc = controller.getDummyResource().getAccountObjectClass();
            if (oc.getAttributeDefinitions().isEmpty()) {
                // It seems like this code is sometimes called with already-initialized resource. So this is a little hack,
                // until things are sorted out.
                controller.addAttrDef(oc, AttributeNames.FULLNAME.local(), String.class, false, false);
                controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
                controller.addAttrDef(oc, AttributeNames.INTERESTS.local(), String.class, false, true);
                controller.addAttrDef(oc, AttributeNames.INTERNAL_ID.local(), String.class, false, false);
            }
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Group extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyGroup("group");

        public static class AttributeNames {
            // nothing here, the only data are members now
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
            public static final AssocName MEMBER_REF = AssocName.ri("memberRef"); // must not collide with "members"
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Privilege extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyCustom("privilege");

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class GroupMembership extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("groupMembership");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(
                                            Account.OBJECT_CLASS_NAME.local(),
                                            Group.OBJECT_CLASS_NAME.local()) // others can be added here
                                    .withLinkAttributeName(Account.LinkNames.GROUP.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(false)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Group.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Group.LinkNames.MEMBER_REF.local()) // visible because of tests
                                    .withReturnedByDefault(false)
                                    .withExpandedByDefault(false)
                                    .withProvidingUnclassifiedReferences(true)
                                    .withMaxOccurs(-1)
                                    .build())
                            .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }

    public class AccountPrivilege extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("accountPrivilege");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Account.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Account.LinkNames.PRIV.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(false)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Privilege.OBJECT_CLASS_NAME.local())
                                    .build())
                            .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }
}

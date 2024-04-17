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

import org.jetbrains.annotations.NotNull;

/**
 * Represents the default scenario residing on given dummy resource.
 * No need for initialization of classes and attributes; only the native associations should be initialized (for now).
 */
public class DummyDefaultScenario extends AbstractDummyScenario {

    DummyDefaultScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public final GroupMembership groupMembership = new GroupMembership();
    public final AccountPrivilege accountPrivilege = new AccountPrivilege();

    public static DummyDefaultScenario on(DummyResourceContoller controller) {
        return new DummyDefaultScenario(controller);
    }

    public DummyDefaultScenario initialize() {
        groupMembership.initialize();
        accountPrivilege.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyAccount("account");

        public static class AttributeNames {
            // TODO
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
            public static final AssocName PRIV = AssocName.ri("priv");
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Group extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyGroup("group");

        public static class AttributeNames {
            // TODO
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
            public static final AssocName MEMBERS_INVISIBLE = AssocName.ri("members-invisible"); // name is not important
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Privilege extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = legacyCustom("privilege");

        public static class AttributeNames {
            // TODO
        }

        public static class LinkNames {
            public static final AssocName HOLDERS_INVISIBLE = AssocName.ri("holders-invisible"); // name is not important
        }

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
                                    .withInvisibleLinkAttributeName(Group.LinkNames.MEMBERS_INVISIBLE.local())
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
                                    .withInvisibleLinkAttributeName(Privilege.LinkNames.HOLDERS_INVISIBLE.local())
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

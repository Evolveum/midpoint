/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.associations;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.icfsName;
import static com.evolveum.midpoint.test.ObjectClassName.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.*;

/** Represents very simplified AD scenario residing on given dummy resource. */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyAdTrivialScenario extends AbstractDummyScenario {

    public final Account account = new Account();
    public final Group group = new Group();

    public final AccountGroup accountGroup = new AccountGroup();

    private DummyAdTrivialScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyAdTrivialScenario on(DummyResourceContoller controller) {
        return new DummyAdTrivialScenario(controller);
    }

    public DummyAdTrivialScenario initialize() {
        account.initialize();
        group.initialize();
        accountGroup.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("account");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
        }

        public static class LinkNames {
            public static final AssocName GROUP = AssocName.ri("group");
        }

        void initialize() {
            // Account class already exists
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
        }

        public static class LinkNames {
            // We do not use "member" to avoid collision with the built-in "member" attribute. TODO reconsider
            public static final AssocName ACCOUNT = AssocName.ri("account");
        }

        void initialize() {
            // Group class already exists
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class AccountGroup extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("accountGroup");

        void initialize() {
            controller.addLinkClassDefinition(aLinkClassDefinition()
                    .withName(NAME.local())
                    .withFirstParticipant(aParticipant()
                            .withObjectClassNames(Account.OBJECT_CLASS_NAME.local())
                            .withLinkAttributeName(Account.LinkNames.GROUP.local())
                            .withMaxOccurs(-1)
                            .withReturnedByDefault(true)
                            .build())
                    .withSecondParticipant(aParticipant()
                            .withObjectClassNames(Group.OBJECT_CLASS_NAME.local())
                            .withLinkAttributeName(Group.LinkNames.ACCOUNT.local())
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

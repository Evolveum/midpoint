/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.dummys;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.*;
import static com.evolveum.midpoint.test.ObjectClassName.*;

/** Represents the Document Management System scenario residing on given dummy resource. */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyDmsScenario extends AbstractDummyScenario {

    public final Account account = new Account();
    public final Document document = new Document();
    public final Access access = new Access();

    public final AccountAccess accountAccess = new AccountAccess();
    public final AccessDocument accessDocument = new AccessDocument();

    private DummyDmsScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyDmsScenario on(DummyResourceContoller controller) {
        return new DummyDmsScenario(controller);
    }

    public DummyDmsScenario initialize() {
        account.initialize();
        document.initialize();
        access.initialize();
        accountAccess.initialize();
        accessDocument.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("account");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
        }

        public static class LinkNames {
            public static final AssocName ACCESS = AssocName.ri("access");
        }

        void initialize() {
            // Account class already exists
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Access extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("access");

        public static class AttributeNames {
            public static final AttrName LEVEL = ri("level");
        }

        public static class LinkNames {
            public static final AssocName DOCUMENT = AssocName.ri("document");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, AttributeNames.LEVEL.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Document extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("document");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class AccountAccess extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("accountAccess");

        void initialize() {
            controller.addLinkClassDefinition(aLinkClassDefinition()
                    .withName(NAME.local())
                    .withFirstParticipant(aParticipant()
                            .withObjectClassNames(Account.OBJECT_CLASS_NAME.local())
                            .withLinkAttributeName(Account.LinkNames.ACCESS.local())
                            .withMaxOccurs(-1)
                            .withReturnedByDefault(true)
                            .withExpandedByDefault(true)
                            .build())
                    .withSecondParticipant(aParticipant()
                            .withObjectClassNames(Access.OBJECT_CLASS_NAME.local())
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

    public class AccessDocument extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("accessDocument");

        void initialize() {
            controller.addLinkClassDefinition(aLinkClassDefinition()
                    .withName(NAME.local())
                    .withFirstParticipant(aParticipant()
                            .withObjectClassNames(Access.OBJECT_CLASS_NAME.local())
                            .withLinkAttributeName(Access.LinkNames.DOCUMENT.local())
                            .withMinOccurs(1)
                            .withMaxOccurs(1)
                            .withReturnedByDefault(true)
                            .withExpandedByDefault(false)
                            .build())
                    .withSecondParticipant(aParticipant()
                            .withObjectClassNames(Document.OBJECT_CLASS_NAME.local())
                            .build())
                    .build());
        }

        @Override
        public @NotNull ObjectClassName getLinkClassName() {
            return NAME;
        }
    }
}

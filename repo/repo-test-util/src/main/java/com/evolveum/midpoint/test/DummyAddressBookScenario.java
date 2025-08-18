/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.test.AttrName.icfsName;
import static com.evolveum.midpoint.test.AttrName.ri;
import static com.evolveum.midpoint.test.ObjectClassName.custom;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyObjectClass;

/**
 * Represents the Address Book scenario residing on given dummy resource.
 *
 * Please use `<icfi:useLegacySchema>false</icfi:useLegacySchema>` in the resource definition, as it expects
 * the nicer `ri:person`-style object class names.
 */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyAddressBookScenario extends AbstractDummyScenario {

    public final Person person = new Person();
    public final Address address = new Address();
    public final Email email = new Email();

    public final PersonAddress personAddress = new PersonAddress();
    public final PersonEmail personEmail = new PersonEmail();

    private DummyAddressBookScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyAddressBookScenario on(DummyResourceContoller controller) {
        return new DummyAddressBookScenario(controller);
    }

    public DummyAddressBookScenario initialize() {
        person.initialize();
        address.initialize();
        email.initialize();
        personAddress.initialize();
        personEmail.initialize();
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

        /** For complex attributes (technically, links to embedded objects). */
        public static class LinkNames {
            public static final AttrName ADDRESS = AttrName.ri("address");
            public static final AttrName EMAIL = AttrName.ri("email");
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

    public class Address extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("address");

        public static class AttributeNames {
            public static final AttrName TYPE = ri("type");
            public static final AttrName PRIMARY = ri("primary");
            public static final AttrName STREET = ri("street");
            public static final AttrName CITY = ri("city");
            public static final AttrName ZIP = ri("zip");
            public static final AttrName COUNTRY = ri("country");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, AttributeNames.TYPE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.PRIMARY.local(), Boolean.class, false, false);
            controller.addAttrDef(oc, AttributeNames.STREET.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.CITY.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.ZIP.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.COUNTRY.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Email extends ScenarioObjectClass {

        // for simplicity, we are not reusing standard "org" class
        public static final ObjectClassName OBJECT_CLASS_NAME = custom("email");

        public static class AttributeNames {
            public static final AttrName TYPE = ri("type");
            public static final AttrName PRIMARY = ri("primary");
            public static final AttrName VALUE = ri("value");
        }

        void initialize() {
            var oc = DummyObjectClass.embedded();
            controller.addAttrDef(oc, AttributeNames.TYPE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.PRIMARY.local(), Boolean.class, false, false);
            controller.addAttrDef(oc, AttributeNames.VALUE.local(), String.class, false, false);
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class PersonAddress extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("personAddress");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Person.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Person.LinkNames.ADDRESS.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(true)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Address.OBJECT_CLASS_NAME.local())
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

    public class PersonEmail extends ScenarioLinkClass {

        public static final ObjectClassName NAME = custom("personEmail");

        void initialize() {
            controller.addLinkClassDefinition(
                    aLinkClassDefinition()
                            .withName(NAME.local())
                            .withFirstParticipant(aParticipant()
                                    .withObjectClassNames(Person.OBJECT_CLASS_NAME.local())
                                    .withLinkAttributeName(Person.LinkNames.EMAIL.local())
                                    .withMaxOccurs(-1)
                                    .withReturnedByDefault(true)
                                    .withExpandedByDefault(true)
                                    .build())
                            .withSecondParticipant(aParticipant()
                                    .withObjectClassNames(Email.OBJECT_CLASS_NAME.local())
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
}

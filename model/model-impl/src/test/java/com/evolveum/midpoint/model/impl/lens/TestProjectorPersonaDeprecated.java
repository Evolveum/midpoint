/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjectorPersonaDeprecated extends AbstractTestProjectorPersona {

    @Override
    protected File getPersonaRoleFile() {
        return ROLE_PERSONA_ADMIN_FILE;
    }

    @Override
    protected String getPersonaRoleOid() {
        return ROLE_PERSONA_ADMIN_OID;
    }

    @Override
    protected void assertPersonaSubtypeOrArchetype(PersonaConstructionType personaConstructionType) {
        PrismAsserts.assertEqualsCollectionUnordered("Wrong subtype", personaConstructionType.getTargetSubtype(), "admin");
    }
}

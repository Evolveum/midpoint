/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.io.File;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjectorPersona extends AbstractTestProjectorPersona {

    private static final File ARCHETYPE_ADMIN_FILE = new File(TEST_DIR, "archetype-admin.xml");
    private static final String ARCHETYPE_ADMIN_OID = "1783774f-042f-4baf-9812-8daf4ad1ce64";

    private static final File ROLE_PERSONA_ARCHETYPE_ADMIN_FILE = new File(TEST_DIR, "role-persona-archetype-admin.xml");
    private static final String ROLE_PERSONA_ARCHETYPE_ADMIN_OID = "e4a03293-cedb-4a49-94f0-e5cdd95f9252";


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(ARCHETYPE_ADMIN_FILE);
    }

    @Override
    protected File getPersonaRoleFile() {
        return ROLE_PERSONA_ARCHETYPE_ADMIN_FILE;
    }

    @Override
    protected String getPersonaRoleOid() {
        return ROLE_PERSONA_ARCHETYPE_ADMIN_OID;
    }

    @Override
    protected void assertPersonaSubtypeOrArchetype(PersonaConstructionType personaConstructionType) {
        PrismAsserts.assertEqualsCollectionUnordered("Wrong archetype", personaConstructionType.getArchetypeRef().stream().map(a -> a.getOid()).collect(Collectors.toList()), ARCHETYPE_ADMIN_OID);
    }
}

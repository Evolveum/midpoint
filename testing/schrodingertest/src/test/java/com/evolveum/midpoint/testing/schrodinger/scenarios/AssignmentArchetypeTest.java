/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by honchar
 */
public class AssignmentArchetypeTest extends AbstractSchrodingerTest {

    private static final File ARCHETYPE_BUSINESS_ROLE_FILE = new File("src/test/resources/configuration/objects/archetypes/archetype-business-role.xml");
    private static final String RELATIONS_CONTAINER_HEADER_KEY = "Relations";
    private static final String RELATION_CONTAINER_HEADER_KEY = "Relation";
    private static final String NEWLY_ADDED_RELATION_CONTAINER_HEADER_KEY = "RelationDefinitionType.details";

    @Test(priority = 0)
    public void importArchetypeBusinessRole() {
        importObject(ARCHETYPE_BUSINESS_ROLE_FILE);
    }

    @Test
    public void configureRelationDefinitions(){
        //TODO wait till MID-5144 fix
//        basicPage
//                .roleManagement()
//                    .form()
//                        .expandContainerPropertiesPanel(RELATIONS_CONTAINER_HEADER_KEY)
//                        .addNewContainerValue(RELATION_CONTAINER_HEADER_KEY, NEWLY_ADDED_RELATION_CONTAINER_HEADER_KEY)
//                        .expandContainerPropertiesPanel(NEWLY_ADDED_RELATION_CONTAINER_HEADER_KEY)

    }
}

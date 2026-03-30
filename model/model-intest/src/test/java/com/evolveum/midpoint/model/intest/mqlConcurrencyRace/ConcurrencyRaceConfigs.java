/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.io.File;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

abstract class ConcurrencyRaceConfigs extends ConcurrencyRaceEnums {

    protected static final File TEST_DIR = new File("src/test/resources/mql-concurrency-race");

    protected static final String PROP_SHORT_USERS = "midpoint.mqlRace.short.users";
    protected static final String PROP_SHORT_THREADS = "midpoint.mqlRace.short.threads";
    protected static final String PROP_SHORT_ROUNDS = "midpoint.mqlRace.short.rounds";
    protected static final String PROP_SHORT_HOT_SET = "midpoint.mqlRace.short.hotSet";
    protected static final String PROP_SHORT_HOT_REPEATS = "midpoint.mqlRace.short.hotRepeats";
    protected static final String PROP_MANUAL_ENABLED = "midpoint.mqlRace.manual.enabled";
    protected static final String PROP_MANUAL_USERS = "midpoint.mqlRace.manual.users";
    protected static final String PROP_MANUAL_THREADS = "midpoint.mqlRace.manual.threads";
    protected static final String PROP_MANUAL_DURATION_SECONDS = "midpoint.mqlRace.manual.durationSeconds";
    protected static final String PROP_MANUAL_HOT_SET = "midpoint.mqlRace.manual.hotSet";
    protected static final String PROP_MANUAL_HOT_REPEATS = "midpoint.mqlRace.manual.hotRepeats";
    protected static final String PROP_SERIALIZE_PER_USER = "midpoint.mqlRace.serializePerUser";

    protected static final int DEFAULT_SHORT_USERS = 400;
    protected static final int DEFAULT_SHORT_THREADS = 16;
    protected static final int DEFAULT_SHORT_ROUNDS = 10;
    protected static final int DEFAULT_SHORT_HOT_SET = 64;
    protected static final int DEFAULT_SHORT_HOT_REPEATS = 3;
    protected static final int DEFAULT_MANUAL_USERS = 4000;
    protected static final int DEFAULT_MANUAL_THREADS = 40;
    protected static final int DEFAULT_MANUAL_DURATION_SECONDS = 900;
    protected static final int DEFAULT_MANUAL_HOT_SET = 128;
    protected static final int DEFAULT_MANUAL_HOT_REPEATS = 12;
    protected static final int DEFAULT_NOISE_OBJECTS = 250;

    protected static final String NS_RACE = "http://midpoint.evolveum.com/xml/ns/test/mql-concurrency-race";

    protected static final QName RACE_WORKPLACE_ID = new QName(NS_RACE, "workplaceId");
    protected static final QName RACE_MANAGER_PERSONAL_NUMBER = new QName(NS_RACE, "managerPersonalNumber");
    protected static final QName RACE_INITIAL_OWNER_NAME = new QName(NS_RACE, "initialOwnerName");
    protected static final QName RACE_SAP_CODE_1 = new QName(NS_RACE, "sapCode1");
    protected static final QName RACE_EX_ROLE_SAP_CODE_1 = new QName(NS_RACE, "exRoleSapCode1");
    protected static final QName RACE_EX_ROLE_SAP_CODE = new QName(NS_RACE, "exRoleSapCode");
    protected static final QName RACE_EX_ROLE_APP_INSTANCE = new QName(NS_RACE, "exRoleApplicationInstanceName");
    protected static final QName RACE_EX_ROLE_VARIANT = new QName(NS_RACE, "exRoleVariant");

    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_SHADOW = template(
            "object-template-mql.xml", "7c46f8cf-1d7a-45df-9a67-c1a0d61b0801");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_SHADOW = template(
            "object-template-xml.xml", "f6d8bb9b-44f7-4c1b-a8f7-fce29f092f5d");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_SINGLE = template(
            "object-template-mql-single.xml", "d7e3f3ab-6f4f-4fb7-b6b6-e2f6c5461201");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_SINGLE = template(
            "object-template-xml-single.xml", "96a68747-f51a-41fc-96e6-5c27ad6f8502");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_SERVICE = template(
            "object-template-mql-service.xml", "98d22012-5bba-4f83-b7d6-5ad3e2cf2101");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_SERVICE = template(
            "object-template-xml-service.xml", "5e8bf29e-85df-4c61-9305-a05a61cd7201");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_ORG_WORKPLACE = template(
            "object-template-mql-org-workplace.xml", "768db2d3-2e73-4cfd-80e5-8fbbba4cf301");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_ORG_WORKPLACE = template(
            "object-template-xml-org-workplace.xml", "cd4b0e8d-7721-46b1-8b28-0d2905ee5b02");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_ORG_MANAGER = template(
            "object-template-mql-org-manager.xml", "90fcf182-c498-4fc3-9f94-5f94db35d603");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_ORG_MANAGER = template(
            "object-template-xml-org-manager.xml", "5d6bdc03-aa68-4d9d-aac2-602a2c9b2e04");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_MQL_ROLE = template(
            "object-template-mql-role.xml", "95d4c0c3-52dd-4f2c-b749-84b8ca10db05");
    protected static final TestObject<ObjectTemplateType> TEMPLATE_XML_ROLE = template(
            "object-template-xml-role.xml", "4d983b59-fd2e-4f00-a08e-f1e0d5d77f06");

    protected static final Map<TemplateType, Map<ScenarioSet, TestObject<ObjectTemplateType>>> SINGLE_TEMPLATES = Map.of(
            TemplateType.MQL, Map.of(
                    ScenarioSet.ALL, TEMPLATE_MQL_SINGLE,
                    ScenarioSet.SERVICE_ONLY, TEMPLATE_MQL_SERVICE,
                    ScenarioSet.ORG_BY_WORKPLACE_ONLY, TEMPLATE_MQL_ORG_WORKPLACE,
                    ScenarioSet.ORG_BY_MANAGER_ONLY, TEMPLATE_MQL_ORG_MANAGER,
                    ScenarioSet.ROLE_ONLY, TEMPLATE_MQL_ROLE),
            TemplateType.XML, Map.of(
                    ScenarioSet.ALL, TEMPLATE_XML_SINGLE,
                    ScenarioSet.SERVICE_ONLY, TEMPLATE_XML_SERVICE,
                    ScenarioSet.ORG_BY_WORKPLACE_ONLY, TEMPLATE_XML_ORG_WORKPLACE,
                    ScenarioSet.ORG_BY_MANAGER_ONLY, TEMPLATE_XML_ORG_MANAGER,
                    ScenarioSet.ROLE_ONLY, TEMPLATE_XML_ROLE));

    protected static final Map<TemplateType, TestObject<ObjectTemplateType>> SHADOW_TEMPLATES = Map.of(
            TemplateType.MQL, TEMPLATE_MQL_SHADOW,
            TemplateType.XML, TEMPLATE_XML_SHADOW);

    protected static final String RAW_MQL_A = """
            extension/initialOwnerName exists and
            extension/initialOwnerName = `name`
            """;
    protected static final String RAW_MQL_B1 = "identifier = $workplaceId";
    protected static final String RAW_MQL_B2 = "extension/managerPersonalNumber = $personalNumber";
    protected static final String RAW_MQL_C = """
            extension/exRoleSapCode1 = `dynamicCode1` and
            extension/exRoleSapCode = 'yyy' and
            extension/exRoleApplicationInstanceName = 'FSD' and
            extension/exRoleVariant = 'PRIMARY'
            """;
    protected static final String RAW_MQL_A_SHADOW = """
            extension/initialOwnerName exists and
            extension/initialOwnerName = `serviceOwnerShadow`
            """;
    protected static final String RAW_MQL_B1_SHADOW = "identifier = $workplaceShadow";
    protected static final String RAW_MQL_B2_SHADOW = "extension/managerPersonalNumber = $managerNumberShadow";
    protected static final String RAW_MQL_C_SHADOW = """
            extension/exRoleSapCode1 = `dynamicCodeShadow` and
            extension/exRoleSapCode = 'yyy' and
            extension/exRoleApplicationInstanceName = 'FSD' and
            extension/exRoleVariant = 'PRIMARY'
            """;

    protected static StressConfig shortConfig(
            String name,
            CohortSelection selection,
            MappingMode mappingMode,
            ScenarioSet scenarioSet,
            ConcurrencyMode concurrencyMode) {
        return new StressConfig(
                name,
                readInt(PROP_SHORT_USERS, DEFAULT_SHORT_USERS),
                readInt(PROP_SHORT_THREADS, DEFAULT_SHORT_THREADS),
                readInt(PROP_SHORT_ROUNDS, DEFAULT_SHORT_ROUNDS),
                0,
                selection,
                mappingMode,
                scenarioSet,
                concurrencyMode,
                readInt(PROP_SHORT_HOT_SET, DEFAULT_SHORT_HOT_SET),
                readInt(PROP_SHORT_HOT_REPEATS, DEFAULT_SHORT_HOT_REPEATS),
                Boolean.getBoolean(PROP_SERIALIZE_PER_USER));
    }

    protected static StressConfig shortScenarioConfig(String name, TemplateType templateType, ScenarioSet scenarioSet) {
        return new StressConfig(
                name,
                readInt(PROP_SHORT_USERS, DEFAULT_SHORT_USERS),
                readInt(PROP_SHORT_THREADS, DEFAULT_SHORT_THREADS),
                readInt(PROP_SHORT_ROUNDS, DEFAULT_SHORT_ROUNDS),
                0,
                templateType == TemplateType.MQL ? CohortSelection.MQL_ONLY : CohortSelection.XML_ONLY,
                MappingMode.SINGLE,
                scenarioSet,
                ConcurrencyMode.DISTINCT_USERS,
                readInt(PROP_SHORT_HOT_SET, DEFAULT_SHORT_HOT_SET),
                readInt(PROP_SHORT_HOT_REPEATS, DEFAULT_SHORT_HOT_REPEATS),
                Boolean.getBoolean(PROP_SERIALIZE_PER_USER));
    }

    protected static StressConfig manualConfig(
            String name,
            CohortSelection selection,
            MappingMode mappingMode,
            ScenarioSet scenarioSet,
            ConcurrencyMode concurrencyMode) {
        return new StressConfig(
                name,
                readInt(PROP_MANUAL_USERS, DEFAULT_MANUAL_USERS),
                readInt(PROP_MANUAL_THREADS, DEFAULT_MANUAL_THREADS),
                0,
                readInt(PROP_MANUAL_DURATION_SECONDS, DEFAULT_MANUAL_DURATION_SECONDS),
                selection,
                mappingMode,
                scenarioSet,
                concurrencyMode,
                readInt(PROP_MANUAL_HOT_SET, DEFAULT_MANUAL_HOT_SET),
                readInt(PROP_MANUAL_HOT_REPEATS, DEFAULT_MANUAL_HOT_REPEATS),
                Boolean.getBoolean(PROP_SERIALIZE_PER_USER));
    }

    protected static TestObject<ObjectTemplateType> templateFor(
            TemplateType templateType, MappingMode mappingMode, ScenarioSet scenarioSet) {
        if (mappingMode == MappingMode.SHADOW) {
            return SHADOW_TEMPLATES.get(templateType);
        }
        return SINGLE_TEMPLATES.get(templateType).get(scenarioSet);
    }

    protected record StressConfig(
            String name,
            int users,
            int threads,
            int rounds,
            int durationSeconds,
            CohortSelection cohortSelection,
            MappingMode mappingMode,
            ScenarioSet scenarioSet,
            ConcurrencyMode concurrencyMode,
            int hotSetSize,
            int hotRepeats,
            boolean serializePerUser) {
        @Override
        public String toString() {
            return "name=" + name
                    + ", users=" + users
                    + ", threads=" + threads
                    + ", rounds=" + rounds
                    + ", durationSeconds=" + durationSeconds
                    + ", cohortSelection=" + cohortSelection
                    + ", mappingMode=" + mappingMode
                    + ", scenarioSet=" + scenarioSet
                    + ", concurrencyMode=" + concurrencyMode
                    + ", hotSetSize=" + hotSetSize
                    + ", hotRepeats=" + hotRepeats
                    + ", serializePerUser=" + serializePerUser;
        }
    }

    private static TestObject<ObjectTemplateType> template(String filename, String oid) {
        return TestObject.file(TEST_DIR, filename, oid);
    }

    protected static int readInt(String propertyName, int defaultValue) {
        return Integer.getInteger(propertyName, defaultValue);
    }
}

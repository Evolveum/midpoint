/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMqlConcurrencyRace extends ConcurrencyRaceHelpers {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAdd(TEMPLATE_MQL_SHADOW, initResult);
        repoAdd(TEMPLATE_XML_SHADOW, initResult);
        repoAdd(TEMPLATE_MQL_SINGLE, initResult);
        repoAdd(TEMPLATE_XML_SINGLE, initResult);
        repoAdd(TEMPLATE_MQL_SERVICE, initResult);
        repoAdd(TEMPLATE_XML_SERVICE, initResult);
        repoAdd(TEMPLATE_MQL_ORG_WORKPLACE, initResult);
        repoAdd(TEMPLATE_XML_ORG_WORKPLACE, initResult);
        repoAdd(TEMPLATE_MQL_ORG_MANAGER, initResult);
        repoAdd(TEMPLATE_XML_ORG_MANAGER, initResult);
        repoAdd(TEMPLATE_MQL_ROLE, initResult);
        repoAdd(TEMPLATE_XML_ROLE, initResult);

        for (CohortKey cohortKey : activeCohorts()) {
            String subtype = subtypeFor(cohortKey);
            subtypeByCohort.put(cohortKey, subtype);
            setDefaultObjectTemplate(
                    UserType.COMPLEX_TYPE,
                    subtype,
                    templateFor(cohortKey.templateType(), cohortKey.mappingMode(), cohortKey.scenarioSet()).oid,
                    initResult);
        }

        int usersToSeed = determineUsersToSeed();
        repoAddObjects(createTargets(usersToSeed), initResult);
        repoAddObjects(createUsers(usersToSeed), initResult);
    }

    //Short stress test on distinct MQL users with single mappings across all assignment scenarios to see whether concurrency corrupts otherwise independent recomputes.
    @Test
    public void test100DistinctUsersMqlShortRace() throws Exception {
        runStress(shortConfig("distinct-users-mql", CohortSelection.MQL_ONLY, MappingMode.SINGLE, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Same short distinct-user stress test as a control, but using XML instead of MQL.
    @Test
    public void test110DistinctUsersXmlShortControl() throws Exception {
        runStress(shortConfig("distinct-users-xml", CohortSelection.XML_ONLY, MappingMode.SINGLE, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Mixed short stress test with both MQL and XML user cohorts together to check whether both modes behave consistently under concurrent distinct-user load.
    @Test
    public void test120DistinctUsersMqlVsXmlMixed() throws Exception {
        runStress(shortConfig("distinct-users-mixed", CohortSelection.MIXED, MappingMode.SINGLE, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Short stress test that repeatedly recomputes the same MQL users concurrently with shadow mappings to provoke a same-user race.
    @Test
    public void test200SameUserConcurrentMqlRace() throws Exception {
        runStress(shortConfig("same-focus-mql", CohortSelection.MQL_ONLY, MappingMode.SHADOW, ScenarioSet.ALL,
                ConcurrencyMode.SAME_USER_CONCURRENT));
    }

    //Same same-user concurrent shadow-mapping stress test as a control, but using XML instead of MQL.
    @Test
    public void test210SameUserConcurrentXmlControl() throws Exception {
        runStress(shortConfig("same-focus-xml", CohortSelection.XML_ONLY, MappingMode.SHADOW, ScenarioSet.ALL,
                ConcurrencyMode.SAME_USER_CONCURRENT));
    }

    //Short stress test on distinct MQL users using single mappings across all scenarios to verify that the MQL single-mapping variant is stable under parallel recomputes.
    @Test
    public void test300DistinctUsersMqlSingleMappings() throws Exception {
        runStress(shortConfig("distinct-users-mql-single", CohortSelection.MQL_ONLY, MappingMode.SINGLE, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Short stress test on distinct MQL users using shadow mappings across all scenarios to verify whether MQL shadow-based recomputes remain stable under concurrency.
    @Test
    public void test310DistinctUsersMqlShadowMappings() throws Exception {
        runStress(shortConfig("distinct-users-mql-shadow", CohortSelection.MQL_ONLY, MappingMode.SHADOW, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //XML control version of the distinct-user shadow-mapping stress test to compare shadow-mapping behavior without MQL.
    @Test
    public void test320DistinctUsersXmlShadowMappings() throws Exception {
        runStress(shortConfig("distinct-users-xml-shadow", CohortSelection.XML_ONLY, MappingMode.SHADOW, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Short distinct-user stress test for the MQL service-only scenario to verify that only the expected service assignment is produced under concurrency.
    @Test
    public void test400DistinctUsersServiceOnlyMql() throws Exception {
        runStress(shortScenarioConfig("distinct-service-mql", TemplateType.MQL, ScenarioSet.SERVICE_ONLY));
    }

    //XML control version of the distinct-user service-only scenario test.
    @Test
    public void test410DistinctUsersServiceOnlyXml() throws Exception {
        runStress(shortScenarioConfig("distinct-service-xml", TemplateType.XML, ScenarioSet.SERVICE_ONLY));
    }

    //Short distinct-user stress test for the MQL workplace-org-only scenario to verify that only the workplace-based org assignment is produced under concurrency.
    @Test
    public void test420DistinctUsersOrgByWorkplaceOnlyMql() throws Exception {
        runStress(shortScenarioConfig("distinct-org-workplace-mql", TemplateType.MQL, ScenarioSet.ORG_BY_WORKPLACE_ONLY));
    }

    //XML control version of the distinct-user workplace-org-only scenario test.
    @Test
    public void test430DistinctUsersOrgByWorkplaceOnlyXml() throws Exception {
        runStress(shortScenarioConfig("distinct-org-workplace-xml", TemplateType.XML, ScenarioSet.ORG_BY_WORKPLACE_ONLY));
    }

    //Short distinct-user stress test for the MQL manager-org-only scenario to verify that only the manager-based org assignment is produced under concurrency.
    @Test
    public void test440DistinctUsersOrgByManagerOnlyMql() throws Exception {
        runStress(shortScenarioConfig("distinct-org-manager-mql", TemplateType.MQL, ScenarioSet.ORG_BY_MANAGER_ONLY));
    }

    //XML control version of the distinct-user manager-org-only scenario test.
    @Test
    public void test450DistinctUsersOrgByManagerOnlyXml() throws Exception {
        runStress(shortScenarioConfig("distinct-org-manager-xml", TemplateType.XML, ScenarioSet.ORG_BY_MANAGER_ONLY));
    }

    //Short distinct-user stress test for the MQL role-only scenario to verify that only the expected role assignment is produced under concurrency.
    @Test
    public void test460DistinctUsersRoleOnlyMql() throws Exception {
        runStress(shortScenarioConfig("distinct-role-mql", TemplateType.MQL, ScenarioSet.ROLE_ONLY));
    }

    //XML control version of the distinct-user role-only scenario test.
    @Test
    public void test470DistinctUsersRoleOnlyXml() throws Exception {
        runStress(shortScenarioConfig("distinct-role-xml", TemplateType.XML, ScenarioSet.ROLE_ONLY));
    }

    //Longer manually enabled stress test over mixed MQL and XML distinct-user cohorts to search for rarer concurrency failures under heavier load.
    @Test
    public void test900ManualDistinctUsers() throws Exception {
        if (!Boolean.getBoolean(PROP_MANUAL_ENABLED)) {
            throw new SkipException("Manual stress disabled. Set -D" + PROP_MANUAL_ENABLED + "=true to run the long version.");
        }
        runStress(manualConfig("manual-distinct-users", CohortSelection.MIXED, MappingMode.SINGLE, ScenarioSet.ALL,
                ConcurrencyMode.DISTINCT_USERS));
    }

    //Longer manually enabled stress test over mixed MQL and XML same-user concurrent recomputes with shadow mappings to search for rarer same-focus race failures under heavier load.
    @Test
    public void test910ManualSameUserConcurrent() throws Exception {
        if (!Boolean.getBoolean(PROP_MANUAL_ENABLED)) {
            throw new SkipException("Manual stress disabled. Set -D" + PROP_MANUAL_ENABLED + "=true to run the long version.");
        }
        runStress(manualConfig("manual-same-focus", CohortSelection.MIXED, MappingMode.SHADOW, ScenarioSet.ALL,
                ConcurrencyMode.SAME_USER_CONCURRENT));
    }
}

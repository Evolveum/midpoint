/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.impl.ReportWebService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;

/**
 * Basic report tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportWebService extends AbstractReportIntegrationTest {

    @Autowired protected ReportWebService reportWebService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_READER_FILE, true, initResult);
        repoAddObjectFromFile(USER_READER_FILE, true, initResult);
        repoAddObjectFromFile(ROLE_RUNNER_FILE, true, initResult);
        repoAddObjectFromFile(USER_RUNNER_FILE, true, initResult);
        repoAddObjectFromFile(USER_READER_RUNNER_FILE, true, initResult);

        repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_CSV_FILE, ReportType.class, initResult);
        repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_FILE, ReportType.class, initResult);
        repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_FILE, ReportType.class, initResult);

        // Let's make this more interesting by adding a couple of users
        importObjectsFromFileNotRaw(USERS_MONKEY_ISLAND_FILE, initTask, initResult);
    }

    @Test
    public void test000Sanity() {
        assertNotNull("No web service", reportWebService);
    }

    @Test
    public void test100ProcessReportUserList() throws Exception {
        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        // WHEN
        when();
        ObjectListType userList = reportWebService.processReport(REPORT_USER_LIST_EXPRESSIONS_CSV_OID, query, parameters, null);

        // THEN
        then();
        displayValue("Returned user list (" + userList.getObject().size() + " objects)", userList);

        assertUserList(userList);
    }

    @Test
    public void test110ProcessReportUserListNoReportOid() {
        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        try {

            // WHEN
            when();
            reportWebService.processReport(null, query, parameters, null);

            assertNotReached();

        } catch (Fault f) {
            // THEN
            then();
            display("Expected fault", f);
        }
    }

    @Test
    public void test112ProcessReportUserListInvalidReportOid() {
        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        try {

            // WHEN
            when();
            reportWebService.processReport("l00n3y", query, parameters, null);

            assertNotReached();

        } catch (Fault f) {
            // THEN
            then();
            display("Expected fault", f);
        }
    }

    /**
     * MID-5463
     */
    @Test
    public void test115ProcessReportUserListUnauthorizedReader() throws Exception {
        login(USER_READER_USERNAME);

        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        try {

            // WHEN
            when();
            reportWebService.processReport(REPORT_USER_LIST_EXPRESSIONS_CSV_OID, query, parameters, null);

            assertNotReached();

        } catch (Fault f) {
            // THEN
            then();
            display("Expected fault", f);
        } finally {
            login(USER_ADMINISTRATOR_USERNAME);
        }
    }

    /**
     * MID-5463
     */
    @Test
    public void test116ProcessReportUserListUnauthorizedRunner() throws Exception {
        login(USER_RUNNER_USERNAME);

        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        try {

            // WHEN
            when();
            reportWebService.processReport(REPORT_USER_LIST_EXPRESSIONS_CSV_OID, query, parameters, null);

            assertNotReached();

        } catch (Fault f) {
            // THEN
            then();
            display("Expected fault", f);
        } finally {
            login(USER_ADMINISTRATOR_USERNAME);
        }
    }

    /**
     * MID-5463
     */
    @Test
    public void test119ProcessReportUserListReaderRunner() throws Exception {
        login(USER_READER_RUNNER_USERNAME);

        String query = createAllQueryString(UserType.class);
        RemoteReportParametersType parameters = createReportParameters();

        ObjectListType userList;
        try {

            // WHEN
            when();
            userList = reportWebService.processReport(REPORT_USER_LIST_EXPRESSIONS_CSV_OID, query, parameters, null);

        } finally {
            login(USER_ADMINISTRATOR_USERNAME);
        }

        // THEN
        then();
        displayValue("Returned user list (" + userList.getObject().size() + " objects)", userList);

        assertUserList(userList);
    }

    // TODO: test that violates safe profile

    private String createAllQueryString(Class<?> type) {
        return "<filter><type><type>" + type.getSimpleName() + "</type></type></filter>";
    }

    private RemoteReportParametersType createReportParameters() {
        return new RemoteReportParametersType();
    }

    private void assertUserList(ObjectListType userList) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<UserType>> currentUsers = modelService.searchObjects(UserType.class, null, null, task, result);
        displayValue("Current users in midPoint (" + currentUsers.size() + " users)", currentUsers.toString());

        assertEquals("Unexpected number of returned objects", currentUsers.size(), userList.getObject().size());
    }

}

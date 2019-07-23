/**
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.report;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Basic report test, using "safe" expression profile.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportSafe extends TestReport {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_SAFE_FILE;
	}
	
	/**
     * Reports with poisonous operations in the query. This should work with null profile.
     * But it should fail with safe profile.
     * Field operations are safe in this report, just the query is poisonous.
     */
	@Test
	@Override
	public void test112ReportUserListExpressionsPoisonousQueryCsv() throws Exception {
		final String TEST_NAME = "test110ReportUserListExpressionsCsv";
		testReportListUsersCsvFailure(TEST_NAME, REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_OID);
	}
	
	/**
	 * Reports with poisonous operations in the field expression. This should work with null profile.
	 * But it should fail with safe profile.
	 * Query expression is safe in this report, just fields are poisonous.
	 */
	@Test
	public void test114ReportUserListExpressionsPoisonousFieldCsv() throws Exception {
		final String TEST_NAME = "test114ReportUserListExpressionsPoisonousFieldCsv";
		testReportListUsersCsvFailure(TEST_NAME, REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_OID);
	}

	/**
	 * Reports with report.searchAuditRecords() operation in the query expression. This should work with null profile.
	 * But it should fail with safe profile.
	 */
	@Test
	public void test300ReportAuditLegacy() throws Exception {
		final String TEST_NAME = "test300ReportAuditLegacy";
		testReportAuditCsvFailure(TEST_NAME, REPORT_AUDIT_CSV_LEGACY_OID);
	}

}

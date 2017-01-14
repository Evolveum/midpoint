/*
 * Copyright (c) 2010-2017 Evolveum
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

/**
 * Set of tests for policy-based approvals. These are structure in the following way:
 *
 * assignments: Tests approvals for simple assigning of (sensitive) roles.
 *   plain: Uses default policy rules that are used if no other rules are present. The default rules look
 *        for org:approver assignees and/or approverRef objects).
 *     TestAssignmentApprovalPlainImplicit: tests assignments, uses org:approver relation
 *     TestAssignmentApprovalPlainExplicit: tests assignments, uses approverRef relation
 *   global: Uses global policy rules (but quite similar to default ones)
 *     TestAssignmentApprovalGlobal: ## not used, as the functionality is not implemented yet ##
 *   metarole: Uses metarole-induced policy rules.
 *     TestAssignmentApprovalMetaroleExplicit: uses metaroles that are explicitly assigned to given roles
 *     TestAssignmentsWithDifferentMetaroles: DOES NOT EXTEND AbstractTestAssignmentApproval (as other approval tests do),
 *       but uses specific metaroles to provide different approval requirements. See Role21..23.
 *
 * lifecycle: Tests approvals for add/modify/delete of roles
 *   plain: Uses default rules that look for org:owner assignees.
 *     TestLifecyclePlain: tests role modify/delete (uses org:owner relation)
 *   global: Uses global policy rules (but quite similar to default ones)
 *     TestLifecycleGlobal: for role modify/delete, uses org:owner relation; for role add, uses specified user
 *
 * sod: Tests approvals for SoD violations
 *
 * @author mederly
 */
package com.evolveum.midpoint.wf.impl.policy;
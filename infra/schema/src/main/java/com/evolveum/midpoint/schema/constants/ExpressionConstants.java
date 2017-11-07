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
package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ExpressionConstants {

	// Generic variables
	public static final QName VAR_INPUT = new QName(SchemaConstants.NS_C, "input");
	public static final QName VAR_OBJECT = new QName(SchemaConstants.NS_C, "object");

	// Variables used in various mappings
	public static final QName VAR_FOCUS = new QName(SchemaConstants.NS_C, "focus");
	public static final QName VAR_PROJECTION = new QName(SchemaConstants.NS_C, "projection");
	public static final QName VAR_SOURCE = new QName(SchemaConstants.NS_C, "source");
	public static final QName VAR_ASSIGNMENT = new QName(SchemaConstants.NS_C, "assignment");
	public static final QName VAR_EVALUATED_ASSIGNMENT = new QName(SchemaConstants.NS_C, "evaluatedAssignment");
	public static final QName VAR_ASSIGNMENT_PATH = new QName(SchemaConstants.NS_C, "assignmentPath");
	public static final QName VAR_IMMEDIATE_ASSIGNMENT = new QName(SchemaConstants.NS_C, "immediateAssignment");
	public static final QName VAR_THIS_ASSIGNMENT = new QName(SchemaConstants.NS_C, "thisAssignment");
	public static final QName VAR_FOCUS_ASSIGNMENT = new QName(SchemaConstants.NS_C, "focusAssignment");
	public static final QName VAR_IMMEDIATE_ROLE = new QName(SchemaConstants.NS_C, "immediateRole");
	public static final QName VAR_CONTAINING_OBJECT = new QName(SchemaConstants.NS_C, "containingObject");
	public static final QName VAR_ORDER_ONE_OBJECT = new QName(SchemaConstants.NS_C, "thisObject");
	public static final QName VAR_OPERATION = new QName(SchemaConstants.NS_C, "operation");
	public static final QName VAR_RESOURCE = new QName(SchemaConstants.NS_C, "resource");
	public static final QName VAR_MODEL_CONTEXT = new QName(SchemaConstants.NS_C, "modelContext");
	public static final QName VAR_PRISM_CONTEXT = new QName(SchemaConstants.NS_C, "prismContext");
	public static final QName VAR_CONFIGURATION = new QName(SchemaConstants.NS_C, "configuration");
	public static final QName VAR_ENTITLEMENT = new QName(SchemaConstants.NS_C, "entitlement");
	
	/**
	 * User that is currently executing the operation.
	 */
    public static final QName VAR_ACTOR = new QName(SchemaConstants.NS_C, "actor");
    
    /**
     * Subject of an authorization. This is usually the same as actor. But it may be different
     * in some exotic use cases (e.g. if administrator needs to evaluate authorizations of
     * a different user.
     */
    public static final QName VAR_SUBJECT = new QName(SchemaConstants.NS_C, "subject");
    
	public static final QName VAR_VALUE = new QName(SchemaConstants.NS_C, "value");
	public static final QName VAR_ORGS = new QName(SchemaConstants.NS_C, "orgs");

	public static final QName VAR_TARGET = new QName(SchemaConstants.NS_C, "target");

	// DEPRECATED variables, just for compatibility
	public static final QName VAR_USER = new QName(SchemaConstants.NS_C, "user");
	public static final QName VAR_ACCOUNT = new QName(SchemaConstants.NS_C, "account");
	public static final QName VAR_SHADOW = new QName(SchemaConstants.NS_C, "shadow");

	// existence mapping variables
	public static final QName VAR_LEGAL = new QName(SchemaConstants.NS_C, "legal");
    public static final QName VAR_ASSIGNED = new QName(SchemaConstants.NS_C, "assigned");
	public static final QName VAR_FOCUS_EXISTS = new QName(SchemaConstants.NS_C, "focusExists");
	public static final QName VAR_ADMINISTRATIVE_STATUS = new QName(SchemaConstants.NS_C, "administrativeStatus");

	public static final QName VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION = new QName(SchemaConstants.NS_C, "associationTargetObjectClassDefinition");

	/**
	 * Numeric value describing the current iteration. It starts with 0 and increments on every iteration.
	 * Iterations are used to find unique values for an account, to resolve naming conflicts, etc.
	 */
	public static final QName VAR_ITERATION = new QName(SchemaConstants.NS_C, "iteration");

	/**
	 * String value describing the current iteration. It is usually suffix that is appended to the username
	 * or a similar "extension" of the value. It should have different value for every iteration. The actual
	 * value is determined by the iteration settings.
	 */
	public static final QName VAR_ITERATION_TOKEN = new QName(SchemaConstants.NS_C, "iterationToken");

	// Variables used in object merging expressions
	public static final QName VAR_SIDE = new QName(SchemaConstants.NS_C, "side");
	public static final QName VAR_OBJECT_LEFT = new QName(SchemaConstants.NS_C, "objectLeft");
	public static final QName VAR_OBJECT_RIGHT = new QName(SchemaConstants.NS_C, "objectRight");

	public static final QName OUTPUT_ELEMENT_NAME = new QName(SchemaConstants.NS_C, "output");

	// "case" would collide with java keyword
	public static final QName VAR_WORK_ITEM = new QName(SchemaConstants.NS_C, "workItem");
	public static final QName VAR_CERTIFICATION_CASE = new QName(SchemaConstants.NS_C, "certificationCase");
	public static final QName VAR_CAMPAIGN = new QName(SchemaConstants.NS_C, "campaign");
	public static final QName VAR_REVIEWER_SPECIFICATION = new QName(SchemaConstants.NS_C, "reviewerSpecification");

	public static final QName VAR_CHANNEL = new QName(SchemaConstants.NS_C, "channel");
	public static final QName VAR_WORKFLOW_CONTEXT = new QName(SchemaConstants.NS_C, "workflowContext");
	public static final QName VAR_TASK = new QName(SchemaConstants.NS_C, "task");
	public static final QName VAR_RULE_EVALUATION_CONTEXT = new QName(SchemaConstants.NS_C, "ruleEvaluationContext");
	public static final QName VAR_STAGE_DEFINITION = new QName(SchemaConstants.NS_C, "stageDefinition");
}

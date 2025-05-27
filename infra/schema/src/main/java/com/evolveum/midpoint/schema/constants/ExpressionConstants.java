/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.prism.path.ItemName;

import javax.xml.namespace.QName;

/**
 * Constants for all names of the variables in the system.
 *
 * It is good to have all the names gathered in one place. It is better when a new
 * variable is introduced. If all the names are in the same place, it is better to
 * see if the variable is redundant (already used eslewhere). Or if new variable is
 * really needed, seeing all the other names will make it easier to keep the same
 * convention through the system.
 *
 * @author Radovan Semancik
 */
public class ExpressionConstants {

    // Generic variables
    public static final String VAR_INPUT = "input";
    public static final ItemName VAR_INPUT_QNAME = new ItemName(SchemaConstants.NS_C, VAR_INPUT);
    public static final String VAR_OBJECT =  "object";

    // Variables used in various mappings
    public static final String VAR_FOCUS = "focus";
    public static final String VAR_PROJECTION = "projection";
    public static final String VAR_SOURCE = "source";
    public static final String VAR_ASSIGNMENT = "assignment";
    public static final QName VAR_ASSIGNMENT_QNAME = new QName(SchemaConstants.NS_C, VAR_ASSIGNMENT);
    public static final String VAR_ASSIGNMENT_EVALUATOR = "assignmentEvaluator";
    public static final String VAR_EVALUATED_ASSIGNMENT = "evaluatedAssignment";
    public static final String VAR_ASSIGNMENT_PATH = "assignmentPath";
    public static final String VAR_IMMEDIATE_ASSIGNMENT = "immediateAssignment";
    public static final String VAR_THIS_ASSIGNMENT = "thisAssignment";
    public static final String VAR_FOCUS_ASSIGNMENT = "focusAssignment";
    public static final String VAR_IMMEDIATE_ROLE = "immediateRole";
    public static final String VAR_CONTAINING_OBJECT = "containingObject";
    public static final String VAR_THIS_OBJECT = "thisObject";
    public static final String VAR_OPERATION = "operation"; // beware: used for more unrelated things
    public static final String VAR_OPERATION_RESULT = "operationResult";
    public static final String VAR_RESOURCE = "resource";
    public static final String VAR_DELTA = "delta";
    public static final String VAR_MODEL_CONTEXT = "modelContext";
    public static final String VAR_MODEL_ELEMENT_CONTEXT = "modelElementContext";
    public static final String VAR_PRISM_CONTEXT = "prismContext";
    public static final String VAR_LOCALIZATION_SERVICE = "localizationService";
    public static final String VAR_LOCALE = "locale";
    public static final String VAR_CONFIGURATION = "configuration";
    public static final String VAR_ENTITLEMENT = "entitlement";
    public static final String VAR_FILE = "file";
    public static final String VAR_PARENT_OBJECT = "parent";
    public static final String VAR_CANDIDATE = "candidate";

    /**
     * User that is currently executing the operation.
     */
    public static final String VAR_ACTOR = "actor";

    /**
     * Subject of an authorization. This is usually the same as actor. But it may be different
     * in some exotic use cases (e.g. if administrator needs to evaluate authorizations of
     * a different user.
     */
    public static final String VAR_SUBJECT = "subject";

    public static final String VAR_VALUE = "value";
    public static final String VAR_ORGS = "orgs";

    public static final String VAR_TARGET = "target";

    // DEPRECATED variables, just for compatibility
    public static final String VAR_USER = "user";
    public static final String VAR_ACCOUNT = "account";
    public static final String VAR_SHADOW = "shadow";

    // existence mapping variables
    public static final String VAR_LEGAL = "legal";
    public static final ItemName VAR_LEGAL_QNAME = new ItemName(SchemaConstants.NS_C, VAR_LEGAL);

    public static final String VAR_ASSIGNED = "assigned";
    public static final ItemName VAR_ASSIGNED_QNAME = new ItemName(SchemaConstants.NS_C, VAR_ASSIGNED);

    public static final String VAR_FOCUS_EXISTS = "focusExists";
    public static final ItemName VAR_FOCUS_EXISTS_QNAME = new ItemName(SchemaConstants.NS_C, VAR_FOCUS_EXISTS);

    public static final String VAR_ADMINISTRATIVE_STATUS = "administrativeStatus";
    public static final ItemName VAR_ADMINISTRATIVE_STATUS_QNAME = new ItemName(SchemaConstants.NS_C, VAR_ADMINISTRATIVE_STATUS);

    public static final String VAR_ASSOCIATION_DEFINITION = "associationDefinition";

    /**
     * Numeric value describing the current iteration. It starts with 0 and increments on every iteration.
     * Iterations are used to find unique values for an account, to resolve naming conflicts, etc.
     */
    public static final String VAR_ITERATION = "iteration";
    public static final ItemName VAR_ITERATION_QNAME = new ItemName(SchemaConstants.NS_C, VAR_ITERATION);

    /**
     * String value describing the current iteration. It is usually suffix that is appended to the username
     * or a similar "extension" of the value. It should have different value for every iteration. The actual
     * value is determined by the iteration settings.
     */
    public static final String VAR_ITERATION_TOKEN = "iterationToken";
    public static final ItemName VAR_ITERATION_TOKEN_QNAME = new ItemName(SchemaConstants.NS_C, VAR_ITERATION_TOKEN);

    public static final String VAR_REFERENCE_TIME = "referenceTime";

    // Variables used in object merging expressions
    public static final String VAR_SIDE = "side";
    public static final String VAR_OBJECT_LEFT = "objectLeft";
    public static final String VAR_OBJECT_RIGHT = "objectRight";

    public static final ItemName OUTPUT_ELEMENT_NAME = new ItemName(SchemaConstants.NS_C, "output");

    // "case" would collide with java keyword
    public static final String VAR_WORK_ITEM = "workItem";
    public static final String VAR_CERTIFICATION_CASE = "certificationCase";
    public static final String VAR_CAMPAIGN = "campaign";
    public static final String VAR_REVIEWER_SPECIFICATION = "reviewerSpecification";

    public static final String VAR_CHANNEL = "channel";
    public static final String VAR_APPROVAL_CONTEXT = "approvalContext";
    public static final String VAR_TASK = "task";
    public static final String VAR_RULE_EVALUATION_CONTEXT = "ruleEvaluationContext";
    public static final String VAR_STAGE_DEFINITION = "stageDefinition";
    public static final String VAR_THE_CASE = "theCase";
    public static final String VAR_POLICY_RULES = "policyRules";
    public static final String VAR_ITEM_TO_APPROVE = "itemToApprove";

    public static final String VAR_OBJECT_DISPLAY_INFORMATION = "objectDisplayInformation";
    public static final String VAR_TARGET_DISPLAY_INFORMATION = "targetDisplayInformation";

    public static final String VAR_PERFORMER = "performer";
    public static final String VAR_OUTPUT = "output";
    public static final String VAR_EVENT = "event";
    public static final String VAR_REQUESTER = "requester";
    public static final String VAR_REQUESTEE = "requestee";
    public static final String VAR_ASSIGNEE = "assignee";
    public static final String VAR_ASSOCIATION = "association";
    public static final String VAR_SHADOW_DISCRIMINATOR = "shadowDiscriminator";

    public static final String VAR_POLICY_RULE = "policyRule";
    public static final String VAR_POLICY_ACTION = "policyAction";
    public static final String VAR_LOGIN_MODE = "loginMode";

    // Notification variables
    public static final String VAR_TRANSPORT_NAME = "transportName";
    public static final String VAR_FROM = "from";
    public static final String VAR_ENCODED_FROM = "encodedFrom";
    public static final String VAR_TO = "to";
    public static final String VAR_TO_LIST = "toList";
    public static final String VAR_ENCODED_TO = "encodedTo";
    public static final String VAR_ENCODED_TO_LIST = "encodedToList";
    public static final String VAR_MESSAGE_TEXT = "messageText";
    public static final String VAR_ENCODED_MESSAGE_TEXT = "encodedMessageText";
    public static final String VAR_MESSAGE = "message";
    public static final String VAR_RECIPIENT = "recipient";
    public static final String VAR_TEXT_FORMATTER = "textFormatter";
    public static final String VAR_NOTIFICATION_FUNCTIONS = "notificationFunctions";

    public static final String VAR_RECORD = "record";
    public static final String VAR_AUDIT_RECORD = "auditRecord";

    // This one is used in approvals.
    public static final String VAR_OBJECT_DELTA = "objectDelta";

    public static final String VAR_RESOURCE_OBJECT_DELTA = "resourceObjectDelta";

    // For metadata computation
    public static final String VAR_METADATA_COMPUTATION_INPUT = "metadataComputationInput";
    public static final String VAR_METADATA = "metadata";

    public static final String VAR_REQUEST = "request";
    public static final String VAR_ITEM = "item";
    public static final String VAR_BUCKET = "bucket";

    public static final String VAR_SYNCHRONIZATION_CONTEXT = "synchronizationContext";
    public static final String VAR_CORRELATION_CONTEXT = "correlationContext";
    public static final String VAR_CORRELATOR_STATE = "correlatorState";
    public static final String VAR_PROCESSED_OBJECT = "processedObject";
}

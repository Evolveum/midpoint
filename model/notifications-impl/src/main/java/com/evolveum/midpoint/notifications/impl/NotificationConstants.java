/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

public class NotificationConstants {

    public static final QName ACCOUNT_CREATION_QNAME = new QName(SchemaConstants.NS_C, "accountCreation");
    public static final QName ACCOUNT_MODIFICATION_QNAME = new QName(SchemaConstants.NS_C, "accountModification");
    public static final QName ACCOUNT_DELETION_QNAME = new QName(SchemaConstants.NS_C, "accountDeletion");
    public static final QName WORK_ITEM_CREATION_QNAME = new QName(SchemaConstants.NS_C, "workItemCreation");

    public static final QName SUCCESS_QNAME = new QName(SchemaConstants.NS_C, "success");
    public static final QName IN_PROGRESS_QNAME = new QName(SchemaConstants.NS_C, "inProgress");
    public static final QName FAILURE_QNAME = new QName(SchemaConstants.NS_C, "failure");

//    public static final String ACCOUNT_DELTAS = "accountDeltas";
//    public static final String MODEL_CONTEXT = "modelContext";
//    public static final String ACCOUNT_DELTA = "accountDelta";
//    public static final String ACCOUNT_CHANGE = "accountChange";
//    public static final String ACCOUNT_OPERATION_DESCRIPTION = "accountOperationDescription";
}

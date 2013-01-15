/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
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

/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ExpressionConstants {

	public static final QName VAR_INPUT = new QName(SchemaConstants.NS_C, "input");
	public static final QName VAR_USER = new QName(SchemaConstants.NS_C, "user");
	public static final QName VAR_ACCOUNT = new QName(SchemaConstants.NS_C, "account");
	public static final QName VAR_ASSIGNMENT = new QName(SchemaConstants.NS_C, "assignment");
	public static final QName VAR_OPERATION = new QName(SchemaConstants.NS_C, "operation");
	public static final QName VAR_RESOURCE = new QName(SchemaConstants.NS_C, "resource");
	
	public static final QName VAR_LEGAL = new QName(SchemaConstants.NS_C, "legal");
	
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
	
	public static final QName OUTPUT_ELMENT_NAME = new QName(SchemaConstants.NS_C, "output");
	
}

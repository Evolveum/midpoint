/**
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
package com.evolveum.midpoint.model.sync;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.importer.ImportConstants;

/**
 * @author semancik
 *
 */
public class SynchronizationConstants {
	
	public static final String SYNC_URI_PREFIX = "http://midpoint.evolveum.com/model/synchronization";
	public static final String SYNC_EXTENSION_SCHEMA = SYNC_URI_PREFIX + "/extension-2";
	public static final QName FRESHENESS_INTERVAL_PROPERTY_NAME = new QName(SYNC_EXTENSION_SCHEMA, "freshnessInterval");

}

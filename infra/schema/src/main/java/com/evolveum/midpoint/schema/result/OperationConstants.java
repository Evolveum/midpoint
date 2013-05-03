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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.schema.result;

/**
 * @author Radovan Semancik
 *
 */
public class OperationConstants {

	public static final String PREFIX = "com.evolveum.midpoint.common.operation";
	
	public static final String LIVE_SYNC = PREFIX + ".liveSync";
    public static final String LIVE_SYNC_STATISTICS = PREFIX + ".liveSync.statistics";
	public static final String RECONCILIATION = PREFIX + ".reconciliation";
	public static final String RECONCILE_ACCOUNT = PREFIX + ".reconciliation.account";
	public static final String RECOMPUTE = PREFIX + ".recompute";
	public static final String RECOMPUTE_USER = PREFIX + ".recompute.user";
	public static final String RECOMPUTE_STATISTICS = PREFIX + ".recompute.statistics";
	
	
	public static final String IMPORT_ACCOUNTS_FROM_RESOURCE = PREFIX + ".import.accountsFromResource";
    public static final String IMPORT_ACCOUNTS_FROM_RESOURCE_STATISTICS = PREFIX + ".import.accountsFromResource.statistics";
	public static final String IMPORT_OBJECTS_FROM_FILE = PREFIX + ".import.objectsFromFile";
    public static final String IMPORT_OBJECTS_FROM_CAMEL = PREFIX + ".import.objectsFromCamel";
	public static final String IMPORT_OBJECTS_FROM_STREAM = PREFIX + ".import.objectsFromStream";
	public static final String IMPORT_OBJECT = PREFIX + ".import.object";


}

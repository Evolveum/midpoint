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
package com.evolveum.midpoint.common;

import java.io.File;

/**
 * @author semancik
 *
 */
public class CommonTestConstants {
	
	public static final String KEYSTORE_PATH = "src/test/resources/crypto/test-keystore.jceks";
	public static final String KEYSTORE_PASSWORD = "changeit";
	public static File OBJECTS_DIR = new File("src/test/resources/objects");
	
	public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	public static final File USER_JACK_FILE = new File(OBJECTS_DIR, USER_JACK_OID + ".xml");

}

/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Class Doc
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class Version {

	public static final String code_id = "$Id$";
	public static String VERSION = "UNKNOWN";
	private static Logger log = LoggerFactory.getLogger(Version.class);

	static {
		try {
			Properties p = new Properties();
			InputStream st = Version.class
					.getResourceAsStream("/version.properties");
			p.load(st);
			st.close();
			VERSION = p.getProperty("project.version");
			log.info("midPoint Annotations {}", VERSION);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void touch() {
	}
}

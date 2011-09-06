/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import java.io.File;

import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ApplicationHomeSetup {

	private static final transient Trace logger = TraceManager.getTrace(ApplicationHomeSetup.class);
	private String MIDPOINT_HOME;

	public void init(String midpointHome) {

		MIDPOINT_HOME = midpointHome;
		
		if (System.getProperty(MIDPOINT_HOME) == null || System.getProperty(MIDPOINT_HOME).isEmpty()) {
			logger.warn("*****************************************************************************************");
			logger.warn(MIDPOINT_HOME + " is not set ! Using default configuration, for more information see http://wiki.evolveum.com/display/midPoint/");
			logger.warn("*****************************************************************************************");

			System.out.println("*******************************************************************************");
			System.out.println(MIDPOINT_HOME + " is not set ! Using default configuration, for more information");
			System.out.println("                 see http://wiki.evolveum.com/display/midPoint/");
			System.out.println("*******************************************************************************");
			return;
		}

		logger.info(MIDPOINT_HOME +" = " + System.getProperty(MIDPOINT_HOME));
		System.out.println(MIDPOINT_HOME + " = " + System.getProperty(MIDPOINT_HOME));

		String mh = System.getProperty(MIDPOINT_HOME);

		if (!checkDirectoryExistence(mh)) {
			createDir(mh);
		}

		directorySetup(mh);

	}

	/**
	 * Creates directory structure under root
	 * 
	 * @param dir
	 */

	protected void directorySetup(String dir) {

		/*
		 * Directory information based on:
		 * http://wiki.evolveum.com/display/midPoint
		 * /midpoint.home+-+directory+structure
		 */

		if (!checkDirectoryExistence(dir + "/icf-connectors")) {
			logger.warn("Missing directory " + dir + "/icf-connectors/. Regeneration in progress...");
			createDir(dir + "/icf-connectors");
			System.setProperty("midpoint.dir.icf", dir + "/icf-connectors");
		}

		if (!checkDirectoryExistence(dir + "/idm-legacy")) {
			logger.warn("Missing directory " + dir + "/idm-legacy/. Regeneration in progress...");
			createDir(dir + "/idm-legacy");
			System.setProperty("midpoint.dir.legacy", dir + "/idm-legacy");
		}

		if (!checkDirectoryExistence(dir + "/log")) {
			logger.warn("Missing directory " + dir + "/log/. Regeneration in progress...");
			createDir(dir + "/log");
		}
	}

	/**
	 * Checking directory existence
	 * 
	 * @param dir
	 * @return
	 */
	private boolean checkDirectoryExistence(String dir) {
		File d = new File(dir);
		if (d.isFile()) {
			logger.error(dir + " is file and NOT a directory.");
			throw new SystemException(dir + " is file and NOT a directory !!!");
		}

		if (d.isDirectory()) {
			logger.info("Directory " + dir + " already exists. Reusing it.");
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Creates directory
	 * 
	 * @param dir
	 */
	private void createDir(String dir) {
		File d = new File(dir);
		if ( d.exists() && d.isDirectory()) {
			return;
		}
		Boolean st = d.mkdirs();
		if (!st) {
			logger.error("Unable to create directory " + dir + " as user " + System.getProperty("user.name"));
			//throw new SystemException("Unable to create directory " + dir + " as user "
			//		+ System.getProperty("user.name"));
		}
	}

	
}

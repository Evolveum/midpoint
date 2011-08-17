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

package com.evolveum.midpoint.test.ldap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

/**
 * Sample Class Doc
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class OpenDJUnitTestAdapter extends AbstractTestNGSpringContextTests {

	protected static String ldapDataDir = "target/test-data/opendj";

	protected static String ldapTmpDir = "target/tmp-data";

	protected static String djTemplatePath = "test-data/opendj.template";

	protected static OpenDJController controller;

	public OpenDJUnitTestAdapter() {
	}

	/**
	 * 
	 * Note: this does not copy the files, just locates the template or extracts it
	 * from system resources.
	 * The OpenDJController will copy the files.
	 * 
	 * @return
	 */
	private static File extractTemplate(String customTemplatePath) {
		
		String templatePath = djTemplatePath;
		if (customTemplatePath!=null) {
			templatePath = customTemplatePath;
		}
		
		File templateFile = new File(templatePath);
		if (templateFile.isDirectory()) {
			return templateFile;
		}
		
		String templateResourcePath = ClassLoader.getSystemResource(templatePath).getPath();
		System.out.println("Template resource path: "+templateResourcePath);

		File templateResourceFile = new File(templateResourcePath);
		if (templateResourceFile.isDirectory()) {
			return templateResourceFile;
		}
		System.out.println("--- Extracting OpenDJ from a system resource, template path "+templatePath+" ----");
		templateResourcePath = templateResourcePath.replace("file:", "").split("!")[0];
		System.out.println("path after expansion: "+templateResourcePath);

		JarFile jf = null;
		try {
			jf = new JarFile(templateResourcePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		File dst = new File(ldapTmpDir);
		dst.mkdirs();

		for (Enumeration en = jf.entries(); en.hasMoreElements();) {
			JarEntry je = (JarEntry) en.nextElement();
			if (je.getName().contains(templatePath)) {
				String srcName = je.getName();
				String dstName = srcName.replace(templatePath, "");
				if (dstName.length() < 3)
					continue;
				System.out.println("++ " + srcName + " -->" + dstName + "  (" + je.getSize() + ")");
				String newName = ldapTmpDir + dstName;
				
				if ( je.getSize() == 0  && newName.endsWith("/") ) {
					new File(newName).mkdirs();
					continue;
				}
				if (new File(newName).exists())
					continue;
				InputStream is = null;
				try {
					
					OutputStream out = new FileOutputStream(newName);
					byte buf[] = new byte[65536];
					is = jf.getInputStream(je);
					int len;
					while ((len = is.read(buf)) > 0) {

						out.write(buf, 0, len);
					}
					out.close();
					is.close();

				} catch (FileNotFoundException e) {
					throw new IllegalStateException(e);

				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		System.out.println("--- Extracted ----");
		return dst;
	}

	public static void startACleanDJ(String templatePath) throws Exception {
		if (controller == null) {
			controller = new OpenDJController(new File(ldapDataDir),
					extractTemplate(templatePath));
		}
		controller.refreshFromTemplate();
		controller.start();
	}

	public static void startACleanDJ() throws Exception {
		startACleanDJ(null);
	}

	
	public static void stopDJ() throws Exception {
		controller.stop();
	}
}

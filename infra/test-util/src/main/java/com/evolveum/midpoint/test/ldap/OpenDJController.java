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
 * Portions Copyrighted 2011 [name of copyright owner] Portions Copyrighted 2010
 * Forgerock
 */

package com.evolveum.midpoint.test.ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.opends.messages.Message;
import org.opends.server.config.ConfigException;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.types.Attribute;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.types.InitializationException;
import org.opends.server.util.EmbeddedUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This class controls embedded OpenDJ instance.
 * 
 * It is used in Unit tests. It configures and starts and stops the instance. It
 * can even manage a "template" configuration of OpenDJ and copy it to working
 * instance configuration.
 * 
 * @author Radovan Semancik
 */
public class OpenDJController {

	private String DATA_TEMPLATE = "test-data/opendj.template";
	private String SERVER_ROOT = "target/test-data/opendj";

	protected File serverRoot = new File(SERVER_ROOT);
	protected File configFile = null;
	protected File templateRoot = new File(DATA_TEMPLATE);

	private static final Trace LOGGER = TraceManager.getTrace(OpenDJController.class);

	protected InternalClientConnection internalConnection;

	public OpenDJController() {
		init();
	}

	public OpenDJController(String serverRoot) {
		SERVER_ROOT = serverRoot;
		this.serverRoot = new File(serverRoot);
		init();
	}

	public OpenDJController(String serverRoot, String templateServerRoot) {
		SERVER_ROOT = serverRoot;
		DATA_TEMPLATE = templateServerRoot;
		this.serverRoot = new File(serverRoot);
		this.templateRoot = new File(templateServerRoot);
		init();
	}

	/**
	 * Initialize
	 * 
	 */

	private void init() {
		if (!serverRoot.exists()) {
			serverRoot.mkdirs();
		}

		if (configFile == null) {
			configFile = new File(serverRoot, "config/config.ldif");
		}
	}

	/**
	 * Get the value of serverRoot.
	 * 
	 * The top directory of working OpenDS installation. The OpenDS placed in
	 * this directory will be used during the tests.
	 * 
	 * @return the value of serverRoot
	 */
	public File getServerRoot() {
		return this.serverRoot;
	}

	/**
	 * Set the value of serverRoot
	 * 
	 * The top directory of working OpenDS installation. The OpenDS placed in
	 * this directory will be used during the tests.
	 * 
	 * @param serverRoot
	 *            new value of serverRoot
	 */
	public void setServerRoot(File serverRoot) {
		this.serverRoot = serverRoot;
	}

	/**
	 * Get the value of configFile
	 * 
	 * File name of primary OpenDS configuration file. Normally
	 * <serverRoot>/config/config.ldif
	 * 
	 * @return the value of configFile
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Set the value of configFile
	 * 
	 * File name of primary OpenDS configuration file. Normally
	 * <serverRoot>/config/config.ldif
	 * 
	 * @param configFile
	 *            new value of configFile
	 */
	public void setConfigFile(File configFile) {
		this.configFile = configFile;
	}

	/**
	 * Get the value of templateServerRoot
	 * 
	 * The top directory of template OpenDS installation. All the files from
	 * this directory will be copied to the working OpenDS directory
	 * (serverRoot). This usually happens before the tests.
	 * 
	 * @return the value of templateServerRoot
	 */
	public File getTemplateServerRoot() {
		return templateRoot;
	}

	/**
	 * Get the value of internalConnection
	 * 
	 * The connection to the OpenDS instance. It can be used to fetch and
	 * manipulate the data.
	 * 
	 * @return the value of internelConnection
	 */
	public InternalClientConnection getInternalConnection() {
		return internalConnection;
	}

	/**
	 * Refresh working OpenDS installation from the template.
	 * 
	 * The existing working OpenDS installation (in serverRoot) will be
	 * discarded and replaced by a fresh known-state setup (from
	 * templateServerRoot).
	 * 
	 * @throws IOException
	 */
	public void refreshFromTemplate() throws IOException {
		deleteDirectory(serverRoot);
		extractTemplate(serverRoot);
	}

	/**
	 * Extract tepmplate from class
	 * 
	 * @param destination
	 * @throws IOException
	 */
	private void extractTemplate(File dst) throws IOException {

		LOGGER.info("Extracting OpenDJ template....");
		if (!dst.exists()) {
			LOGGER.debug("Creating target dir {}", dst.getPath());
			dst.mkdirs();
		}

		// Determing if we need to extract from JAR or directory
		if (templateRoot.isDirectory()) {
			LOGGER.trace("Need to do directory copy.");
			copyDirectory(templateRoot, dst);
			return;
		}

		LOGGER.debug("Try to localize OpenDJ Template in JARs as " + DATA_TEMPLATE);

		URL srcUrl = ClassLoader.getSystemResource(DATA_TEMPLATE);
		// sample:
		// file:/C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar!/test-data/opendj.template
		// output:
		// C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar
		String srcName = srcUrl.getPath().substring(6).split("!/")[0];

		JarFile jar = new JarFile(srcName);
		LOGGER.debug("Extracting OpenDJ from JAR file {} to {}", srcName, dst.getPath());

		Enumeration<JarEntry> entries = jar.entries();

		JarEntry e;
		byte buf[] = new byte[655360];
		while (entries.hasMoreElements()) {
			e = entries.nextElement();

			// skip other files
			if (!e.getName().contains(DATA_TEMPLATE)) {
				continue;
			}

			// prepare destination file
			String filepath = e.getName().substring(DATA_TEMPLATE.length());
			File dstFile = new File(dst, filepath);
			
			//test if directory
			if (e.isDirectory()) {
				LOGGER.debug("Create directory: {}",dstFile.getAbsolutePath());
				dstFile.mkdirs();
				continue;
			}
			
			LOGGER.debug("Extract {} to {}", filepath, dstFile.getAbsolutePath());
			// Find file on classpath
			InputStream is = ClassLoader.getSystemResourceAsStream(e.getName());
			//InputStream is = jar.getInputStream(e);	//old way
			
			//Copy content
			OutputStream out = new FileOutputStream(dstFile);
			int len;
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			is.close();
		}
		jar.close();
		LOGGER.debug("OpenDJ Extracted");
	}

	/**
	 * Start the embedded OpenDJ directory server using files coppied from the
	 * template.
	 * 
	 * @return
	 * @throws IOException
	 */
	public InternalClientConnection startCleanServer() throws IOException {
		refreshFromTemplate();
		return start();
	}

	/**
	 * Start the embedded OpenDJ directory server.
	 * 
	 * Configuration and databases from serverRoot location will be used.
	 * 
	 * @return
	 */
	public InternalClientConnection start() {

		LOGGER.info("Starting OpenDJ server");

		DirectoryEnvironmentConfig envConfig = new DirectoryEnvironmentConfig();
		try {
			envConfig.setServerRoot(serverRoot);
			envConfig.setConfigFile(configFile);
			// envConfig.setDisableConnectionHandlers(true);
		} catch (InitializationException ex) {
			ex.printStackTrace();
			throw new RuntimeException("OpenDJ initialization failed", ex);
		}

		// Check if the server is already running
		if (EmbeddedUtils.isRunning()) {
			throw new RuntimeException("Server already running");
		} else {
			try {

				EmbeddedUtils.startServer(envConfig);

			} catch (ConfigException ex) {
				LOGGER.error("Possible OpenDJ misconfiguration: " + ex.getMessage(), ex);
				throw new RuntimeException("OpenDJ startup failed", ex);
			} catch (InitializationException ex) {
				LOGGER.error("OpenDJ startup failed", ex);
				throw new RuntimeException("OpenDJ startup failed", ex);
			}
		}

		internalConnection = InternalClientConnection.getRootConnection();
		if (internalConnection == null) {
			LOGGER.error("OpenDJ cannot get internal connection (null)");
			throw new RuntimeException("OpenDS cannot get internal connection (null)");
		}

		LOGGER.info("OpenDJ server started");

		return internalConnection;
	}

	/**
	 * Stop the embedded OpenDS server.
	 * 
	 */
	public void stop() {
		if (EmbeddedUtils.isRunning()) {
			LOGGER.debug("Stopping OpenDJ server");
			EmbeddedUtils.stopServer(this.getClass().getName(), Message.EMPTY);
			LOGGER.info("OpenDJ server is stopped");
		} else {
			LOGGER.warn("Attempt to stop OpenDJ server that is already stopped.");
		}
	}

	/**
	 * Copy a directory and its contents.
	 * 
	 * @param src
	 *            The name of the directory to copy.
	 * @param dst
	 *            The name of the destination directory.
	 * @throws IOException
	 *             If the directory could not be copied.
	 */
	public static void copyDirectory(File src, File dst) throws IOException {
		if (src.isDirectory()) {
			// Create the destination directory if it does not exist.
			if (!dst.exists()) {
				dst.mkdirs();
			}

			// Recursively copy sub-directories and files.
			for (String child : src.list()) {
				copyDirectory(new File(src, child), new File(dst, child));
			}
		} else {
			copyFile(src, dst);
		}
	}

	/**
	 * Delete a directory and its contents.
	 * 
	 * @param dir
	 *            The name of the directory to delete.
	 * @throws IOException
	 *             If the directory could not be deleted.
	 */
	public static void deleteDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			// Recursively delete sub-directories and files.
			for (String child : dir.list()) {
				deleteDirectory(new File(dir, child));
			}
		}

		dir.delete();
	}

	/**
	 * Copy a file.
	 * 
	 * @param src
	 *            The name of the source file.
	 * @param dst
	 *            The name of the destination file.
	 * @throws IOException
	 *             If the file could not be copied.
	 */
	public static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[8192];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public Set<String> asSet(List<Attribute> attributes) {
		// Just blindly get the fist one now.
		// There is most likely just one anyway.
		// TODO: improve that later

		// Attribute attr = attributes.get(0);
		Set<String> result = new HashSet<String>();

		// TODO find newer OpenDS jar
		// Iterator<AttributeValue> iterator = attr.iterator();
		// while (iterator.hasNext()) {
		// result.add(iterator.next().toString());
		// }

		return result;
	}
}

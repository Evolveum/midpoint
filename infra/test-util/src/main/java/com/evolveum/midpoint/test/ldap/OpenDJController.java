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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

/**
 * This class controls embedded OpenDJ instance.
 * 
 * It is used in Unit tests. It configures and starts and stops the instance.
 * It can even manage a "template" configuration of OpenDJ and copy it to
 * working instance configuration.
 *
 * @author Radovan Semancik
 */
public class OpenDJController {

    protected File serverRoot = new File("target/test-data/opendj");
    protected File configFile = null;
    protected File templateServerRoot = new File("test-data/opendj.template");


    protected InternalClientConnection internalConnection;

    public OpenDJController() {
        init();
    }


    public OpenDJController(File serverRoot) {
    	this.serverRoot = serverRoot;
        init();
    }

    public OpenDJController(File serverRoot,File templateServerRoot) {
    	this.serverRoot = serverRoot;
    	this.templateServerRoot = templateServerRoot;
        init();
    }

    public OpenDJController(String serverRootDirname) {
        this.serverRoot = new File(serverRootDirname);
        init();
    }

    private void init() {
        if (!serverRoot.exists()){
            serverRoot.mkdirs();
        }
        if (configFile == null) {
        	configFile = new File(serverRoot, "config/config.ldif");
        }
    }

    /**
     * Get the value of serverRoot.
     *
     * The top directory of working OpenDS installation.
     * The OpenDS placed in this directory will be used during
     * the tests.
     *
     * @return the value of serverRoot
     */
     public File getServerRoot() {
        return this.serverRoot;
    }

    /**
     * Set the value of serverRoot
     *
     * The top directory of working OpenDS installation.
     * The OpenDS placed in this directory will be used during
     * the tests.
     *
     * @param serverRoot new value of serverRoot
     */
    public void setServerRoot(File serverRoot) {
        this.serverRoot = serverRoot;
    }

    /**
     * Get the value of configFile
     *
     * File name of primary OpenDS configuration file.
     * Normally <serverRoot>/config/config.ldif
     *
     * @return the value of configFile
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     * Set the value of configFile
     *
     * File name of primary OpenDS configuration file.
     * Normally <serverRoot>/config/config.ldif
     *
     * @param configFile new value of configFile
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Get the value of templateServerRoot
     *
     * The top directory of template OpenDS installation.
     * All the files from this directory will be copied to the working
     * OpenDS directory (serverRoot). This usually happens before the tests.
     *
     * @return the value of templateServerRoot
     */
    public File getTemplateServerRoot() {
        return templateServerRoot;
    }

    /**
     * Set the value of templateServerRoot
     *
     * The top directory of template OpenDS installation.
     * All the files from this directory will be copied to the working
     * OpenDS directory (serverRoot). This usually happens before the tests.
     * 
     * @param templateServerRoot new value of templateServerRoot
     */
    public void setTemplateServerRoot(File templateServerRoot) {
        this.templateServerRoot = templateServerRoot;
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
     * Set the value of internalConnection
     *
     * The connection to the OpenDS instance. It can be used to fetch and
     * manipulate the data.
     *
     * @param internelConnection new value of internelConnection
     */
    public void setInternalConnection(InternalClientConnection internalConnection) {
        this.internalConnection = internalConnection;
    }

    /**
     * Refresh working OpenDS installation from the template.
     *
     * The existing working OpenDS installation (in serverRoot) will be
     * discarded and replaced by a fresh known-state setup (from templateServerRoot).
     *
     * @throws IOException
     */
    public void refreshFromTemplate() throws IOException {
        deleteDirectory(serverRoot);
        File template = locateTemplate();
        if (template.isDirectory()) {
        	// If the template is already expanded, copy it
        	copyDirectory(template,serverRoot);
        } else {
        	// Otherwise expand it to the destination place
        	extractTemplate(template,templateServerRoot.getPath());
        }
    }
    
    private File locateTemplate() {
		
    	// E.g. if running directly from classes
		if (templateServerRoot.isDirectory()) {
			return templateServerRoot;
		}
		
		String templateResourcePath = ClassLoader.getSystemResource(templateServerRoot.getPath()).getPath();
		System.out.println("Template resource path: "+templateResourcePath);

		File templateResourceFile = new File(templateResourcePath);
		// E.g. In case of exploded war
		if (templateResourceFile.isDirectory()) {
			return templateResourceFile;
		}
		
		// E.g. in case of JAR file
		System.out.println("Using OpenDJ template from a system resource "+templateResourcePath);
		templateResourcePath = templateResourcePath.replace("file:", "").split("!")[0];
		return new File(templateResourcePath);
    }
    
	private void extractTemplate(File templateResourceFile, String templatePath) throws IOException {
		System.out.println("*** Extracting OpenDJ from JAR file "+templateResourceFile+" to "+serverRoot.getPath());
		JarFile jarfile = null;
		jarfile = new JarFile(templateResourceFile);

		if (!serverRoot.exists()) {
			serverRoot.mkdirs();
		}

		for (Enumeration<JarEntry> entries = jarfile.entries(); entries.hasMoreElements();) {
			JarEntry jarEntry = entries.nextElement();
			if (jarEntry.getName().contains(templatePath)) {
				String srcPath = jarEntry.getName();
				String dstRelPath = srcPath.replace(templatePath, "");
				if (dstRelPath.length() < 3)
					continue;
				String dstPath = serverRoot.getPath() + dstRelPath;
				System.out.println("[Copy] JAR:" + srcPath + " --> " + dstPath + "  (" + jarEntry.getSize() + " bytes)");
				
				if ( jarEntry.getSize() == 0  && dstPath.endsWith("/") ) {
					new File(dstPath).mkdirs();
					continue;
				}
				if (new File(dstPath).exists())
					continue;
				
				InputStream is = null;
				OutputStream out = new FileOutputStream(dstPath);
				byte buf[] = new byte[65536];
				is = jarfile.getInputStream(jarEntry);
				int len;
				while ((len = is.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				is.close();
			}
		}
		System.out.println("*** Extracted");
	}

    
    /**
     * Start the embedded OpenDJ directory server using files coppied from the template.
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

        DirectoryEnvironmentConfig envConfig = new DirectoryEnvironmentConfig();
        try {
            envConfig.setServerRoot(serverRoot);
            envConfig.setConfigFile(configFile);
            //envConfig.setDisableConnectionHandlers(true);
        } catch (InitializationException ex) {
            ex.printStackTrace();
            throw new RuntimeException("OpenDS initialization failed", ex);
        }

        // Check if the server is already running
        if (EmbeddedUtils.isRunning()) {
            throw new RuntimeException("Server already running");
        } else {
            try {

                EmbeddedUtils.startServer(envConfig);

            } catch (ConfigException ex) {
            	System.out.println("Possible OpenDJ misconfiguration: "+ex.getMessage());
                ex.printStackTrace();
                throw new RuntimeException("OpenDS startup failed", ex);
            } catch (InitializationException ex) {
                ex.printStackTrace();
                throw new RuntimeException("OpenDS startup failed", ex);
            }
        }

        internalConnection = InternalClientConnection.getRootConnection();
        if (internalConnection == null) {
            throw new RuntimeException("OpenDS cannot get internal connection (null)");
        }

        return internalConnection;
    }

    /**
     * Stop the embedded OpenDS server.
     *
     */
    public void stop() {
        if (EmbeddedUtils.isRunning()){
            System.out.println("Stopping OpenDJ server");
            EmbeddedUtils.stopServer(this.getClass().getName(), Message.EMPTY);
            System.out.println("OpenDJ server is stopped");
        } else {

            System.err.println("Warrning: OpenDJ server is already stopped.");
        }
    }

    /**
     * Copy a directory and its contents.
     *
     * @param src
     *          The name of the directory to copy.
     * @param dst
     *          The name of the destination directory.
     * @throws IOException
     *           If the directory could not be copied.
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
     *          The name of the directory to delete.
     * @throws IOException
     *           If the directory could not be deleted.
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
     *          The name of the source file.
     * @param dst
     *          The name of the destination file.
     * @throws IOException
     *           If the file could not be copied.
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

        //Attribute attr = attributes.get(0);
        Set<String> result = new HashSet<String>();


        //TODO find newer OpenDS jar
//        Iterator<AttributeValue> iterator = attr.iterator();
//        while (iterator.hasNext()) {
//            result.add(iterator.next().toString());
//        }

        return result;
    }
}

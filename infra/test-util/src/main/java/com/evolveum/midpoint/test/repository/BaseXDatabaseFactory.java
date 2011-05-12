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

package com.evolveum.midpoint.test.repository;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.xmldb.api.modules.*;
import org.xmldb.api.base.*;
import org.xmldb.api.*;
import org.basex.BaseXServer;
import org.basex.server.ClientSession;

/**
 * Factory class for embedded BaseX XML database that mocks
 * real midPoint repository.
 * 
 * Only one databse is started, but multiple port
 * instances are returned.
 * 
 * TODO: This really, really needs refactoring. It is 
 * unreadable and object management is all wrong.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class BaseXDatabaseFactory {

    public static final String code_id = "$Id$";

    /** Database context. */
    static final Context CONTEXT = new Context();

    /** Database driver. */
    public static final String DRIVER = "org.basex.api.xmldb.BXDatabase";

    /** Database url. */
    static final String URI = "xmldb:basex://localhost:1984/midPoint";

    /** Sample query. */
    private static final String QUERY = "//object";
    
    private static final String DEFAULT_INIT_FILE_PATH = "../../infra/test-util/src/main/resources/test-data/repository/";

    /** Session reference. */
    private static ClientSession session;

    /** Single Collection. */
    private static Collection collection = null;
    // ------------------------------------------------------------------------
    // Start server on default port 1984.

    private static BaseXServer server = null;

    /**
     * TODO remmove  testClazz attribute
     * @param testClazz
     * @return
     */
    @Deprecated
    public static RepositoryPortType getRepositoryPort(Class testClazz) {
        return getRepositoryPort(new File(DEFAULT_INIT_FILE_PATH));
    }
    
    public static RepositoryPortType getRepositoryPort() {
        return getRepositoryPort(new File(DEFAULT_INIT_FILE_PATH));
    }

    public static RepositoryPortType getRepositoryPort(File base) {
        RepositoryService port = null;
        try {
            
            if (!base.exists()) {
                throw new IllegalArgumentException("Repository initialization file does not exists " + base.getAbsolutePath());
            }

            String initPath = base.getAbsolutePath();
            if (base.isDirectory()) {
            	initPath = initPath + File.separator; 
            }
            XMLServerStart(initPath, new String[]{"-d"});
            // Register the database.
            Class<?> c = Class.forName(DRIVER);
            Database db = (Database) c.newInstance();
            DatabaseManager.registerDatabase(db);
            // Collection instance.
            collection = DatabaseManager.getCollection(URI, "admin", "admin");
            
            JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            port = new RepositoryService();
            port.initialis(collection, ctx);

        } catch (Exception ex) {
            ex.printStackTrace();
            XMLServerStop();
            port = null;
        }
        return port;
    }

    /**
     * 
     * TODO: Refactor needed. The method name is wrong.
     * 
     * @param args (ignored) command-line arguments
     * @throws Exception exception
     */
    public static void XMLServerStart(String base, final String[] args) throws Exception {
        if (null == server) {
            System.out.println("=== Mock midPoint Repository (Embedded BaseX Server) ===");
            System.out.println("Base: "+base);
            
            // ------------------------------------------------------------------------
            // Start server on default port 1984.
            server = new BaseXServer(args);

            // ------------------------------------------------------------------------
            // Create a client session with host name, port, user name and password
            System.out.println("\n* Create a client session.");

            session = new ClientSession("localhost", 1984, "admin", "admin");

            // ------------------------------------------------------------------------
            // Create a database
            String DBPath = String.format("CREATE DB midPoint %s", base);

            System.out.println("\n* Create a database: " + DBPath);
            
            session.execute(DBPath);
        }
    }
    
    /**
     * Shut down the mock repository.
     * 
     * TODO: Refactor needed. The method name is wrong.
     * 
     * @param args (ignored) command-line arguments
     * @throws Exception exception
     */
    public static void XMLServerStop() {
        if (null != collection) {
            // ------------------------------------------------------------------------
            // Close the collection
            System.out.println("\n* Close the collection.");
            try {
                collection.close();
            } catch (XMLDBException ex) {
            } finally {
                collection = null;
            }
        }
        if (null != session) {
            try {
                // ------------------------------------------------------------------------
                // Drop the database
                System.out.println("\n* Close and drop the database.");
                session.execute("DROP DB midPoint");

                // ------------------------------------------------------------------------
                // Close the client session
                System.out.println("\n* Close the client session.");

                session.close();
            } catch (IOException ex) {
            	// TODO!!!
            } catch (BaseXException ex) {
            	// TODO!!!!
            } finally {
                session = null;
            }
        }
        if (null != server) {

            // ------------------------------------------------------------------------
            // Stop the server
            System.out.println("\n* Stop the server.");

            server.stop();
            server = null;
        }
    }

    /**
     * TODO: Refactor needed. The method name is wrong.
     * 
     * @param args (ignored) command-line arguments
     * @throws Exception exception
     */
    public void XMLDBQuery() throws Exception {

        System.out.println("=== XMLDBQuery ===");

        System.out.println("\n* Run query via XML:DB:");

        // Collection instance.
        Collection coll = null;

        try {
            // Register the database.
            Class<?> c = Class.forName(DRIVER);
            Database db = (Database) c.newInstance();
            DatabaseManager.registerDatabase(db);

            // Receive the database.
            coll = DatabaseManager.getCollection(URI);

            // Receive the XPath query service.
            XPathQueryService service = (XPathQueryService) coll.getService("XPathQueryService", "1.0");

            // Execute the query and receives all results.
            ResourceSet set = service.query(QUERY);

            // Create a result iterator.
            ResourceIterator iter = set.getIterator();

            // Loop through all result items.
            while (iter.hasMoreResources()) {
                // Receive the next results.
                Resource res = iter.nextResource();

                // Write the result to the console.
                System.out.println(res.getContent());
            }
        } catch (final XMLDBException ex) {
            // Handle exceptions.
            System.err.println("XML:DB Exception occured " + ex.errorCode);
        } finally {
            // Close the collection.
            if (coll != null) {
                coll.close();
            }
        }
    }
    
}

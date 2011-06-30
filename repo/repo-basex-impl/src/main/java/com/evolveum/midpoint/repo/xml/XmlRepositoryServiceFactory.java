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
 * Portions Copyrighted 2011 Igor Farinic
 */
package com.evolveum.midpoint.repo.xml;

import java.io.IOException;

import org.basex.BaseXServer;
import org.basex.core.BaseXException;
import org.basex.server.ClientQuery;
import org.basex.server.ClientSession;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

public class XmlRepositoryServiceFactory {

	/** Database driver. */
	public static final String DRIVER = "org.basex.api.xmldb.BXDatabase";

	private static final String url = "xmldb:basex://localhost:1984/midPoint";

	BaseXServer server;

	public void init() throws RepositoryServiceFactoryException {

		// set debug mode for BaseX server
		String args = "-d";
		// start BaseX server
		server = new BaseXServer(args);
		
		boolean newDb = false;

		try {
			ClientSession session = new ClientSession("localhost", 1984, "admin", "admin");
			try {
			session.execute("OPEN midPoint");
			} catch (BaseXException ex) {
				if ("Database 'midPoint' was not found.".equals(ex.getMessage())) {
					newDb = true;
				}
			}
			if (newDb) {
				//create and initialize midPoint DB
				// FIXME: remove hardcoded values
				session.execute("CREATE DB midPoint");
				String serializedObject = "<c:objects xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"/>";
				StringBuilder query = new StringBuilder()
						.append("declare namespace c='" + SchemaConstants.NS_C + "';\n")
						.append("let $x := ").append(serializedObject).append("\n")
						.append("return insert node $x into doc(\"midPoint\")");

				ClientQuery cq;

				cq = session.query(query.toString());
				cq.execute();
			}
		} catch (BaseXException e) {
			throw new RepositoryServiceFactoryException("XML DB Exception during DB initialization", e);
		} catch (IOException e) {
			throw new RepositoryServiceFactoryException("XML DB IO Exception during DB initialization", e);
		}

	}

	public void destroy() {
		if (null != server) {
			server.stop();
		}
	}

	public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
		try {

			// TODO: remove hardcoded values
			ClientSession session = new ClientSession("localhost", 1984, "admin", "admin");
			session.execute("OPEN midPoint");
			RepositoryService repositoryService = new XmlRepositoryService(session);
			return repositoryService;
		} catch (IOException e) {
			throw new RepositoryServiceFactoryException("XML DB IO Exception during client initialization", e);
		} catch (BaseXException e) {
			throw new RepositoryServiceFactoryException("XML DB Exception during client initialization", e);
		}
	}

}

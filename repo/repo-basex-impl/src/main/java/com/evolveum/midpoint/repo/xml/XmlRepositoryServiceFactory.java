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

import org.basex.api.xmldb.BXCollection;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

public class XmlRepositoryServiceFactory {

	/** Database driver. */
	public static final String DRIVER = "org.basex.api.xmldb.BXDatabase";

	private RepositoryPortType repositoryService;
	private static final String url = "xmldb:basex://localhost:1984/midPoint";

	public void init() throws RepositoryServiceFactoryException {
		Class<?> c;
		try {
			c = Class.forName(DRIVER);
			Database db = (Database) c.newInstance();
			DatabaseManager.registerDatabase(db);
			BXCollection collection = new BXCollection("midPoint", false);

			XMLResource res = (XMLResource) collection.createResource("objects", XMLResource.RESOURCE_TYPE);
			res.setContent("<c:objects xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"/>");
			collection.storeResource(res);
		} catch (ClassNotFoundException e) {
			throw new RepositoryServiceFactoryException("Class " + DRIVER
					+ " for XML DB driver was not found", e);
		} catch (InstantiationException e) {
			throw new RepositoryServiceFactoryException("Failed to create instance for class " + DRIVER, e);
		} catch (IllegalAccessException e) {
			throw new RepositoryServiceFactoryException("Failed to create instance for class " + DRIVER, e);
		} catch (XMLDBException e) {
			throw new RepositoryServiceFactoryException("Xml db exception during client initialization", e);
		}

	}

	public RepositoryPortType getRepositoryService() throws RepositoryServiceFactoryException {
		if (null == repositoryService) {
			try {
				Collection collection = DatabaseManager.getCollection(url);
				if (null == collection) {
					throw new RepositoryServiceFactoryException(
							"Error retrieving midPoint collection from xml database");
				}
				repositoryService = new XmlRepositoryService(collection);
			} catch (XMLDBException e) {
				throw new RepositoryServiceFactoryException("Xml db exception during client initialization",
						e);
			}
		}

		return repositoryService;
	}

}

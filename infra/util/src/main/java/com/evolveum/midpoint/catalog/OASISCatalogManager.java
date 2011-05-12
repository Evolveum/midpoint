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

package com.evolveum.midpoint.catalog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

public class OASISCatalogManager {

    public static final String DEFAULT_CATALOG_NAME_WAR = "../jax-ws-catalog.xml";
    public static final String DEFAULT_CATALOG_NAME_JAR = "META-INF/jax-ws-catalog.xml";
    public static final String CATALOG_DEBUG_KEY = "OASISCatalogManager.catalog.debug.level";
    private static final Logger LOG = Logger.getLogger(OASISCatalogManager.class.getName());
    private static final String DEBUG_LEVEL = System.getProperty(CATALOG_DEBUG_KEY);
    private Catalog resolver;
    private Set<URL> loadedCatalogs = Collections.synchronizedSet(new HashSet<URL>());

    public OASISCatalogManager() {
        CatalogManager catalogManager = new CatalogManager();
        if (DEBUG_LEVEL != null) {
            catalogManager.debug.setDebug(Integer.parseInt(DEBUG_LEVEL));
        }
        catalogManager.setUseStaticCatalog(false);
        catalogManager.setIgnoreMissingProperties(true);
        CatalogResolver catalogResolver = new CatalogResolver(catalogManager);
        this.resolver = catalogResolver.getCatalog();
    }

    public Catalog getCatalog() {
        return this.resolver;
    }

    public void loadContextCatalogs() {
        loadContextCatalogs(DEFAULT_CATALOG_NAME_WAR);
        loadContextCatalogs(DEFAULT_CATALOG_NAME_JAR);
    }

    public void loadContextCatalogs(String name) {
        try {
            loadCatalogs(Thread.currentThread().getContextClassLoader(), name);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error loading " + name + " catalog files", e);
        }
    }

    public void loadCatalogs(ClassLoader classLoader, String name) throws IOException {
        if (classLoader == null) {
            return;
        }

        Enumeration<URL> catalogs = classLoader.getResources(name);
        while (catalogs.hasMoreElements()) {
            URL catalogURL = catalogs.nextElement();
            if (!loadedCatalogs.contains(catalogURL)) {
                this.resolver.parseCatalog(catalogURL);
                loadedCatalogs.add(catalogURL);
            }
        }
    }

    public void loadCatalog(URL catalogURL) throws IOException {
        if (!loadedCatalogs.contains(catalogURL)) {
            if ("file".equals(catalogURL.getProtocol())) {
                try {
                    File file = new File(catalogURL.toURI());
                    if (!file.exists()) {
                        throw new FileNotFoundException(file.getAbsolutePath());
                    }
                } catch (URISyntaxException e) {
                    //just process as is
                }
            }

            this.resolver.parseCatalog(catalogURL);

            loadedCatalogs.add(catalogURL);
        }
    }

    public static OASISCatalogManager getContextCatalog() {
        OASISCatalogManager oasisCatalog = new OASISCatalogManager();
        oasisCatalog.loadContextCatalogs();
        return oasisCatalog;
    }
}

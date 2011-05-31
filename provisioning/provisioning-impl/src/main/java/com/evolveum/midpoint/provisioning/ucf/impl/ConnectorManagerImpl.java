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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConfiguredConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.util.ClasspathUrlFinder;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Currently the only implementation of the UCF Connector Manager API interface.
 * 
 * It is hardcoded to ICF now.
 * 
 * @author Radovan Semancik
 */
public class ConnectorManagerImpl implements ConnectorManager {
	
	// This ususally refers to WEB-INF/lib/icf-connectors
	private static final String BUNDLE_PATH = "../../lib/icf-connectors";
	private static final String BUNDLE_PREFIX = "org.identityconnectors";
	private static final String BUNDLE_SUFFIX = ".jar";
	
	private static final Trace log = TraceManager.getTrace(ConnectorManagerImpl.class);
	
	private ConnectorInfoManager localConnectorInfoManager;
	private Map<String,ConnectorInfo> connectors;

	public ConnectorManagerImpl() {
	}
	
	public void initialize() {
		Set<URL> bundleURLs = listBundleJars();
		
		connectors = new HashMap<String, ConnectorInfo>();
		List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
                ConnectorKey key = connectorInfo.getConnectorKey();
                String mapKey = keyToString(key);
				connectors.put(mapKey,connectorInfo);
        }
	}

	@Override
	public ConfiguredConnector createConfiguredConnector(ResourceType resource) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<ConnectorType> listConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String,ConnectorInfo> e : connectors.entrySet()) {
			ConnectorType connectorType = new ConnectorType();
			String oid = e.getKey();
			connectorType.setOid(oid);
			ConnectorInfo cinfo = e.getValue();
			ConnectorKey key = cinfo.getConnectorKey();
			connectorType.setName("ICF "+key.getConnectorName());
			connectorType.setVersion(key.getBundleVersion());
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}
	
	private String keyToString(ConnectorKey key) {
		StringBuilder sb = new StringBuilder();
		sb.append(key.getBundleName());
		sb.append("-");
		sb.append(key.getBundleVersion());
		sb.append("-");
		sb.append(key.getConnectorName());
		return sb.toString();
	}
	
	/**
	 * Returns ICF connector info manager that manages local connectors.
	 * The manager will be created if it does not exist yet.
	 * @return ICF connector info manager that manages local connectors
	 */
	private ConnectorInfoManager getLocalConnectorInfoManager() {
		if (null == localConnectorInfoManager) {
			Set<URL> bundleUrls = listBundleJars();
			ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
			localConnectorInfoManager = factory.getLocalManager(bundleUrls.toArray(new URL[0]));
		}
		return localConnectorInfoManager;
	}
	
	/**
	 * Lists all ICF connector bundles, either in BUNDLE_PATH or in current classpath.
	 * 
	 * @return set of connector bundle URLs.
	 */
	private Set<URL> listBundleJars() {
		Set<URL> bundleURLs = new HashSet<URL>();
		
		// Look for connectors in the BUNDLE_PATH folder
		
		File icfFolder = null;
		try {
			icfFolder = new File(new File(this.getClass().getClassLoader().getResource("com").toURI()),
					BUNDLE_PATH);
		} catch (URISyntaxException ex) {
			log.debug("Couldn't find icf-connectors folder, reason: " + ex.getMessage());
		}

		// Take only those that start with BUNDLE_PREFIX and end with BUNDLE_SUFFIX
		
		final FileFilter fileFilter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (!file.exists() || file.isDirectory()) {
					return false;
				}
				String fileName = file.getName();
				if (fileName.startsWith(BUNDLE_PREFIX) && fileName.endsWith(BUNDLE_SUFFIX)) {
					return true;
				}
				return false;
			}
		};

		if (icfFolder == null || !icfFolder.exists() || !icfFolder.isDirectory()) {
			// cannot find icfFolder, therefore we are going to seach classpath
			// this is useful in tests
			URL[] resourceURLs = ClasspathUrlFinder.findClassPaths();
			for (int j = 0; j < resourceURLs.length; j++) {
				URL bundleUrl = resourceURLs[j];
				if ("file".equals(bundleUrl.getProtocol())) {
					File file = new File(bundleUrl.getFile());
					if (fileFilter.accept(file)) {
						bundleURLs.add(bundleUrl);
					}
				}
			}
		} else {
			// Looking in icfFolder
			File[] connectors = icfFolder.listFiles(fileFilter);
			for (File file : connectors) {
				try {
					bundleURLs.add(file.toURI().toURL());
				} catch (MalformedURLException ex) {
					log.debug("Couldn't transform file path " + file.getAbsolutePath()
							+ " to URL, reason: " + ex.getMessage());
				}
			}
		}

		if (log.isDebugEnabled()) {
			for (URL u : bundleURLs) {
				log.debug("Bundle URL: {}", u);
			}
		}

		return bundleURLs;
	}
	
}

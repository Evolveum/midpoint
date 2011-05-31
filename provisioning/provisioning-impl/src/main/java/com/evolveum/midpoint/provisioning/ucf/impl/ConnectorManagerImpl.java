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
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.common.security.GuardedString;
import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	private static final String CONFIGURATION_PROPERTIES_XML_ELEMENT_NAME = "configurationProperties";
	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns/resource/icf/";
	
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
		String connectorOid = getConnectorOid(resource);
		ConnectorInfo cinfo = findConnectorInfoByOid(connectorOid);
		APIConfiguration apiConfig = cinfo.createDefaultAPIConfiguration();
		
		transformConnectorConfiguration(apiConfig,resource);
		
		ConnectorFacade cfacade =
                    ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
		
		ConfiguredConnectorImpl connectorImpl = new ConfiguredConnectorImpl(cfacade);
		
		return connectorImpl;
	}

	@Override
	public Set<ConnectorType> listConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String,ConnectorInfo> e : connectors.entrySet()) {			
			String oid = e.getKey();			
			ConnectorInfo cinfo = e.getValue();
			ConnectorType connectorType = convertToConnectorType(cinfo, oid);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}

	@Override
	public ConnectorType getConnector(String oid) {
		ConnectorInfo cinfo = findConnectorInfoByOid(oid);
		return convertToConnectorType(cinfo, oid);
	}
	
	private ConnectorType convertToConnectorType(ConnectorInfo cinfo,String oid) {
		ConnectorType connectorType = new ConnectorType();
		connectorType.setOid(oid);
			ConnectorKey key = cinfo.getConnectorKey();
			connectorType.setName("ICF "+key.getConnectorName());
			connectorType.setNamespace(ICF_CONFIGURATION_NAMESPACE_PREFIX+oid);
			connectorType.setConnectorVersion(key.getBundleVersion());
			return connectorType;
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
	
	private void transformConnectorConfiguration(APIConfiguration apiConfig, ResourceType resource) {
		
		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();
		
		String connectorConfNs = getConnectorType(resource).getNamespace();
		List<Element> xmlConfig = resource.getConfiguration().getAny();
		for (Element e : xmlConfig) {
			if (e.getNamespaceURI()!=null && e.getNamespaceURI().equals(connectorConfNs) &&
				e.getLocalName()!=null && e.getLocalName().equals(CONFIGURATION_PROPERTIES_XML_ELEMENT_NAME)) {
				NodeList configurationNodelist = e.getChildNodes();
				
				for (int i=0;i<configurationNodelist.getLength();i++) {
					Node node = configurationNodelist.item(i);
					if (node.getNodeType()==Node.ELEMENT_NODE) {
						Element configElement = (Element) node;
						
						if (configElement.getNamespaceURI() == null || !configElement.getNamespaceURI().equals(connectorConfNs)) {
							log.warn("Found element with a wrong namespace ({}) in resource OID={}",configElement.getNamespaceURI(),resource.getOid());
						} else {
							
							String propertyName = configElement.getLocalName();
							ConfigurationProperty property = configProps.getProperty(propertyName);
							Class type = property.getType();
							
							if (type.isArray()) {
								List<Object> values = new ArrayList<Object>();
								Object value = convertToJava(configElement,type.getComponentType());
								values.add(value);
								// Loop over until the elements have the same local name
								while (i+1<configurationNodelist.getLength() &&
									   configurationNodelist.item(i+1).getNodeType()==Node.ELEMENT_NODE &&
									   ((Element)(configurationNodelist.item(i+1))).getLocalName().equals(propertyName)) {
									i++;
									configElement = (Element)configurationNodelist.item(i);
									Object avalue = convertToJava(configElement,type.getComponentType());
									values.add(avalue);
								}
								property.setValue(values.toArray());
								
							} else {
								Object value = convertToJava(configElement,type);
								property.setValue(value);
							}
						}
					}
				}
			}
		}
		
		// TODO: pools, etc.
		
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
	
	private ConnectorInfo findConnectorInfoByOid(String connectorOid) {
		return connectors.get(connectorOid);
	}

	// TODO: Following two methods should do into some kind of ResourceUtil...
	
	private String getConnectorOid(ResourceType resource) {
		if (resource.getConnectorRef()!=null) {
			return resource.getConnectorRef().getOid();
		} else if (resource.getConnector()!=null) {
			return resource.getConnector().getOid();
		} else {
			return null;
		}
	}
	
	private ConnectorType getConnectorType(ResourceType resource) {
		if (resource.getConnector()!=null) {
			return resource.getConnector();
		} else if (resource.getConnectorRef()!=null) {
			String oid = resource.getConnectorRef().getOid();
			return getConnector(oid);
		} else {
			log.error("Resource does not contain connector or connectorRef (OID="+resource.getOid()+")");
			throw new IllegalArgumentException("Resource does not contain connector or connectorRef (OID="+resource.getOid()+")");
		}
	}
	
	// Kind of a hack now. Should be switched to a more sophisticated converters later
	private Object convertToJava(Element configElement, Class type) {
		String stringContent = configElement.getTextContent();
		if (type.equals(String.class)) {
			return stringContent;
		} else if (type.equals(Integer.class)) {
			return Integer.valueOf(stringContent);
		} else if (type.equals(int.class)) {
			return Integer.parseInt(stringContent);
		} else if (type.equals(GuardedString.class)) {
			return new GuardedString(stringContent.toCharArray());
		} else {
			log.error("Unknown type for ICF conversion: {}",type);
			throw new IllegalArgumentException("Unknown type for ICF conversion: "+type);
		}
	}
	
}

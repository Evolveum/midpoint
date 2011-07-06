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

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.object.ObjectResolver;
import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.ClasspathUrlFinder;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Currently the only implementation of the UCF Connector Manager API interface.
 * 
 * It is hardcoded to ICF now.
 * 
 * This class holds a list of all known ICF connectors in the system.
 * 
 * @author Radovan Semancik
 */
@Component
public class ConnectorManagerIcfImpl implements ConnectorManager {

	// This ususally refers to WEB-INF/lib/icf-connectors
	private static final String BUNDLE_PATH = "../../lib/icf-connectors";
	private static final String BUNDLE_PREFIX = "org.identityconnectors";
	private static final String BUNDLE_SUFFIX = ".jar";
	private static final String CONFIGURATION_PROPERTIES_XML_ELEMENT_NAME = "configurationProperties";
	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns/resource/icf/";
	private static final Trace log = TraceManager.getTrace(ConnectorManagerIcfImpl.class);
	private ConnectorInfoManager localConnectorInfoManager;
	private Map<String, ConnectorInfo> connectors;

	public ConnectorManagerIcfImpl() {
	}

	/**
	 * Initialize the ICF implementation. Look for all connector bundles,
	 * get basic information about them and keep that in memory.
	 */
	@PostConstruct
	public void initialize() {
		Set<URL> bundleURLs = listBundleJars();

		connectors = new HashMap<String, ConnectorInfo>();
		List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
			ConnectorKey key = connectorInfo.getConnectorKey();
			String mapKey = keyToString(key);
			connectors.put(mapKey, connectorInfo);
		}
	}

	/**
	 * Creates new connector instance.
	 * 
	 * It will initialize the connector by taking the XML Resource definition,
	 * transforming it to the ICF configuration and applying that to the 
	 * new connector instance.
	 * @throws ObjectNotFoundException 
	 * 
	 */
	@Override
	public ConnectorInstance createConnectorInstance(ResourceType resource) throws ObjectNotFoundException {
		// Take the ConnectorInfo from the memory store
		String connectorOid = ResourceTypeUtil.getConnectorOid(resource);
		ConnectorInfo cinfo = findConnectorInfoByOid(connectorOid);
		
		if (cinfo==null) {
			throw new ObjectNotFoundException("The connector with OID "+connectorOid+" was not found");
		}
		
		// Get default configuration for the connector. This is important, as
		// it contains types of connector configuration properties.
		// So we do not need to know the connector configuration schema
		// here. We are in fact looking at the data that the schema is
		// generated from.
		APIConfiguration apiConfig = cinfo.createDefaultAPIConfiguration();

		// Transform XML configuration from the resource to the ICF connector
		// configuration
		transformConnectorConfiguration(apiConfig, resource);

		// Create new connector instance using the transformed configuration
		ConnectorFacade cfacade =
				ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

		// Create new midPoint ConnectorInstance and pass it the ICF connector facade
		ConnectorInstanceIcfImpl connectorImpl = new ConnectorInstanceIcfImpl(cfacade,resource);

		return connectorImpl;
	}

	/**
	 * Returns a list XML representation of the ICF connectors.
	 */
	@Override
	public Set<ConnectorType> listConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String, ConnectorInfo> e : connectors.entrySet()) {
			String oid = e.getKey();
			ConnectorInfo cinfo = e.getValue();
			ConnectorType connectorType = convertToConnectorType(cinfo, oid);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}

	/**
	 * Returns a single XML representation of ICF connector.
	 */
	@Override
	public ConnectorType getConnector(String oid) {
		ConnectorInfo cinfo = findConnectorInfoByOid(oid);
		return convertToConnectorType(cinfo, oid);
	}

	/**
	 * Converts ICF ConnectorInfo into a midPoint XML connector representation.
	 * 
	 * TODO: schema transformation
	 */
	private ConnectorType convertToConnectorType(ConnectorInfo cinfo, String oid) {
		ConnectorType connectorType = new ConnectorType();
		connectorType.setOid(oid);
		ConnectorKey key = cinfo.getConnectorKey();
		connectorType.setName("ICF " + key.getConnectorName());
		connectorType.setNamespace(ICF_CONFIGURATION_NAMESPACE_PREFIX + oid);
		connectorType.setConnectorVersion(key.getBundleVersion());
		return connectorType;
	}

	/**
	 * Converts ICF connector key to a simple string.
	 * 
	 * The string may be used as an OID.
	 */
	private String keyToString(ConnectorKey key) {
		StringBuilder sb = new StringBuilder("icf1-");
		sb.append(key.getBundleName());
		sb.append("-");
		sb.append(key.getBundleVersion());
		sb.append("-");
		sb.append(key.getConnectorName());
		return sb.toString();
	}

	/**
	 * Transforms midPoint XML configuration of the connector to the ICF
	 * configuration.
	 * 
	 * The "configuration" part of the XML resource definition will be used.
	 * 
	 * The provided ICF APIConfiguration will be modified, some values may be
	 * overwritten.
	 * 
	 * @param apiConfig ICF connector configuration
	 * @param resource  midPoint XML configuration
	 */
	private void transformConnectorConfiguration(APIConfiguration apiConfig, ResourceType resource) {

		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// The namespace of all the configuration properties specific to the
		// connector instance will have a connector instance namespace. This
		// namespace can be found in the resource definition.
		String connectorConfNs = getConnectorType(resource).getNamespace();
		
		// Iterate over all the elements of XML resource definition that are in
		// the "configuration" part.
		List<Element> xmlConfig = resource.getConfiguration().getAny();
		for (Element e : xmlConfig) {
			
			// Process the "configurationProperties" part of configuraiton
			if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(connectorConfNs)
					&& e.getLocalName() != null && e.getLocalName().equals(CONFIGURATION_PROPERTIES_XML_ELEMENT_NAME)) {
				
				// Iterate over all the XML elements there. Each element is
				// a configuration property.
				NodeList configurationNodelist = e.getChildNodes();
				for (int i = 0; i < configurationNodelist.getLength(); i++) {
					Node node = configurationNodelist.item(i);
					// We care only about elements, ignoring comments and text
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element configElement = (Element) node;

						// All the elements must be in a connector instance namespace.
						if (configElement.getNamespaceURI() == null || !configElement.getNamespaceURI().equals(connectorConfNs)) {
							log.warn("Found element with a wrong namespace ({}) in resource OID={}", configElement.getNamespaceURI(), resource.getOid());
						} else {

							// Local name of the element is the same as the name of ICF configuration property
							String propertyName = configElement.getLocalName();
							ConfigurationProperty property = configProps.getProperty(propertyName);
							
							// Check (java) type of ICF configuration property, behave accordingly
							Class type = property.getType();
							if (type.isArray()) {
								// Special handling for array values. If the type
								// of the property is array, the XML element may appear
								// several times.
								List<Object> values = new ArrayList<Object>();
								// Convert the first value
								Object value = convertToJava(configElement, type.getComponentType());
								values.add(value);
								// Loop over until the elements have the same local name
								while (i + 1 < configurationNodelist.getLength()
										&& configurationNodelist.item(i + 1).getNodeType() == Node.ELEMENT_NODE
										&& ((Element) (configurationNodelist.item(i + 1))).getLocalName().equals(propertyName)) {
									i++;
									configElement = (Element) configurationNodelist.item(i);
									// Conver all the remaining values
									Object avalue = convertToJava(configElement, type.getComponentType());
									values.add(avalue);
								}
								
								// Convert array to a list with appropriate type
								Object valuesArrary = Array.newInstance(type.getComponentType(), values.size());
								for (int j = 0; j < values.size(); ++j) {
									Object avalue = values.get(j);
									Array.set(valuesArrary, j, avalue);
								}
								property.setValue(valuesArrary);

							} else {
								// Single-valued propery are easy to convert
								Object value = convertToJava(configElement, type);
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
	 * 
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

	/**
	 * Gets connector from the resource, regardless whenther there is a
	 * "composite" connector or connectorRef.
	 */
	private ConnectorType getConnectorType(ResourceType resource) {

		ObjectResolver resolver = new ObjectResolver() {
			@Override
			public ObjectType resolve(String oid) {
				return getConnector(oid);
			}
		};
		ConnectorType connector = ResourceTypeUtil.getConnectorType(resource, resolver);
		if (connector==null) {
			log.error("Resource does not contain connector or connectorRef (OID=" + resource.getOid() + ")");
			throw new IllegalArgumentException("Resource does not contain connector or connectorRef (OID=" + resource.getOid() + ")");
		}
		return connector;
	}

	private Object convertToJava(Element configElement, Class type) {
		if (type.equals(GuardedString.class)) {
			// Guarded string is a special ICF beast
			return new GuardedString(configElement.getTextContent().toCharArray());
		} else {
			return XsdTypeConverter.toJavaValue(configElement, type);
		}
	}
}

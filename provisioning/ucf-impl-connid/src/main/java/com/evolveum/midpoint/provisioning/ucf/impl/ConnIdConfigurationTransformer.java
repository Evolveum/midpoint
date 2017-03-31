/**
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ResultsHandlerConfiguration;
import org.identityconnectors.framework.api.operations.APIOperation;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class ConnIdConfigurationTransformer {
	
	private static final Trace LOGGER = TraceManager.getTrace(ConnIdConfigurationTransformer.class);
	
	private ConnectorType connectorType;
	private ConnectorInfo cinfo;
	private Protector protector;
	
	public ConnIdConfigurationTransformer(ConnectorType connectorType, ConnectorInfo cinfo, Protector protector) {
		super();
		this.connectorType = connectorType;
		this.cinfo = cinfo;
		this.protector = protector;
	}

	/**
	 * Transforms midPoint XML configuration of the connector to the ICF
	 * configuration.
	 * <p/>
	 * The "configuration" part of the XML resource definition will be used.
	 * <p/>
	 * The provided ICF APIConfiguration will be modified, some values may be
	 * overwritten.
	 * 
	 * @param apiConfig
	 *            ICF connector configuration
	 * @param resourceType
	 *            midPoint XML configuration
	 * @throws SchemaException
	 * @throws ConfigurationException
	 */
	public APIConfiguration transformConnectorConfiguration(PrismContainerValue configuration)
			throws SchemaException, ConfigurationException {

		APIConfiguration apiConfig = cinfo.createDefaultAPIConfiguration();
		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// The namespace of all the configuration properties specific to the
		// connector instance will have a connector instance namespace. This
		// namespace can be found in the resource definition.
		String connectorConfNs = connectorType.getNamespace();

		PrismContainer configurationPropertiesContainer = configuration
				.findContainer(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		if (configurationPropertiesContainer == null) {
			// Also try this. This is an older way.
			configurationPropertiesContainer = configuration.findContainer(new QName(connectorConfNs,
					SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME));
		}

		transformConnectorConfiguration(configProps, configurationPropertiesContainer, connectorConfNs);

		PrismContainer connectorPoolContainer = configuration.findContainer(new QName(
				SchemaConstants.NS_ICF_CONFIGURATION,
				ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME));
		ObjectPoolConfiguration connectorPoolConfiguration = apiConfig.getConnectorPoolConfiguration();
		transformConnectorPoolConfiguration(connectorPoolConfiguration, connectorPoolContainer);

		PrismProperty producerBufferSizeProperty = configuration.findProperty(new QName(
				SchemaConstants.NS_ICF_CONFIGURATION,
				ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME));
		if (producerBufferSizeProperty != null) {
			apiConfig.setProducerBufferSize(parseInt(producerBufferSizeProperty));
		}

		PrismContainer connectorTimeoutsContainer = configuration.findContainer(new QName(
				SchemaConstants.NS_ICF_CONFIGURATION,
				ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME));
		transformConnectorTimeoutsConfiguration(apiConfig, connectorTimeoutsContainer);

        PrismContainer resultsHandlerConfigurationContainer = configuration.findContainer(new QName(
        		SchemaConstants.NS_ICF_CONFIGURATION,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME));
        ResultsHandlerConfiguration resultsHandlerConfiguration = apiConfig.getResultsHandlerConfiguration();
        transformResultsHandlerConfiguration(resultsHandlerConfiguration, resultsHandlerConfigurationContainer);
        
        return apiConfig;

	}

	private void transformConnectorConfiguration(ConfigurationProperties configProps,
			PrismContainer<?> configurationPropertiesContainer, String connectorConfNs)
			throws ConfigurationException, SchemaException {

		if (configurationPropertiesContainer == null || configurationPropertiesContainer.getValue() == null) {
			throw new SchemaException("No configuration properties container in " + connectorType);
		}

		int numConfingProperties = 0;
		List<QName> wrongNamespaceProperties = new ArrayList<>();

		for (PrismProperty prismProperty : configurationPropertiesContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getElementName();

			// All the elements must be in a connector instance
			// namespace.
			if (propertyQName.getNamespaceURI() == null
					|| !propertyQName.getNamespaceURI().equals(connectorConfNs)) {
				LOGGER.warn("Found element with a wrong namespace ({}) in {}",
						propertyQName.getNamespaceURI(), connectorType);
				wrongNamespaceProperties.add(propertyQName);
			} else {

				numConfingProperties++;

				// Local name of the element is the same as the name
				// of ICF configuration property
				String propertyName = propertyQName.getLocalPart();
				ConfigurationProperty property = configProps.getProperty(propertyName);
				
				if (property == null) {
					throw new ConfigurationException("Unknown configuration property "+propertyName);
				}

				// Check (java) type of ICF configuration property,
				// behave accordingly
				Class<?> type = property.getType();
				if (type.isArray()) {
					property.setValue(convertToIcfArray(prismProperty, type.getComponentType()));
					// property.setValue(prismProperty.getRealValuesArray(type.getComponentType()));
				} else {
					// Single-valued property are easy to convert
					property.setValue(convertToIcfSingle(prismProperty, type));
					// property.setValue(prismProperty.getRealValue(type));
				}
			}
		}
		// empty configuration is OK e.g. when creating a new resource using wizard
		if (numConfingProperties == 0 && !wrongNamespaceProperties.isEmpty()) {
			throw new SchemaException("No configuration properties found. Wrong namespace? (expected: "
					+ connectorConfNs + ", present e.g. " + wrongNamespaceProperties.get(0) + ")");
		}
	}

	private void transformConnectorPoolConfiguration(ObjectPoolConfiguration connectorPoolConfiguration,
			PrismContainer<?> connectorPoolContainer) throws SchemaException {

		if (connectorPoolContainer == null || connectorPoolContainer.getValue() == null) {
			return;
		}

		for (PrismProperty prismProperty : connectorPoolContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getElementName();
			if (propertyQName.getNamespaceURI().equals(SchemaConstants.NS_ICF_CONFIGURATION)) {
				String subelementName = propertyQName.getLocalPart();
				if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS
						.equals(subelementName)) {
					connectorPoolConfiguration.setMinEvictableIdleTimeMillis(parseLong(prismProperty));
				} else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_IDLE
						.equals(subelementName)) {
					connectorPoolConfiguration.setMinIdle(parseInt(prismProperty));
				} else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxIdle(parseInt(prismProperty));
				} else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_OBJECTS
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxObjects(parseInt(prismProperty));
				} else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_WAIT
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxWait(parseLong(prismProperty));
				} else {
					throw new SchemaException(
							"Unexpected element "
									+ propertyQName
									+ " in "
									+ ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
				}
			} else {
				throw new SchemaException(
						"Unexpected element "
								+ propertyQName
								+ " in "
								+ ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
			}
		}
	}

	private void transformConnectorTimeoutsConfiguration(APIConfiguration apiConfig,
			PrismContainer<?> connectorTimeoutsContainer) throws SchemaException {

		if (connectorTimeoutsContainer == null || connectorTimeoutsContainer.getValue() == null) {
			return;
		}

		for (PrismProperty prismProperty : connectorTimeoutsContainer.getValue().getProperties()) {
			QName propertQName = prismProperty.getElementName();

			if (SchemaConstants.NS_ICF_CONFIGURATION.equals(propertQName.getNamespaceURI())) {
				String opName = propertQName.getLocalPart();
				Class<? extends APIOperation> apiOpClass = ConnectorFactoryConnIdImpl.resolveApiOpClass(opName);
				if (apiOpClass != null) {
					apiConfig.setTimeout(apiOpClass, parseInt(prismProperty));
				} else {
					throw new SchemaException("Unknown operation name " + opName + " in "
							+ ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
				}
			}
		}
	}

    private void transformResultsHandlerConfiguration(ResultsHandlerConfiguration resultsHandlerConfiguration,
                                                     PrismContainer<?> resultsHandlerConfigurationContainer) throws SchemaException {

        if (resultsHandlerConfigurationContainer == null || resultsHandlerConfigurationContainer.getValue() == null) {
            return;
        }

        for (PrismProperty prismProperty : resultsHandlerConfigurationContainer.getValue().getProperties()) {
            QName propertyQName = prismProperty.getElementName();
            if (propertyQName.getNamespaceURI().equals(SchemaConstants.NS_ICF_CONFIGURATION)) {
                String subelementName = propertyQName.getLocalPart();
                if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableNormalizingResultsHandler(parseBoolean(prismProperty));
                } else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableFilteredResultsHandler(parseBoolean(prismProperty));
                } else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setFilteredResultsHandlerInValidationMode(parseBoolean(prismProperty));
                } else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableCaseInsensitiveFilter(parseBoolean(prismProperty));
                } else if (ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableAttributesToGetSearchResultsHandler(parseBoolean(prismProperty));
                } else {
                    throw new SchemaException(
                            "Unexpected element "
                                    + propertyQName
                                    + " in "
                                    + ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
                }
            } else {
                throw new SchemaException(
                        "Unexpected element "
                                + propertyQName
                                + " in "
                                + ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
            }
        }
    }

    
    private int parseInt(PrismProperty<?> prop) {
		return prop.getRealValue(Integer.class);
	}

	private long parseLong(PrismProperty<?> prop) {
		Object realValue = prop.getRealValue();
		if (realValue instanceof Long) {
			return (Long) realValue;
		} else if (realValue instanceof Integer) {
			return ((Integer) realValue);
		} else {
			throw new IllegalArgumentException("Cannot convert " + realValue.getClass() + " to long");
		}
	}

    private boolean parseBoolean(PrismProperty<?> prop) {
        return prop.getRealValue(Boolean.class);
    }

    private Object convertToIcfSingle(PrismProperty<?> configProperty, Class<?> expectedType)
			throws ConfigurationException {
		if (configProperty == null) {
			return null;
		}
		PrismPropertyValue<?> pval = configProperty.getValue();
		return convertToIcf(pval, expectedType);
	}

	private Object[] convertToIcfArray(PrismProperty prismProperty, Class<?> componentType)
			throws ConfigurationException {
		List<PrismPropertyValue> values = prismProperty.getValues();
		Object valuesArrary = Array.newInstance(componentType, values.size());
		for (int j = 0; j < values.size(); ++j) {
			Object icfValue = convertToIcf(values.get(j), componentType);
			Array.set(valuesArrary, j, icfValue);
		}
		return (Object[]) valuesArrary;
	}
	
	private Object convertToIcf(PrismPropertyValue<?> pval, Class<?> expectedType) throws ConfigurationException {
		Object midPointRealValue = pval.getValue();
		if (expectedType.equals(GuardedString.class)) {
			// Guarded string is a special ICF beast
			// The value must be ProtectedStringType
			if (midPointRealValue instanceof ProtectedStringType) {
				ProtectedStringType ps = (ProtectedStringType) pval.getValue();
				return ConnIdUtil.toGuardedString(ps, pval.getParent().getElementName().getLocalPart(), protector);
			} else {
				throw new ConfigurationException(
						"Expected protected string as value of configuration property "
								+ pval.getParent().getElementName().getLocalPart() + " but got "
								+ midPointRealValue.getClass());
			}

		} else if (expectedType.equals(GuardedByteArray.class)) {
			// Guarded string is a special ICF beast
			// TODO
//			return new GuardedByteArray(Base64.decodeBase64((ProtectedByteArrayType) pval.getValue()));
			return new GuardedByteArray(((ProtectedByteArrayType) pval.getValue()).getClearBytes());
		} else if (midPointRealValue instanceof PolyString) {
			return ((PolyString)midPointRealValue).getOrig();
		} else if (midPointRealValue instanceof PolyStringType) {
			return ((PolyStringType)midPointRealValue).getOrig();
		} else if (expectedType.equals(File.class) && midPointRealValue instanceof String) {
			return new File((String)midPointRealValue);
		} else if (expectedType.equals(String.class) && midPointRealValue instanceof ProtectedStringType) {
			try {
				return protector.decryptString((ProtectedStringType)midPointRealValue);
			} catch (EncryptionException e) {
				throw new ConfigurationException(e);
			}
		} else {
			return midPointRealValue;
		}
	}


}

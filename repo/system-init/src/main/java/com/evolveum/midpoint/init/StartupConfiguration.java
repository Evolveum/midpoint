/*
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
package com.evolveum.midpoint.init;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.configuration.*;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class StartupConfiguration implements MidpointConfiguration {
    
    private static final String USER_HOME_SYSTEM_PROPERTY_NAME = "user.home";
    private static final String MIDPOINT_HOME_SYSTEM_PROPERTY_NAME = "midpoint.home";
    private static final String MIDPOINT_CONFIGURATION_SECTION = "midpoint";
    private static final String SAFE_MODE = "safeMode";
    private static final String PROFILING_ENABLED = "profilingEnabled";

    private static final String DEFAULT_CONFIG_FILE_NAME = "config.xml";
	private static final String LOGBACK_CONFIG_FILENAME = "logback.xml";
	private static final String LOGBACK_EXTRA_CONFIG_FILENAME = "logback-extra.xml";
	
	private static final Trace LOGGER = TraceManager.getTrace(StartupConfiguration.class);
	private static final Trace LOGGER_WELCOME = TraceManager.getTrace("com.evolveum.midpoint.init.welcome");

    private CompositeConfiguration config = null;
    private Document xmlConfigAsDocument = null;        // just in case when we need to access original XML document
    private String midPointHomePath = null;
    private String configFilename = null;

    /**
     * Default constructor
     */
    public StartupConfiguration() {
        this.configFilename = DEFAULT_CONFIG_FILE_NAME;
    }

    /**
     * Alternative constructor for use in the tests.
     */
    public StartupConfiguration(String midPointHome, String configFilename) {
    	this.midPointHomePath = midPointHome;
        this.configFilename = configFilename;
    }

    /**
     * Get current configuration file name
     */
    public String getConfigFilename() {
        return this.configFilename;
    }

    /**
     * Set configuration filename
     */
    public void setConfigFilename(String configFilename) {
        this.configFilename = configFilename;
    }

    @Override
    public String getMidpointHome() {
        return midPointHomePath;
    }

    @Override
    public Configuration getConfiguration(String componentName) {
        if (null == componentName) {
            throw new IllegalArgumentException("NULL argument");
        }
        Configuration sub = config.subset(componentName);
        // Insert replacement for relative path to midpoint.home else clean
        // replace
        if (getMidpointHome() != null) {
            sub.addProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME, getMidpointHome());
        } else {
            @SuppressWarnings("unchecked")
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME_SYSTEM_PROPERTY_NAME + "}/", ""));
                sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME_SYSTEM_PROPERTY_NAME + "}", ""));
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Configuration for {} :", componentName);
            @SuppressWarnings("unchecked")
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                LOGGER.debug("    {} = {}", key, sub.getProperty(key));
            }
        }
        return sub;
    }

    /**
     * Initialize system configuration
     */
    public void init() {
              
        if (midPointHomePath == null) {
        	
	        if (System.getProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME) == null || System.getProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME).isEmpty()) {
	            LOGGER.info("{} system property is not set, using default configuration", MIDPOINT_HOME_SYSTEM_PROPERTY_NAME);

	            midPointHomePath = System.getProperty(USER_HOME_SYSTEM_PROPERTY_NAME);
				if (!midPointHomePath.endsWith("/")) {
					midPointHomePath += "/";
				}
				midPointHomePath += "midpoint";
				
			} else {
	
				midPointHomePath = System.getProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME);
			}
        }
        
        if (midPointHomePath != null) {
                //Fix missing last slash in path
                if (!midPointHomePath.endsWith("/")) {
                	midPointHomePath = midPointHomePath + "/";
                }
        }

        // This is not really good practice. But some components such as reports rely on well-formatted midpoint.home system property.
        System.setProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME, midPointHomePath);
        
        File midpointHome = new File(midPointHomePath);
        
        setupInitialLogging(midpointHome);

        loadConfiguration(midpointHome);

        if (isSafeMode()) {
            LOGGER.info("Safe mode is ON; setting tolerateUndeclaredPrefixes to TRUE");
            QNameUtil.setTolerateUndeclaredPrefixes(true);
        }
        
        // Make sure that this is called very early in the startup sequence.
        // This is needed to properly initialize the resources
        // (the "org/apache/xml/security/resource/xmlsecurity" resource bundle error)
        WSSConfig.init();
    }

    /**
     * Loading logic
     */
    private void loadConfiguration(File midpointHome) {
        if (config != null) {
            config.clear();
        } else {
            config = new CompositeConfiguration();
        }

        DocumentBuilder documentBuilder = DOMUtil.createDocumentBuilder();          // we need namespace-aware document builder (see GeneralChangeProcessor.java)

        if (midpointHome != null) {
        	
            ApplicationHomeSetup ah = new ApplicationHomeSetup();
            ah.init(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME);

        	File configFile = new File(midpointHome, this.getConfigFilename());
        	System.out.println("Loading midPoint configuration from file "+configFile);
        	LOGGER.info("Loading midPoint configuration from file {}", configFile);
        	try {
                if (!configFile.exists()) {
                    LOGGER.warn("Configuration file {} does not exists. Need to do extraction ...", configFile);
                    boolean success = ClassPathUtil.extractFileFromClassPath(this.getConfigFilename(), configFile.getPath());
                    if (!success || !configFile.exists()) {
                    	String message = "Unable to extract configuration file " + this.getConfigFilename() + " from classpath";
                        LOGGER.error(message);
                        System.out.println(message);
                        throw new SystemException(message);
                    }
                }
                //Load and parse properties
                config.addProperty(MIDPOINT_HOME_SYSTEM_PROPERTY_NAME, midPointHomePath);
                createXmlConfiguration(documentBuilder, configFile.getPath());
            } catch (ConfigurationException e) {
                String message = "Unable to read configuration file [" + configFile + "]: " + e.getMessage();
                LOGGER.error(message);
                System.out.println(message);
                throw new SystemException(message, e);      // there's no point in continuing with midpoint initialization
            }

        } else {
            // Load from current directory
            try {
                createXmlConfiguration(documentBuilder, this.getConfigFilename());
            } catch (ConfigurationException e) {
                String message = "Unable to read configuration file [" + this.getConfigFilename() + "]: " + e.getMessage();
                LOGGER.error(message);
                System.out.println(message);
                throw new SystemException(message, e);
            }
        }
    }
    
	private void setupInitialLogging(File midpointHome) {
		File logbackConfigFile = new File(midpointHome, LOGBACK_CONFIG_FILENAME);
		boolean clear = false;
		if (logbackConfigFile.exists()) {
			clear = true;
		} else {
			logbackConfigFile = new File(midpointHome, LOGBACK_EXTRA_CONFIG_FILENAME);
			if (!logbackConfigFile.exists()) {
				return;
			}
		}
		LOGGER.info("Loading logging configuration from {} ({})", logbackConfigFile,
				clear?"clearing default configuration":"extending defalt configuration");
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		if (clear) {
			context.reset();
		}
		try {
		  JoranConfigurator configurator = new JoranConfigurator();
		  configurator.setContext(context);
		  configurator.doConfigure(logbackConfigFile);
		} catch (Exception e) {
			// This will logged by defalt logging configuration
			LOGGER.error("Error loading additional logging configuration: {}", e.getMessage(), e);
			// If normal logging fail make sure it is logged by web container
			e.printStackTrace();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private void createXmlConfiguration(DocumentBuilder documentBuilder, String filename) throws ConfigurationException {
        XMLConfiguration xmlConfig = new XMLConfiguration();
        xmlConfig.setDocumentBuilder(documentBuilder);
        xmlConfig.setFileName(filename);
        xmlConfig.load();
        config.addConfiguration(xmlConfig);

        xmlConfigAsDocument = DOMUtil.parseFile(filename);
    }

    @Override
    public Document getXmlConfigAsDocument() {
        return xmlConfigAsDocument;
    }

    @Override
    public boolean isSafeMode() {
        Configuration c = getConfiguration(MIDPOINT_CONFIGURATION_SECTION);
        if (c == null) {
            return false;           // should not occur
        }
        return c.getBoolean(SAFE_MODE, false);
    }

	@Override
	public boolean isProfilingEnabled() {
		Configuration c = getConfiguration(MIDPOINT_CONFIGURATION_SECTION);
		if (c == null) {
			return false;           // should not occur
		}
		return c.getBoolean(PROFILING_ENABLED, false);
	}

    @Override
    public String toString() {
        @SuppressWarnings("unchecked")
        Iterator<String> i = config.getKeys();
        StringBuilder sb = new StringBuilder();
        while (i.hasNext()) {
            String key = i.next();
            sb.append(key);
            sb.append(" = ");
            sb.append(config.getString(key));
            sb.append("; ");
        }
        return sb.toString();
    }
}

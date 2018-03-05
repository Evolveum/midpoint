/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.common.configuration.api;

/**
 * @author mamut
 */
import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;

public interface MidpointConfiguration {
	
	String SYSTEM_CONFIGURATION_SECTION = "midpoint.system";
	
	String getMidpointHome();
	
	/**
	 * get configuration for symbolic name of the component from configuration
	 * subsytem.
	 * 
	 * 
	 * @param componetID
	 *            name of the component Samples of names: <li>
	 *            <ul>
	 *            repository -> midpoint.repository
	 *            </ul>
	 *            <ul>
	 *            provisioning -> midpoint.provisioning
	 *            </ul>
	 *            <ul>
	 *            model -> midpoint.model
	 *            </ul>
	 *            </li>
	 * 
	 * @return Configuration object {@link http
	 *         ://commons.apache.org/configuration
	 *         /apidocs/org/apache/commons/configuration/Configuration.html}
	 *         Sample how to get config value:
	 *         {@code config.getInt("port", 1234);}
	 */
	public Configuration getConfiguration(String componetID);

    /**
     * If there's a need to retrieve original midpoint config as XML.
     *
     * XMLConfiguration does not work well - sometimes it omits values from the document.
     * So it's best to parse the config ourselves.
     *
     * @return
     */
    Document getXmlConfigAsDocument();

    boolean isSafeMode();

	boolean isProfilingEnabled();
}

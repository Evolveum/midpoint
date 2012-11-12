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
 */
package com.evolveum.icf.dummy.connector;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Test Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DummyConfiguration extends AbstractConfiguration {

    private static final Log log = Log.getLog(DummyConfiguration.class);

    private String instanceId;
    private String fakeName;
    
    @ConfigurationProperty(displayMessageKey = "UI_INSTANCE_ID",
    		helpMessageKey = "UI_INSTANCE_ID_HELP")
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String config) {
        this.instanceId = config;
    }

    @ConfigurationProperty(displayMessageKey = "UI_FAKE_NAME",
    		helpMessageKey = "UI_FAKE_NAME_HELP")
    public String getFakeName() {
        return fakeName;
    }

    public void setFakeName(String config) {
        this.fakeName = config;
    }
    	
	/**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        log.info("begin");

        //TODO: validate configuration

        log.info("end");
    }

	
}
 
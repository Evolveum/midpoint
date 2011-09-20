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
package com.evolveum.icf.dummy;

import org.identityconnectors.common.logging.Log;
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

    private static final Log log = Log.getLog(TestConfiguration.class);
    // Example of exposed configuration properties.
    private String config;

    @ConfigurationProperty(displayMessageKey = "UI_CONFIG",
    helpMessageKey = "UI_CONFIG_HELP")
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        log.info("begin");

        //TODO: validate ocnfiguration

        log.info("end");
    }
}

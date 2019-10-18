/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.configuration.api;

import org.apache.commons.configuration.Configuration;

public interface RuntimeConfiguration {

    /**
     * Return symbolic name of the component in configuration subsytem.
     *  Samples:
     *  <li>
     *      <ul>repository -> midpoint.repository</ul>
     *      <ul>provisioning  -> midpoint.provisioning</ul>
     *      <ul>model -> midpoint.model</ul>
     *  </li>
     * @return    String name of component
     */

    public String getComponentId();

    /**
     * Returns current component configuration in commons configuration structure
     * {@link http://commons.apache.org/configuration/apidocs/org/apache/commons/configuration/Configuration.html}
     *
     * <p>
     * Example  of structure for repository:
     * </p>
     * <p>
     * {@code
     *
     *        Configuration config =  new BaseConfiguration();
     *        config.setProperty("host", "localhost");
     *         config.setProperty("port" , 12345);
     *         return config;
     * }
     * </p>
     *
     * Note: current configuration can be obtained only on fully initialized objects. If called on not initialized objects, then it can end with undefined behavior
     *
     * @return    Commons configuration
     */
    public Configuration getCurrentConfiguration();
}

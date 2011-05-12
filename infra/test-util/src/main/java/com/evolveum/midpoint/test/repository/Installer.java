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

package com.evolveum.midpoint.test.repository;

import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author
 */
public class Installer implements BundleActivator {

    private ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {

        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("osgi.remote.interfaces", "*");
        props.put("osgi.remote.configuration.type", "pojo");
        props.put("osgi.remote.configuration.pojo.address", "http://localhost:9000/RepositoryService");
        registration = context.registerService(RepositoryPortType.class.getName(), new RepositoryService(), props);


        System.out.println("OSGi Repository Test Bundle: Started");
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        System.out.println("OSGi Repository Test Bundle: Stopped");
    }
}

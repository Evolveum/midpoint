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

package com.evolveum.midpoint.model.test.util.mock;

import org.mockito.Mockito;

import com.evolveum.midpoint.provisioning.synchronization.SynchronizationProcessManager;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 *
 * @author lazyman
 */
public class MockFactory {

    public static ProvisioningPortType createProvisioningPortType() {
        return Mockito.mock(ProvisioningPortType.class);
    }

    public static RepositoryPortType createRepositoryPortType() {
        return Mockito.mock(RepositoryPortType.class);
    }
    
    public static SynchronizationProcessManager createSyncProcesManager() {
    	return Mockito.mock(SynchronizationProcessManager.class);
    }
}

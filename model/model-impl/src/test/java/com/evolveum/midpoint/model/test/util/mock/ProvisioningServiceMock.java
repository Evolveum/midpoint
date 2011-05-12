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

import com.evolveum.midpoint.provisioning.service.ProvisioningService;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class ProvisioningServiceMock extends ProvisioningService {

    private ResourceAccessInterface rai;

    public void setRai(ResourceAccessInterface rai) {
        this.rai = rai;
    }

    @Override
    protected ResourceAccessInterface getResourceAccessInterface(String resourceOID, ResourceType inputResource) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
        return rai;
    }

    @Override
    protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
        return rai;
    }
}

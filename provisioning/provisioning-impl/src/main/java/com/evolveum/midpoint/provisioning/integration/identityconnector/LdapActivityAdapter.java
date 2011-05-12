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

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.Set;
import javax.xml.namespace.QName;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;

/**
 * Adapter interface to add more functionality without commit to ICF.
 * 
 * @author elek
 */
public class LdapActivityAdapter {
 
    public void preConvertAttributes(ConnectorFacade connector, ResourceObject resourceObject, Set<Attribute> attributes) {
        QName activationName = new QName(SchemaConstants.NS_C,"activation");
        //TODO the following part is under development. Currently the tests of provisioning is failed
        //if the implementations is turned on. It should be fixed

//        //TODO check if it's an OpenDJ server and not an other ldap server
//        ResourceAttribute activationAttr = resourceObject.getValue(activationName);
//        if (activationAttr!=null){
//            ActivationType act = activationAttr.getSingleJavaValue(ActivationType.class);
//            //. If this attribute exists in the user's entry with any value other than "false", then the account will be disabled. I
//            attributes.add(AttributeBuilder.build("ds-pwp-account-disabled", act.isEnabled() ? "false" : "true"));
//        }
//        resourceObject.removeValue(activationName);
    }

}

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

package com.evolveum.midpoint.provisioning.conversion;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import javax.xml.namespace.QName;
import junit.framework.Assert;




import org.junit.Test;
import org.w3c.dom.Node;

/**
 *
 * @author elek
 */
public class ActivationConverterTest {

    public ActivationConverterTest() {
    }

    @Test
    public void testConversion() {
        ActivationType type = new ActivationType();
        type.setEnabled(Boolean.FALSE);

        ActivationConverter converter = new ActivationConverter();
        Node node = converter.convertToXML(null, type);
        System.out.println(node.getLocalName());
        ActivationType n = (ActivationType) converter.convertToJava(node);

        Assert.assertFalse(n.isEnabled());
    }
}

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

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml"})
public class SchemaHandlingXPath2Test {

	@Autowired
	SchemaHandling schemaHandling;

	@Test
	public void testIfThenElseSupportInOutboundSection() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath2.xml"));
		JAXBElement<UserType> userJaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-xpath2.xml"));
		ResourceObjectShadowType appliedAccountShadow = schemaHandling.applyOutboundSchemaHandlingOnAccount(
				userJaxb.getValue(), accountJaxb.getValue(), accountJaxb.getValue().getResource());
		assertEquals("__NAME__", appliedAccountShadow.getAttributes().getAny().get(0).getLocalName());
		assertEquals("James Bond 007", appliedAccountShadow.getAttributes().getAny().get(0).getTextContent());
	}
}
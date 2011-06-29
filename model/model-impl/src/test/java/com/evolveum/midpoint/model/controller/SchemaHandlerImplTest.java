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
package com.evolveum.midpoint.model.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class SchemaHandlerImplTest {

	@Autowired(required=true)
	private transient SchemaHandler handler;

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullUser() throws Exception {
		handler.processInboundHandling(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullResourceObjectShadow() throws Exception {
		handler.processInboundHandling(new UserType(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullResult() throws Exception {
		handler.processInboundHandling(new UserType(), new ResourceObjectShadowType(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullUser() throws Exception {
		handler.processOutboundHandling(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullResourceObjectShadow() throws Exception {
		handler.processOutboundHandling(new UserType(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullResult() throws Exception {
		handler.processOutboundHandling(new UserType(), new ResourceObjectShadowType(), null);
	}
}

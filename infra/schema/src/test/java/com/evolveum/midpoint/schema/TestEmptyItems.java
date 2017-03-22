/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.collections4.CollectionUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests for mistakenly adding new empty items (PP, PR, PC) into PCVs.
 *
 * @author mederly
 */
public class TestEmptyItems {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@Test
	public void testEmptyItemsOnGet() throws Exception {
		System.out.println("===[ testEmptyItemsOnGet ]===");

		// GIVEN
		UserType user = new UserType(getPrismContext());
		System.out.println("User before:\n" + user.asPrismObject().debugDump());
		assertEquals("Wrong # of user sub-items before 'get' operations", 0, CollectionUtils.emptyIfNull(user.asPrismContainerValue().getItems()).size());

		// WHEN
		user.getAssignment();
		user.getLinkRef();
		user.getEmployeeType();

		// THEN
		System.out.println("User after:\n" + user.asPrismObject().debugDump());
		assertEquals("Wrong # of user sub-items after 'get' operations", 0, CollectionUtils.emptyIfNull(user.asPrismContainerValue().getItems()).size());
	}

	@Test
	public void testEmptyItemsOnParse() throws Exception {
		System.out.println("===[ testEmptyItemsOnParse ]===");

		// GIVEN
		UserType user = new UserType(getPrismContext());
		user.setName(PolyStringType.fromOrig("jack"));
		System.out.println("User before:\n" + user.asPrismObject().debugDump());
		assertEquals("Wrong # of user sub-items before 'get' operations", 1, CollectionUtils.emptyIfNull(user.asPrismContainerValue().getItems()).size());

		// WHEN
		String xml = getPrismContext().xmlSerializer().serialize(user.asPrismObject());
		user = (UserType) getPrismContext().parserFor(xml).parse().asObjectable();

		// THEN
		System.out.println("User after:\n" + user.asPrismObject().debugDump());
		assertEquals("Wrong # of user sub-items after 'get' operations", 1, CollectionUtils.emptyIfNull(user.asPrismContainerValue().getItems()).size());
	}

}

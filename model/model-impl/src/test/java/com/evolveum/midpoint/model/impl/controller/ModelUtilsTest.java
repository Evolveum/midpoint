/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ModelUtilsTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller");
	@Autowired(required = true)
	private Protector protector;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void validatePagingNull() {
		ModelUtils.validatePaging(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadOffsetAttribute() {
		ModelUtils.validatePaging(ObjectPaging.createPaging(-5, 10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadMaxAttribute() {
		ModelUtils.validatePaging(ObjectPaging
				.createPaging(5, -10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

	@Test
	public void validatePagingGood() {
		ModelUtils
				.validatePaging(ObjectPaging
						.createPaging(5, 10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

}

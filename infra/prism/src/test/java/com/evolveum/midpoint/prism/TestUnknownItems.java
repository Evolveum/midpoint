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
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.constructInitializedPrismContext;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * See MID-3249.
 *
 * @author mederly
 */
public class TestUnknownItems {

	public static final String TEST_DIR = "src/test/resources/common/xml";

	public static final File WRONG_ITEM_FILE = new File(TEST_DIR + "/user-wrong-item.xml");
	public static final File WRONG_NAMESPACE_FILE = new File(TEST_DIR + "/user-wrong-namespace.xml");

	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}
	
	@Test(expectedExceptions = SchemaException.class)
	public void test010ParseWrongItemStrict() throws Exception {
		final String TEST_NAME = "testParseWrongItemStrict";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		// WHEN+THEN
		try {
			prismContext.parseObject(WRONG_ITEM_FILE);
		} catch (SchemaException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test
	public void test020ParseWrongItemCompat() throws Exception {
		final String TEST_NAME = "testParseWrongItemCompat";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(WRONG_ITEM_FILE).compat().parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);
	}

	// Currently we simply mark the unknown value as raw.
	// This might or might not be correct.
	// (We should probably throw SchemaException instead.)
	// TODO discuss this
	@Test(enabled = false, expectedExceptions = SchemaException.class)
	public void test110ParseWrongNamespaceStrict() throws Exception {
		final String TEST_NAME = "test110ParseWrongNamespaceStrict";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN+THEN
		PrismObject<UserType> user = prismContext.parseObject(WRONG_NAMESPACE_FILE);
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);
	}

	@Test
	public void test120ParseWrongNamespaceCompat() throws Exception {
		final String TEST_NAME = "test120ParseWrongNamespaceCompat";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(WRONG_NAMESPACE_FILE).compat().parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);
	}


}

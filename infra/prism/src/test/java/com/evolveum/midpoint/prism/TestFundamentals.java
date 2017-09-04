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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 *
 */
public class TestFundamentals {

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}

	@Test
    public void testPrismValueContainsRealValue() throws Exception {
		System.out.println("\n\n===[ testPrismValueContainsRealValue ]===\n");
		// GIVEN
		PrismPropertyValue<String> valFoo1 = new PrismPropertyValue<String>("foo");
		PrismPropertyValue<String> valBar1 = new PrismPropertyValue<String>("bar");
		valBar1.setOriginType(OriginType.OUTBOUND);
		Collection<PrismValue> collection = new ArrayList<PrismValue>();
		collection.add(valFoo1);
		collection.add(valBar1);

		PrismPropertyValue<String> valFoo2 = new PrismPropertyValue<String>("foo");
		PrismPropertyValue<String> valFoo3 = new PrismPropertyValue<String>("foo");
		valFoo3.setOriginType(OriginType.OUTBOUND);

		PrismPropertyValue<String> valBar2 = new PrismPropertyValue<String>("bar");
		valBar2.setOriginType(OriginType.OUTBOUND);
		PrismPropertyValue<String> valBar3 = new PrismPropertyValue<String>("bar");

		PrismPropertyValue<String> valBaz = new PrismPropertyValue<String>("baz");

		// WHEN - THEN
		assert PrismValue.containsRealValue(collection, valFoo1);
		assert PrismValue.containsRealValue(collection, valBar1);
		assert PrismValue.containsRealValue(collection, valFoo2);
		assert PrismValue.containsRealValue(collection, valBar2);
		assert PrismValue.containsRealValue(collection, valFoo3);
		assert PrismValue.containsRealValue(collection, valBar3);
		assert !PrismValue.containsRealValue(collection, valBaz);
    }

	@Test
	public void testRawTypeClone() throws Exception {
		System.out.println("\n\n===[ testRawTypeClone ]===\n");
		// GIVEN
		QName typeQName = new QName("abcdef");
		MapXNode mapXNode = new MapXNode();
		mapXNode.setTypeQName(typeQName);
		RawType rawType = new RawType(mapXNode, PrismTestUtil.getPrismContext());

		// WHEN
		RawType rawTypeClone = rawType.clone();

		// THEN
		assertEquals("Wrong or missing type QName", typeQName, rawTypeClone.getXnode().getTypeQName());
	}

}

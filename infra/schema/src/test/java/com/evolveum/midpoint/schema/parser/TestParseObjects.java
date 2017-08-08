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

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
public class TestParseObjects extends AbstractParserTest {

	@Override
	protected File getFile() {
		return getFile(OBJECTS_FILE_BASENAME);
	}

	@Test
	public void testRoundTrip() throws Exception {
		displayTestTitle("testRoundTrip");
		PrismContext prismContext = getPrismContext();

		PrismParser parser = prismContext.parserFor(getFile());
		List<PrismObject<?>> objects = parser.parseObjects();

		System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

		assertEquals("Wrong # of objects", 3, objects.size());
		assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
		assertEquals("Wrong class of object 2", UserType.class, objects.get(1).asObjectable().getClass());
		assertEquals("Wrong class of object 2", RoleType.class, objects.get(2).asObjectable().getClass());

		PrismSerializer<String> serializer = prismContext.serializerFor(language);
		String serialized = serializer.serializeObjects(objects, SchemaConstants.C_OBJECTS);
		System.out.println("Objects as re-serialized:\n" + serialized);

		System.out.println("Re-serialized to XML:\n" + prismContext.xmlSerializer().serializeObjects(objects, SchemaConstants.C_OBJECTS));
		System.out.println("Re-serialized to JSON:\n" + prismContext.jsonSerializer().serializeObjects(objects, SchemaConstants.C_OBJECTS));
		System.out.println("Re-serialized to YAML:\n" + prismContext.yamlSerializer().serializeObjects(objects, SchemaConstants.C_OBJECTS));

		List<PrismObject<?>> objectsReparsed = prismContext.parserFor(serialized).parseObjects();
		assertEquals("Reparsed objects are different from original ones", objects, objectsReparsed);
	}

}

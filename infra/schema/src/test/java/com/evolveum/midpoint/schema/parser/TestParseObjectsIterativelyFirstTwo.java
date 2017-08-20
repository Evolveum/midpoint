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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.OBJECTS_FILE_BASENAME;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
public class TestParseObjectsIterativelyFirstTwo extends AbstractParserTest {

	@Override
	protected File getFile() {
		return getFile(OBJECTS_FILE_BASENAME);
	}

	@Test
	public void testRoundTrip() throws Exception {
		displayTestTitle("testRoundTrip");
		PrismContext prismContext = getPrismContext();

		PrismParser parser = prismContext.parserFor(getFile());
		List<PrismObject<?>> objects = new ArrayList<>();
		parser.parseObjectsIteratively(new PrismParser.ObjectHandler() {
			@Override
			public boolean handleData(PrismObject<?> object) {
				objects.add(object);
				return objects.size() != 2;
			}

			@Override
			public boolean handleError(Throwable t) {
				throw new AssertionError("unexpected handleError call");
			}
		});

		System.out.println("Objects as parsed: " + DebugUtil.debugDump(objects));

		assertEquals("Wrong # of objects", 2, objects.size());
		assertEquals("Wrong class of object 1", UserType.class, objects.get(0).asObjectable().getClass());
		assertEquals("Wrong class of object 2", UserType.class, objects.get(1).asObjectable().getClass());

		List<PrismObject<?>> objectsStandard = prismContext.parserFor(getFile()).parseObjects();
		objectsStandard.remove(2);
		assertEquals("Objects are different if read in a standard way", objectsStandard, objects);
	}

}

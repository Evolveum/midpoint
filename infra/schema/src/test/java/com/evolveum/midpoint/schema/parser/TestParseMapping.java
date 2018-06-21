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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author mederly
 *
 */
@SuppressWarnings("Convert2MethodRef")
public class TestParseMapping extends AbstractContainerValueParserTest<MappingType> {

	@Override
	protected File getFile() {
		return getFile("mapping");
	}

	@Test
	public void testParseFile() throws Exception {
		displayTestTitle("testParseFile");
		processParsings(null, null);
	}
	
	@Test
	public void testParseRoundTrip() throws Exception{
		displayTestTitle("testParseRoundTrip");

		//processParsings(v -> getPrismContext().serializerFor(language).serialize(v));													// no item name nor definition => cannot serialize
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");		// misleading item name
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeRealValue(v.asContainerable()), "s3");
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	private void processParsings(SerializingFunction<PrismContainerValue<MappingType>> serializer, String serId) throws Exception {
		processParsings(MappingType.class, null, MappingType.COMPLEX_TYPE, null, serializer, serId);
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<MappingType> value)
			throws SchemaException {
		// TODO 
		
	}

	@Override
	protected boolean isContainer() {
		return false;
	}

}

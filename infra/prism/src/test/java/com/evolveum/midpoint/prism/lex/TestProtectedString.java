/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismContextImpl;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.TestProtector;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.xml.security.encryption.XMLCipher;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestProtectedString {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
    public void testParseProtectedStringEncrypted() throws Exception {
		final String TEST_NAME = "testParseProtectedStringEncrypted";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
        Protector protector = PrismInternalTestUtil.createProtector(XMLCipher.AES_128);
        ProtectedStringType protectedStringType = protector.encryptString("salalala");

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        MapXNode protectedStringTypeXNode = ((PrismContextImpl) prismContext).getBeanMarshaller().marshalProtectedDataType(protectedStringType, null);
        System.out.println("Protected string type XNode: " + protectedStringTypeXNode.debugDump());

        // THEN
        ProtectedStringType unmarshalled = new ProtectedStringType();
        XNodeProcessorUtil.parseProtectedType(unmarshalled, protectedStringTypeXNode, prismContext, ParsingContext.createDefault());
        System.out.println("Unmarshalled value: " + unmarshalled);
        assertEquals("Unmarshalled value differs from the original", protectedStringType, unmarshalled);
    }
	
	@Test
    public void testParseProtectedStringHashed() throws Exception {
		final String TEST_NAME = "testParseProtectedStringHashed";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		ProtectedStringType protectedStringType = new ProtectedStringType();
		protectedStringType.setClearValue("blabla");
        Protector protector = PrismInternalTestUtil.createProtector(XMLCipher.AES_128);
        protector.hash(protectedStringType);

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        MapXNode protectedStringTypeXNode = ((PrismContextImpl) prismContext).getBeanMarshaller().marshalProtectedDataType(protectedStringType, null);
        System.out.println("Protected string type XNode: " + protectedStringTypeXNode.debugDump());

        // THEN
        ProtectedStringType unmarshalled = new ProtectedStringType();
        XNodeProcessorUtil.parseProtectedType(unmarshalled, protectedStringTypeXNode, prismContext, ParsingContext.createDefault());
        System.out.println("Unmarshalled value: " + unmarshalled);
        assertEquals("Unmarshalled value differs from the original", protectedStringType, unmarshalled);
    }
}

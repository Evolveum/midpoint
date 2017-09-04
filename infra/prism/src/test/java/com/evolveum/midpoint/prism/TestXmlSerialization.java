/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestXmlSerialization {

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}

	@Test
    public void testHandlingInvalidChars() throws Exception {
		final String TEST_NAME = "testHandlingInvalidChars";
		displayTestTitle(TEST_NAME);

		// GIVEN

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        PrimitiveXNode<String> valOkNode = new PrimitiveXNode<>("abcdef");
        PrimitiveXNode<String> valWrongNode = new PrimitiveXNode<>("abc\1def");

        // THEN

		final DomLexicalProcessor domLexicalProcessor = ((PrismContextImpl) prismContext).getParserDom();
		String ok = domLexicalProcessor.write(valOkNode, new QName("ok"), null);
        System.out.println("correct value serialized to: " + ok);
        assertEquals("Wrong serialization", "<ok>abcdef</ok>", ok.trim());         // todo make this less brittle with regards to serialization style

        try {
            String wrong = domLexicalProcessor.write(valWrongNode, new QName("wrong"), null);
            System.out.println("wrong value serialized to: " + wrong);
            assert false : "Wrong value serialization had to fail but it didn't!";
        } catch (RuntimeException e) {
            System.out.println("wrong value was not serialized (as expected): " + e);
            assertTrue(e.getMessage().contains("Invalid character"), "Didn't get expected error message");
        }
    }
}

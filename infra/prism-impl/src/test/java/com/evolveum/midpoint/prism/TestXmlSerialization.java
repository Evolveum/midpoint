/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestXmlSerialization extends AbstractPrismTest {

    @Test
    public void testHandlingInvalidChars() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();

        // WHEN

        PrimitiveXNodeImpl<String> valOkNode = new PrimitiveXNodeImpl<>("abcdef");
        PrimitiveXNodeImpl<String> valWrongNode = new PrimitiveXNodeImpl<>("abc\1def");

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

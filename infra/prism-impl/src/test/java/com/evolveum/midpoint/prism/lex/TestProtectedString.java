/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.createDefaultParsingContext;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
public class TestProtectedString extends AbstractPrismTest {

    @Test
    public void testParseProtectedStringEncrypted() throws Exception {
        // GIVEN
        Protector protector = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC);
        ProtectedStringType protectedStringType = protector.encryptString("salalala");

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        MapXNodeImpl protectedStringTypeXNode = ((PrismContextImpl) prismContext).getBeanMarshaller().marshalProtectedDataType(protectedStringType, null);
        System.out.println("Protected string type XNode: " + protectedStringTypeXNode.debugDump());

        // THEN
        ProtectedStringType unmarshalled = new ProtectedStringType();
        prismContext.hacks().parseProtectedType(unmarshalled, protectedStringTypeXNode, prismContext, createDefaultParsingContext());
        System.out.println("Unmarshalled value: " + unmarshalled);
        assertEquals("Unmarshalled value differs from the original", protectedStringType, unmarshalled);
    }

    @Test
    public void testParseProtectedStringHashed() throws Exception {
        // GIVEN
        ProtectedStringType protectedStringType = new ProtectedStringType();
        protectedStringType.setClearValue("blabla");
        Protector protector = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC);
        protector.hash(protectedStringType);

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        MapXNodeImpl protectedStringTypeXNode = ((PrismContextImpl) prismContext).getBeanMarshaller().marshalProtectedDataType(protectedStringType, null);
        System.out.println("Protected string type XNode: " + protectedStringTypeXNode.debugDump());

        // THEN
        ProtectedStringType unmarshalled = new ProtectedStringType();
        prismContext.hacks().parseProtectedType(unmarshalled, protectedStringTypeXNode, prismContext, createDefaultParsingContext());
        System.out.println("Unmarshalled value: " + unmarshalled);
        assertEquals("Unmarshalled value differs from the original", protectedStringType, unmarshalled);
    }
}

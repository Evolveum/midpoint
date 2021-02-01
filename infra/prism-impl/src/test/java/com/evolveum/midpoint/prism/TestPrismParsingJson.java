/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.lex.json.reader.JsonReader;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.google.common.base.Joiner;

public class TestPrismParsingJson extends TestPrismParsing {

    @Override
    protected String getSubdirName() {
        return "json";
    }

    @Override
    protected String getFilenameSuffix() {
        return "json";
    }

    @Override
    protected String getOutputFormat() {
        return PrismContext.LANG_JSON;
    }

    @Test
    public void testPrismNamespaceContext() throws SchemaException, IOException {
        File jackContext = new File(getCommonSubdir(), "user-jack-object-context.json");
        JsonReader parser = new JsonReader(getPrismContext().getSchemaRegistry());

        @NotNull
        RootXNodeImpl rootXNode = parser.read(new ParserFileSource(jackContext), getPrismContext().getDefaultParsingContext());
        assertNotNull(rootXNode);
        MapXNodeImpl mapNode = rootXNode.toMapXNode();
        MapXNode password = get(MapXNode.class, mapNode, "object", "password");
        assertNotNull(password.toMap().get(ProtectedDataType.F_ENCRYPTED_DATA), "EncryptedData should be qualified");

        PrimitiveXNode<ItemPath> pathNode = get(PrimitiveXNode.class, mapNode, "object", "accountRef", "filter", "equal", "path");


        ItemPathType path = pathNode.getParsedValue(ItemPathType.COMPLEX_TYPE, ItemPathType.class);
        assertNotNull(path);
        assertEquals(path.getItemPath().firstName(), UserType.F_NAME);
    }

    private static final <E extends T,T extends XNode> E get(Class<T> type, MapXNode root, String... path) {
        XNode current = root;
        for(String cmp : path) {
            if(current instanceof MapXNode) {
                current = ((MapXNode) current).get(new QName(cmp));
            } else if(current != null) {
                throw new AssertionError(cmp + " should be MapXNode not " + current.getClass().getSimpleName());
            } else {
                Assert.fail("Node " + cmp + " not found.");
            }
        }
        assertNotNull(current, "Object at " + Joiner.on("/").join(path) + " should not be null");
        assertTrue(type.isInstance(current), "Current must be instanceof " + type);
        return (E) type.cast(current);
    }

}

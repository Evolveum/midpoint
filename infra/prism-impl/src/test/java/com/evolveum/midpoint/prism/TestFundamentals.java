/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author Radovan Semancik
 */
public class TestFundamentals extends AbstractPrismTest {

    @Test
    public void testPrismValueContainsRealValue() {
        // GIVEN
        PrismPropertyValue<String> valFoo1 = new PrismPropertyValueImpl<>("foo");
        PrismPropertyValue<String> valBar1 = new PrismPropertyValueImpl<>("bar");
        valBar1.setOriginType(OriginType.OUTBOUND);
        Collection<PrismValue> collection = new ArrayList<>();
        collection.add(valFoo1);
        collection.add(valBar1);

        PrismPropertyValue<String> valFoo2 = new PrismPropertyValueImpl<>("foo");
        PrismPropertyValue<String> valFoo3 = new PrismPropertyValueImpl<>("foo");
        valFoo3.setOriginType(OriginType.OUTBOUND);

        PrismPropertyValue<String> valBar2 = new PrismPropertyValueImpl<>("bar");
        valBar2.setOriginType(OriginType.OUTBOUND);
        PrismPropertyValue<String> valBar3 = new PrismPropertyValueImpl<>("bar");

        PrismPropertyValue<String> valBaz = new PrismPropertyValueImpl<>("baz");

        // WHEN - THEN
        assert PrismValueCollectionsUtil.containsRealValue(collection, valFoo1);
        assert PrismValueCollectionsUtil.containsRealValue(collection, valBar1);
        assert PrismValueCollectionsUtil.containsRealValue(collection, valFoo2);
        assert PrismValueCollectionsUtil.containsRealValue(collection, valBar2);
        assert PrismValueCollectionsUtil.containsRealValue(collection, valFoo3);
        assert PrismValueCollectionsUtil.containsRealValue(collection, valBar3);
        assert !PrismValueCollectionsUtil.containsRealValue(collection, valBaz);
    }

    @Test
    public void testRawTypeClone() {
        // GIVEN
        QName typeQName = new QName("abcdef");
        MapXNodeImpl mapXNode = new MapXNodeImpl();
        mapXNode.setTypeQName(typeQName);
        mapXNode.freeze();
        RawType rawType = new RawType(mapXNode, PrismTestUtil.getPrismContext());

        // WHEN
        RawType rawTypeClone = rawType.clone();

        // THEN
        assertEquals("Wrong or missing type QName", typeQName, rawTypeClone.getXnode().getTypeQName());
    }
}

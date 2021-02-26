package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineDataType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineItemType;

public class TestBeanSerialization extends AbstractSchemaTest {



    @Test
    public void testPipelineItemXsdType() throws SchemaException {
        PipelineDataType bean = new PipelineDataType();
        bean.beginItem().value("s1");
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        RootXNode xnode = prismContext.xnodeSerializer().root(new QName("output")).serializeRealValue(bean);
        MapXNode output = (MapXNode) xnode.toMapXNode().get(new QName("output"));
        XNode item = ((ListXNode) output.get(PipelineDataType.F_ITEM)).get(0);
        assertTrue(item instanceof MapXNode);

        XNode value = ((MapXNode) item).get(PipelineItemType.F_VALUE);
        assertTrue(value instanceof PrimitiveXNodeImpl);

        assertTrue(value.isExplicitTypeDeclaration());
        assertEquals(value.getTypeQName(), DOMUtil.XSD_STRING);

        //displayValue("output in XML", prismContext.xmlSerializer().root(new QName("output")).serializeRealValue(bean));


    }

}

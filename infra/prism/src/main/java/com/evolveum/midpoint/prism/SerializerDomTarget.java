package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * @author mederly
 */
public class SerializerDomTarget extends SerializerTarget<Element> {

    public SerializerDomTarget(@NotNull ParserHelpers parserHelpers) {
        super(parserHelpers);
    }

    @Override
    public Element serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return parserHelpers.parserRegistry.domParser().serializeXRootToElement(xroot);
    }
}

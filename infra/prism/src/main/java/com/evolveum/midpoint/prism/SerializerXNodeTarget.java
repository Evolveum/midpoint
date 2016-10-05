package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class SerializerXNodeTarget extends SerializerTarget<XNode> {

    public SerializerXNodeTarget(@NotNull ParserHelpers parserHelpers) {
        super(parserHelpers);
    }

    @Override
    public XNode serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return xroot;
    }
}

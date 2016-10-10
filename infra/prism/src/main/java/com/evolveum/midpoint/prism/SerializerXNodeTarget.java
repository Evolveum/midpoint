package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class SerializerXNodeTarget extends SerializerTarget<XNode> {

    public SerializerXNodeTarget(@NotNull LexicalHelpers lexicalHelpers) {
        super(lexicalHelpers);
    }

    @Override
    public XNode serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return xroot;
    }
}

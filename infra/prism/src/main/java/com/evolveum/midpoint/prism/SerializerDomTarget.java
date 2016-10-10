package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * @author mederly
 */
public class SerializerDomTarget extends SerializerTarget<Element> {

    public SerializerDomTarget(@NotNull LexicalHelpers lexicalHelpers) {
        super(lexicalHelpers);
    }

    @Override
    public Element serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return lexicalHelpers.lexicalProcessorRegistry.domParser().serializeXRootToElement(xroot);
    }
}

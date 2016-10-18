package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * @author mederly
 */
public class SerializerDomTarget extends SerializerTarget<Element> {

    public SerializerDomTarget(@NotNull PrismContextImpl prismContext) {
        super(prismContext);
    }

    @Override
    @NotNull
    public Element write(@NotNull RootXNode xroot, SerializationContext context) throws SchemaException {
        return prismContext.getLexicalProcessorRegistry().domProcessor().writeXRootToElement(xroot);
    }
}

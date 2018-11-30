package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public class SerializerDomTarget extends SerializerTarget<Element> {

    public SerializerDomTarget(@NotNull PrismContextImpl prismContext) {
        super(prismContext);
    }

    @Override
    @NotNull
    public Element write(@NotNull RootXNodeImpl xroot, SerializationContext context) throws SchemaException {
        return prismContext.getLexicalProcessorRegistry().domProcessor().writeXRootToElement(xroot);
    }

    @NotNull
    @Override
    public Element write(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName, @Nullable SerializationContext context)
            throws SchemaException {
        return prismContext.getLexicalProcessorRegistry().domProcessor().writeXRootListToElement(roots, aggregateElementName);
    }
}

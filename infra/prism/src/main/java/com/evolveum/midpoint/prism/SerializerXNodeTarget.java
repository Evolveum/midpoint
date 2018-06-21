package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public class SerializerXNodeTarget extends SerializerTarget<RootXNode> {

    public SerializerXNodeTarget(@NotNull PrismContextImpl prismContext) {
        super(prismContext);
    }

    @NotNull
    @Override
    public RootXNode write(@NotNull RootXNode xroot, SerializationContext context) throws SchemaException {
        return xroot;
    }

    @NotNull
    @Override
    public RootXNode write(@NotNull List<RootXNode> roots, QName aggregateElementName, SerializationContext context)
            throws SchemaException {
        throw new UnsupportedOperationException("Serialization of a collection of objects is not supported for XNode target.");
    }
}

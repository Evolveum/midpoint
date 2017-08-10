package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public abstract class SerializerTarget<T> {

    @NotNull public final PrismContextImpl prismContext;

    protected SerializerTarget(@NotNull PrismContextImpl prismContext) {
        this.prismContext = prismContext;
    }

    @NotNull
    abstract public T write(@NotNull RootXNode xroot, SerializationContext context) throws SchemaException;

    @NotNull
    abstract public T write(@NotNull List<RootXNode> roots, QName aggregateElementName,
            SerializationContext context) throws SchemaException;
}

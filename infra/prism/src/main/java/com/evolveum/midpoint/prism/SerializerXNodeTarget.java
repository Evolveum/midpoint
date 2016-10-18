package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

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
}

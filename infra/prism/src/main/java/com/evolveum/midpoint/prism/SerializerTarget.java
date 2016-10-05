package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public abstract class SerializerTarget<T> {

    @NotNull final ParserHelpers parserHelpers;

    protected SerializerTarget(@NotNull ParserHelpers parserHelpers) {
        this.parserHelpers = parserHelpers;
    }

    abstract public T serialize(RootXNode xroot, SerializationContext context) throws SchemaException;
}

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public abstract class SerializerTarget<T> {

    @NotNull final LexicalHelpers lexicalHelpers;

    protected SerializerTarget(@NotNull LexicalHelpers lexicalHelpers) {
        this.lexicalHelpers = lexicalHelpers;
    }

    abstract public T serialize(RootXNode xroot, SerializationContext context) throws SchemaException;
}

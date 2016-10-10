package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class SerializerStringTarget extends SerializerTarget<String> {

    @NotNull private final String language;

    public SerializerStringTarget(@NotNull LexicalHelpers lexicalHelpers, @NotNull String language) {
        super(lexicalHelpers);
        this.language = language;
    }

    @Override
    public String serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return lexicalHelpers.lexicalProcessorRegistry.parserFor(language).write(xroot, context);
    }
}

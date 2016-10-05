package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializerTarget;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class SerializerStringTarget extends SerializerTarget<String> {

    @NotNull private final String language;

    public SerializerStringTarget(@NotNull ParserHelpers parserHelpers, @NotNull String language) {
        super(parserHelpers);
        this.language = language;
    }

    @Override
    public String serialize(RootXNode xroot, SerializationContext context) throws SchemaException {
        return parserHelpers.parserRegistry.parserFor(language).serializeToString(xroot, context);
    }
}

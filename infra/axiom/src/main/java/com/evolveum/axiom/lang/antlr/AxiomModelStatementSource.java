/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.antlr.AxiomParser;
import com.evolveum.axiom.lang.antlr.AxiomParser.StatementContext;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomStatementStreamListener;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class AxiomModelStatementSource extends AxiomAntlrStatementSource implements AxiomIdentifierResolver {

    private static final String IMPORT = "import";
    private static final String NAMESPACE = "namespace";

    private String name;
    private Map<String,String> imports;
    private String namespace;

    public static AxiomModelStatementSource from(InputStream stream) throws IOException, AxiomSyntaxException {
        return from(null, CharStreams.fromStream(stream));
    }

    public static AxiomModelStatementSource from(String sourceName, InputStream stream) throws IOException, AxiomSyntaxException {
        return from(sourceName, CharStreams.fromStream(stream));
    }

    public static AxiomModelStatementSource from(String sourceName, CharStream stream) throws AxiomSyntaxException {
        StatementContext statement = AxiomAntlrStatementSource.contextFrom(sourceName, stream);
        String name = statement.argument().identifier().localIdentifier().getText();
        return new AxiomModelStatementSource(sourceName, statement, name, namespace(statement), imports(statement));
    }

    private AxiomModelStatementSource(String sourceName, StatementContext statement, String namespace, String name, Map<String, String> imports) {
        super(sourceName, statement);
        this.name = name;
        this.imports = imports;
        this.namespace = namespace;
    }


    public String modelName() {
        return name;
    }

    public String namespace() {
        return namespace;
    }

    public void stream(AxiomIdentifierResolver resolver, AxiomStatementStreamListener listener) {
        stream(resolver, listener, Optional.empty());
    }

    public void stream(AxiomIdentifierResolver resolver, AxiomStatementStreamListener listener,
            Optional<Set<AxiomIdentifier>> emitOnly) {
        stream(resolver.or(this), BUILTIN_TYPES.or(this).or(resolver), listener, emitOnly);
    }

    public static Map<String,String> imports(AxiomParser.StatementContext root) {
        Map<String,String> prefixMap = new HashMap<>();
        root.statement().stream().filter(s -> IMPORT.equals(s.identifier().getText())).forEach(c -> {
            String prefix = c.argument().identifier().localIdentifier().getText();
            String namespace = namespace(c);
            prefixMap.put(prefix, namespace);
        });
        prefixMap.put("",namespace(root));
        return prefixMap;
    }

    private static String namespace(StatementContext c) {
        return AxiomAntlrVisitor.convert(c.statement()
                .stream().filter(s -> NAMESPACE.equals(s.identifier().getText()))
                .findFirst().get().argument().string());
    }

    @Override
    public AxiomIdentifier resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        if(prefix == null) {
            prefix = "";
        }
        String maybeNs = imports.get(prefix);
        if(maybeNs != null) {
            return AxiomIdentifier.from(maybeNs, localName);
        }
        return null;
    }
}

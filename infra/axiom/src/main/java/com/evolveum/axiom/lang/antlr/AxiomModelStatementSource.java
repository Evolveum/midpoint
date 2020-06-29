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

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.stream.AxiomItemStream.TargetWithResolver;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class AxiomModelStatementSource extends AxiomAntlrStatementSource implements AxiomNameResolver {

    private static final String IMPORT = "import";
    private static final String NAMESPACE = "namespace";
    private static final String PREFIX = "prefix";

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
        try {
        ItemContext root = AxiomAntlrStatementSource.contextFrom(sourceName, stream);
        String name = root.itemValue().argument().prefixedName().localName().getText();
        return new AxiomModelStatementSource(sourceName, root, name, namespace(root.itemValue()), imports(root.itemValue()));
        } catch (AxiomSyntaxException e) {
            throw e;
        } catch (Exception e) {
            throw new AxiomSyntaxException(SourceLocation.from(sourceName, 0, 0), "Unexpected error", e);
        }
    }

    private AxiomModelStatementSource(String sourceName, ItemContext statement, String namespace, String name, Map<String, String> imports) {
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

    @Override
    public void stream(TargetWithResolver target, Optional<Set<AxiomName>> emitOnly) {
        stream(target, emitOnly, this);
    }

    public Map<String, String> imports() {
        return imports;
    }

    // FIXME: Use schema & AxiomItemTarget to get base model data?
    public static Map<String,String> imports(AxiomParser.ItemValueContext root) {
        Map<String,String> prefixMap = new HashMap<>();
        root.item().stream().filter(s -> IMPORT.equals(s.itemName().getText())).forEach(c -> {
            String namespace = AxiomAntlrVisitor2.convert(c.itemValue().argument().string());
            String prefix = prefix(c.itemValue());
            prefixMap.put(prefix, namespace);
        });
        prefixMap.put("",namespace(root));
        return prefixMap;
    }


    private static String namespace(AxiomParser.ItemValueContext c) {
        return AxiomAntlrVisitor2.convert(c.item()
                .stream().filter(s -> NAMESPACE.equals(s.itemName().getText()))
                .findFirst().get().itemValue().argument().string());
    }

    private static String prefix(AxiomParser.ItemValueContext c) {
        return AbstractAxiomAntlrVisitor.convertToString(c.item()
                .stream().filter(s -> PREFIX.equals(s.itemName().getText()))
                .findFirst().get().itemValue().argument());
    }

    @Override
    public AxiomName resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        if(prefix == null) {
            prefix = "";
        }
        String maybeNs = imports.get(prefix);
        if(maybeNs != null) {
            return AxiomName.from(maybeNs, localName);
        }
        return null;
    }
}

package com.evolveum.midpoint.prism.impl.lex.json;

import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.QNameUtil.PrefixedName;
import com.evolveum.midpoint.util.QNameUtil.QNameInfo;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Strings;

public abstract class DefinitionContext {

    private static final SchemaIgnorant EMPTY = new SchemaIgnorant(new QName(""));

    private static final @NotNull QName FILTER_CLAUSE = new QName(PrismConstants.NS_QUERY, "filterClause");

    private final @NotNull QName name;

    protected DefinitionContext(QName name) {
        this.name = name;
    }

    public static DefinitionContext root(@NotNull SchemaRegistry schemaRegistry) {
        return new Root(schemaRegistry);
    }

    public static DefinitionContext empty() {
        return EMPTY;
    }

    public static QName resolveQName(String name, PrismNamespaceContext context) throws SchemaException {
        return empty().resolve(name, context.withoutDefault()).getName();
    }


    public @NotNull QName getName() {
        return name;
    }

    public @NotNull DefinitionContext resolve(@NotNull String name, @NotNull PrismNamespaceContext namespaceContext) throws SchemaException {
        if (isInfra(name)) {
            // Infra properties are unqualified for now
            // TODO: We could return definition for infra properties later
            return unawareFrom(new QName(name));
        }
        if (!QNameUtil.isUriQName(name)) {
            PrefixedName prefixed = QNameUtil.parsePrefixedName(name);
            if (prefixed.prefix().isEmpty()) {
                DefinitionContext resolved = resolveLocally(name);
                if(resolved != null) {
                    return resolved;
                }
            }
            Optional<String> ns = namespaceContext.namespaceFor(prefixed.prefix());
            if(ns.isPresent()) {
                return toContext(new QName(ns.get(), prefixed.localName()));
            } else if (!prefixed.prefix().isEmpty()) {
                warnOrThrow("Undeclared prefix '%s'", prefixed.prefix());
            } else {
                return toContext(new QName(prefixed.localName()));
            }
        }
        QNameInfo result = QNameUtil.uriToQNameInfo(name, true);
        // FIXME: Explicit empty namespace is workaround for cases, where we somehow lost namespace
        // eg. parsing json with filters without namespaces
        if (Strings.isNullOrEmpty(result.name.getNamespaceURI()) && !result.explicitEmptyNamespace) {
            Optional<String> defaultNs = namespaceContext.defaultNamespace();
            if(defaultNs.isPresent()) {
                result = QNameUtil.qnameToQnameInfo(new QName(defaultNs.get(), result.name.getLocalPart()));
            }
        }
        return toContext(result.name);
    }

    private boolean isInfra(@NotNull String name) {
        return name.startsWith("@");
    }

    private void warnOrThrow(String string, String prefix) throws SchemaException {
        throw new SchemaException(Strings.lenientFormat(string, prefix));
    }

    protected abstract @Nullable DefinitionContext resolveLocally(@NotNull String localName);
    protected abstract @Nullable DefinitionContext resolveLocally(@NotNull QName name);

    private @NotNull DefinitionContext toContext(QName name) {
        DefinitionContext ret = resolveLocally(name);
        if(ret != null) {
            return ret;
        }
        return unawareFrom(name);
    }


    private static class Root extends DefinitionContext {

        private SchemaRegistry registry;

        public Root(SchemaRegistry reg) {
            super(new QName(""));
            this.registry = reg;
        }

        @Override
        protected DefinitionContext resolveLocally(String localName) {
            return null;
        }

        @Override
        protected DefinitionContext resolveLocally(QName name) {
            PrismObjectDefinition<?> def = registry.findObjectDefinitionByElementName(name);
            return awareFrom(name, def);
        }
    }

    private static class ComplexTypeAware extends DefinitionContext {

        protected final ComplexTypeDefinition definition;

        public ComplexTypeAware(QName name, ComplexTypeDefinition definition) {
            super(name);
            this.definition = definition;
        }

        @Override
        protected DefinitionContext resolveLocally(QName name) {
            return awareFrom(name, findDefinition(name));
        }

        protected ItemDefinition<?> findDefinition(QName name) {
            return definition.findLocalItemDefinition(name);
        }

        @Override
        protected DefinitionContext resolveLocally(String localName) {
            QName proposed = new QName(definition.getTypeName().getNamespaceURI(),localName);
            ItemDefinition def = findDefinition(proposed);
            if(def != null) {
                return awareFrom(proposed, def);
            }
            return null;
        }
    }

    private static class ComplexTypeWithSubstitutions extends ComplexTypeAware {

        public ComplexTypeWithSubstitutions(QName name, ComplexTypeDefinition definition) {
            super(name, definition);
        }

        @Override
        protected ItemDefinition<?> findDefinition(QName name) {
            ItemDefinition<?> directHit = super.findDefinition(name);
            if(directHit != null) {
                return directHit;
            }
            for(ItemDefinition potential : definition.getPrismContext().getSchemaRegistry().findItemDefinitionsByElementName(name)) {
                QName substitutionHead = potential.getSubstitutionHead();
                if(substitutionHead != null) {
                    ItemDefinition<?> head = super.findDefinition(substitutionHead);
                    if(head != null) {
                        return potential;
                    }
                }
            }
            return null;
        }
    }

    private static class SchemaIgnorant extends DefinitionContext {

        public SchemaIgnorant(QName name) {
            super(name);
        }

        @Override
        protected DefinitionContext resolveLocally(QName name) {
            return null;
        }

        @Override
        protected DefinitionContext resolveLocally(String localName) {
            return null;
        }

        @Override
        public @NotNull DefinitionContext unaware() {
            return this;
        }
    }

    public static DefinitionContext awareFrom(QName name, ItemDefinition<?> definition) {
        if(name.getLocalPart().equals("filter")) {
            name.toString();
        }

        if (definition instanceof PrismContainerDefinition<?>) {
            // Should we return item name?
            return awareFromType(definition.getItemName(), ((PrismContainerDefinition<?>) definition).getComplexTypeDefinition());
        }
        if (definition instanceof PrismPropertyDefinition<?>) {
            // Properties with structured contents
            Optional<ComplexTypeDefinition> structured = ((PrismPropertyDefinition<?>) definition).structuredType();
            return awareFromType(name, structured.orElse(null));
        }
        if (definition instanceof PrismReferenceDefinition) {
            // Properties with structured contents
            Optional<ComplexTypeDefinition> structured = ((PrismReferenceDefinition) definition).structuredType();
            return awareFromType(name, structured.orElse(null));
        }

        return unawareFrom(name);
    }

    public static DefinitionContext awareFromType(QName name, @Nullable TypeDefinition definition) {
        // FIXME: We can add special hadling here
        if(definition instanceof ComplexTypeDefinition) {
            ComplexTypeDefinition complex = (ComplexTypeDefinition) definition;
            if(hasSubstitutions(complex)) {
                return new ComplexTypeWithSubstitutions(name, complex);
            }
            return new ComplexTypeAware(name, complex);
        }
        return unawareFrom(name);

    }

    /**
     *
     *  FIXME: This should perform actual check on complex type.
     *  Ideal solution is for complex type to carry information about
     *  potential substitution groups.
     * @param complex
     * @return
     */
    private static boolean hasSubstitutions(ComplexTypeDefinition complex) {
        return complex.findLocalItemDefinition(FILTER_CLAUSE) != null;
    }

    public static DefinitionContext unawareFrom(QName name) {
        return new SchemaIgnorant(name);
    }

    public @NotNull DefinitionContext unaware() {
        return unawareFrom(getName());
    }

    @Override
    public String toString() {
        return Objects.toString(getName());
    }
}

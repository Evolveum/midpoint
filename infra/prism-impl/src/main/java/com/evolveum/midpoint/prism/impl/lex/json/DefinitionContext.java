package com.evolveum.midpoint.prism.impl.lex.json;

import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
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

    public static Root root(@NotNull SchemaRegistry schemaRegistry) {
        return new SchemaRoot(schemaRegistry);
    }

    public static Root empty() {
        return EMPTY;
    }

    protected abstract DefinitionContext unawareFrom(QName name);

    public static QName resolveQName(String name, PrismNamespaceContext context) throws SchemaException {
        return empty().resolve(name, context.withoutDefault()).getName();
    }


    public @NotNull QName getName() {
        return name;
    }

    public abstract Optional<QName> getType();

    public @NotNull DefinitionContext resolve(@NotNull String name, @NotNull PrismNamespaceContext namespaceContext) throws SchemaException {
        if (isInfra(name)) {
            if (JsonInfraItems.PROP_VALUE.equals(name)) {
                return valueContext();
            }
            if (JsonInfraItems.PROP_METADATA.equals(name)) {
                return metadataDef();
            }
            // Infra properties are unqualified for now
            // TODO: We could return definition for infra properties later
            return unawareFrom(new QName(name));
        }
        if (!QNameUtil.isUriQName(name)) {
            PrefixedName prefixed = QNameUtil.parsePrefixedName(name);
            if (prefixed.prefix().isEmpty()) {
                DefinitionContext resolved = resolveLocally(name);
                if (resolved != null) {
                    return resolved;
                }
            }
            Optional<String> ns = namespaceContext.namespaceFor(prefixed.prefix());
            if (ns.isPresent()) {
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

    public @NotNull DefinitionContext unaware() {
        return unawareFrom(getName());
    }

    public @NotNull DefinitionContext moreSpecific(@NotNull DefinitionContext other) {
        // Prefer type aware
        if(other instanceof ComplexTypeAware) {
            return other;
        }
        return this;
    }

    public DefinitionContext child(QName name) {
        DefinitionContext maybe = resolveLocally(name);
        if(maybe != null) {
            return maybe;
        }
        return unawareFrom(name);
    }

    private @NotNull DefinitionContext valueContext() {
        return new Value(this);
    }

    private boolean isInfra(@NotNull String name) {
        return name.startsWith("@");
    }

    private void warnOrThrow(String string, String prefix) throws SchemaException {
        throw new SchemaException(Strings.lenientFormat(string, prefix));
    }

    protected @Nullable DefinitionContext resolveLocally(@NotNull String localName) {
        return null;
    }

    protected @Nullable DefinitionContext resolveLocally(@NotNull QName name) {
        return null;
    }

    private @NotNull DefinitionContext toContext(QName name) {
        DefinitionContext ret = resolveLocally(name);
        if(ret != null) {
            return ret;
        }
        return unawareFrom(name);
    }

    private abstract static class SchemaAware extends DefinitionContext {

        protected final SchemaRoot root;
        private final boolean inherited;

        public SchemaAware(QName name, SchemaRoot root, boolean inherited) {
            super(name);
            this.inherited = inherited;
            this.root = root;
        }

        @Override
        public boolean definedInParent() {
            return inherited;
        }

        protected DefinitionContext awareFrom(QName name, ItemDefinition<?> definition, boolean inherited) {
            return root.awareFrom(name, definition, inherited);
        }

        @Override
        public @NotNull DefinitionContext withType(QName typeName) {
            return root.fromType(getName(), typeName, inherited);
        }

        @Override
        protected DefinitionContext unawareFrom(QName name) {
            return root.unawareFrom(name);
        }

        @Override
        public DefinitionContext metadataDef() {
            return root.metadataDef();
        }

    }

    public abstract static class Root extends DefinitionContext {

        protected Root(QName name) {
            super(name);
        }

        @Override
        public abstract DefinitionContext metadataDef();

    }

    private static class SchemaRoot extends Root {

        private SchemaRegistry registry;

        public SchemaRoot(SchemaRegistry reg) {
            super(new QName(""));
            registry = reg;
        }


        public @NotNull DefinitionContext fromType(@NotNull QName name, QName typeName, boolean inherited) {
            var definition = Optional.ofNullable(registry.findComplexTypeDefinitionByType(typeName));
            return awareFrom(name, typeName, definition, inherited);
        }


        DefinitionContext awareFrom(QName name, ItemDefinition<?> definition, boolean inherited) {
            if(definition != null) {
                return awareFrom(definition.getItemName(), definition.getTypeName(),definition.structuredType(), inherited);
            }
            // FIXME: Maybe we should retain schema?
            return unawareFrom(name);
        }

        private DefinitionContext awareFrom(QName name, @NotNull QName typeName,
                Optional<ComplexTypeDefinition> structuredType, boolean inherited) {
            if(structuredType.isPresent()) {
                var complex = structuredType.get();
                if(complex.hasSubstitutions()) {
                    return new ComplexTypeWithSubstitutions(name, complex, this, inherited);
                }
                return new ComplexTypeAware(name, complex, this, inherited);
            }
            return new SimpleType(name, typeName, inherited, this);
        }


        @Override
        public @NotNull DefinitionContext withType(QName typeName) {
            return fromType(getName(), typeName, false);
        }


        @Override
        protected DefinitionContext resolveLocally(String localName) {
            return null;
        }

        @Override
        protected DefinitionContext resolveLocally(QName name) {
            ItemDefinition<?> def = registry.findObjectDefinitionByElementName(name);
            if(def == null) {
                try {
                    def = registry.findItemDefinitionByElementName(name);
                } catch (IllegalStateException e) {
                    return unawareFrom(name);
                }
            }
            return awareFrom(name, def, false);
        }

        @Override
        public Optional<QName> getType() {
            return Optional.empty();
        }

        @Override
        protected DefinitionContext unawareFrom(QName name) {
            return new SimpleType(name, null, false, this);
        }

        @Override
        public DefinitionContext metadataDef() {
            var def = registry.getValueMetadataDefinition();
            return awareFrom(JsonInfraItems.PROP_METADATA_QNAME, def.getTypeName(), def.structuredType(), true);
        }

    }

    private static class ComplexTypeAware extends SchemaAware {

        protected final ComplexTypeDefinition definition;

        public ComplexTypeAware(QName name, ComplexTypeDefinition definition, SchemaRoot root, boolean inherited) {
            super(name, root, inherited);
            this.definition = definition;
        }

        @Override
        public Optional<QName> getType() {
            return Optional.of(definition.getTypeName());
        }

        @Override
        protected DefinitionContext resolveLocally(QName name) {
            return awareFrom(name, findDefinition(name), true);
        }

        protected ItemDefinition<?> findDefinition(QName name) {
            return definition.findLocalItemDefinition(name);
        }

        @Override
        protected DefinitionContext resolveLocally(String localName) {
            QName proposed = new QName(definition.getTypeName().getNamespaceURI(),localName);
            ItemDefinition<?> def = findDefinition(proposed);
            if(def == null) {
                def = findDefinition(new QName(localName));
            }
            if(def != null) {
                return awareFrom(proposed, def, true);
            }
            return null;
        }

        @Override
        public @NotNull DefinitionContext moreSpecific(@NotNull DefinitionContext other) {
            if(other instanceof ComplexTypeAware) {
                ComplexTypeDefinition localType = this.definition;
                ComplexTypeDefinition otherType = ((ComplexTypeAware) other).definition;
                if(localType == otherType) {
                    return other;
                }
                if (localType.getTypeName().equals(otherType.getSuperType())) {
                    return other;
                }
            }
            return this;
        }

    }

    private static class ComplexTypeWithSubstitutions extends ComplexTypeAware {

        public ComplexTypeWithSubstitutions(QName name, ComplexTypeDefinition definition, SchemaRoot root, boolean inherited) {
            super(name, definition, root, inherited);
        }

        @Override
        protected ItemDefinition<?> findDefinition(QName name) {
            return definition.itemOrSubstitution(name).orElse(null);
        }
    }

    private static class SchemaIgnorant extends Root {

        public SchemaIgnorant(QName name) {
            super(name);
        }

        @Override
        public @NotNull DefinitionContext unaware() {
            return this;
        }

        @Override
        public Optional<QName> getType() {
            return Optional.empty();
        }

        @Override
        public @NotNull DefinitionContext withType(QName typeName) {
            return this;
        }

        @Override
        protected DefinitionContext unawareFrom(QName name) {
            return new SchemaIgnorant(name);
        }

        @Override
        public DefinitionContext metadataDef() {
            return new SchemaIgnorant(JsonInfraItems.PROP_METADATA_QNAME);
        }

    }

    private static class Value extends DefinitionContext {

        DefinitionContext delegate;

        public Value(DefinitionContext delegate) {
            super(JsonInfraItems.PROP_VALUE_QNAME);
            this.delegate = delegate;
        }

        @Override
        protected @Nullable DefinitionContext resolveLocally(@NotNull String localName) {
            return delegate.resolveLocally(localName);
        }

        @Override
        protected @Nullable DefinitionContext resolveLocally(@NotNull QName name) {
            return delegate.resolveLocally(name);
        }

        @Override
        public Optional<QName> getType() {
            return delegate.getType();
        }

        @Override
        public @NotNull DefinitionContext withType(QName typeName) {
            return new Value(delegate.withType(typeName));
        }

        @Override
        protected DefinitionContext unawareFrom(QName name) {
            return delegate.unawareFrom(name);
        }

        @Override
        public DefinitionContext metadataDef() {
            return delegate.metadataDef();
        }

    }

    private static class SimpleType extends SchemaAware {

        public SimpleType(QName name, QName type, boolean inherited, SchemaRoot root) {
            super(name, root, inherited);
            this.type = type;
        }

        private final QName type;

        @Override
        public Optional<QName> getType() {
            return Optional.ofNullable(type);
        }

    }

    @Override
    public String toString() {
        return Objects.toString(getName());
    }

    public boolean definedInParent() {
        return false;
    }

    public abstract @NotNull DefinitionContext withType(QName typeName);

    public abstract DefinitionContext metadataDef();

}

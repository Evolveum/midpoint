/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query.lang;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Builder;
import com.evolveum.axiom.lang.antlr.AxiomStrings;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerialization;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static com.evolveum.midpoint.prism.query.PrismQuerySerialization.NotSupportedException;


public class QueryWriter implements Builder<PrismQuerySerialization> {

    private static final String PATH_SELF = ".";

    private static final String MATCHING_RULE_NS = PrismQueryLanguageParserImpl.MATCHING_RULE_NS;

    private final PrismQuerySerializerImpl.SimpleBuilder target;

    public QueryWriter(PrismQuerySerializerImpl.SimpleBuilder target) {
        this.target = target;
    }

    public void writeSelf() {
        target.emitWord(PATH_SELF);
    }


    void writePath(String string) {
        target.emitSpace();
        target.emit(string);
    }


    public void writePath(ItemPath path) {
        ItemPathSerialization pathSer = ItemPathSerialization.serialize(UniformItemPath.from(path), target.context());
        target.addPrefixes(pathSer.undeclaredPrefixes());
        target.emitSpace();
        target.emit(pathSer.getXPathWithoutDeclarations());
    }

    public void writeMatchingRule(@Nullable QName matchingRule) {
        if(matchingRule == null) {
            return;
        }
        target.emit("[");
        emitQName(matchingRule, MATCHING_RULE_NS);

        target.emit("]");

    }

    public void writeFilterName(QName filter) {
        var alias = lookupAlias(filter);
        target.emitSpace();
        if (alias.isPresent()) {
            target.emit(alias.get());
        } else {
            emitQName(filter, FilterNames.QUERY_NS);
        }
    }

    public void writeFilter(ObjectFilter filter) throws NotSupportedException {
        writeFilter(filter, this);
    }

    public void writeNestedFilter(ObjectFilter condition) throws NotSupportedException {
        startNestedFilter();
        writeFilter(condition);
        endNestedFilter();
    }

    public void writeNegatedFilter(ObjectFilter filter) throws NotSupportedException {
        writeFilter(filter, negated());
    }

    public void writeValues(@Nullable List<? extends PrismPropertyValue<?>> values) {
        Preconditions.checkArgument(values != null && !values.isEmpty(), "Value must be specified");
        writeList(values, this::writeValue);
    }


    public void startNestedFilter() {
        target.emitSpace();
        target.emitSeparator("(");
    }

    public void endNestedFilter() {
        target.emit(")");
    }

    public void writeRawValue(Object rawValue) {
        target.emitSpace();
        if (rawValue instanceof ItemPath) {
            writePath((ItemPath) rawValue);
            return;
        }
        if (rawValue instanceof QName) {
            writeQName((QName) rawValue);
            return;
        }
        if (rawValue instanceof Number) {
            // FIXME: we should have some common serialization utility
            target.emit(rawValue.toString());
            return;
        }
        writeString(rawValue);
    }

    public void writeRawValues(Collection<?> oids) {
        writeList(oids, this::writeRawValue);
    }

    @Override
    public PrismQuerySerialization build() {
        return target.build();
    }

    private Optional<String> lookupAlias(QName filter) {
        return FilterNames.aliasFor(filter);
    }

    private void emitQName(QName filter, String additionalDefaultNs) {
        String prefix = resolvePrefix(filter, additionalDefaultNs);
        if(!Strings.isNullOrEmpty(prefix)) {
            target.emit(prefix);
            target.emit(":");
        }
        target.emit(filter.getLocalPart());
    }

    private String resolvePrefix(QName name, String additionalDefaultNs) {
        if (Strings.isNullOrEmpty(name.getNamespaceURI()) || Objects.equals(additionalDefaultNs, name.getNamespaceURI())) {
            return PrismNamespaceContext.DEFAULT_PREFIX;
        }
        return target.prefixFor(name.getNamespaceURI(), name.getPrefix());
    }


    private void writeFilter(ObjectFilter filter, QueryWriter output) throws NotSupportedException {
        FilterSerializers.write(filter, output);
    }

    QueryWriter negated() {
        return new Negated(target);
    }

    private <T> void writeList(Collection<? extends T> values, Consumer<? super T> writer) {
        target.emitSpace();
        if(values.size() == 1) {
            writer.accept(values.iterator().next());
        } else {
            target.emitSeparator("(");
            var valuesIter = values.iterator();
            while (valuesIter.hasNext()) {
                writer.accept(valuesIter.next());
                if (valuesIter.hasNext()) {
                    target.emitSeparator(", ");
                }
            }
            target.emit(")");
        }
    }

    private void writeValue(PrismPropertyValue<?> prismPropertyValue) {
        // Now we emit values
        Object rawValue = prismPropertyValue.getValue();
        //QName typeName = prismPropertyValue.getTypeName();

        writeRawValue(rawValue);
    }

    private void writeString(Object rawValue) {
        target.emit(AxiomStrings.toSingleQuoted(rawValue.toString()));
    }

    private void writeQName(QName rawValue) {
        emitQName(rawValue, null);
    }

    class Negated extends QueryWriter {

        public Negated(PrismQuerySerializerImpl.SimpleBuilder target) {
            super(target);
        }

        @Override
        public void writeFilterName(QName filter) {
            target.emitWord("not");
            super.writeFilterName(filter);
        }

        @Override
        public void writeFilter(ObjectFilter filter) throws NotSupportedException {
            super.writeFilter(filter, QueryWriter.this);
        }

        @Override
        QueryWriter negated() {
            return this;
        }

    }


}

package com.evolveum.midpoint.prism.impl.query.lang;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.axiom.lang.antlr.AxiomAntlrLiterals;
import com.evolveum.axiom.lang.antlr.AxiomQuerySource;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.AndFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.DoubleQuoteStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.FilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.FilterNameContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.GenFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.ItemFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.MultilineStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.OrFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.PathContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.PrefixedNameContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SingleQuoteStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.StringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SubFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SubfilterOrValueContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.ValueSpecificationContext;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.impl.query.EqualFilterImpl;
import com.evolveum.midpoint.prism.impl.query.NotFilterImpl;
import com.evolveum.midpoint.prism.impl.query.SubstringFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PrismQueryLanguageParser {


    public static final String QUERY_NS = "http://prism.evolveum.com/xml/ns/public/query-3";
    public static final String MATCHING_RULE_NS = "http://prism.evolveum.com/xml/ns/public/matching-rule-3";


    public interface ItemFilterFactory {

        ObjectFilter create(ItemDefinition<?> definition, ItemPath path, QName matchingRule, SubfilterOrValueContext subfilterOrValue);

    }

    private abstract class PropertyFilterFactory implements ItemFilterFactory {

        @Override
        public ObjectFilter create(ItemDefinition<?> definition, ItemPath path, QName matchingRule,
                SubfilterOrValueContext subfilterOrValue) {
            Preconditions.checkArgument(subfilterOrValue != null);
            Preconditions.checkArgument(definition instanceof PrismPropertyDefinition<?>);
            PrismPropertyDefinition<?> propDef = (PrismPropertyDefinition<?>) definition;
            ValueSpecificationContext valueSpec = subfilterOrValue.valueSpecification();
            if(valueSpec.path() != null) {
                throw new UnsupportedOperationException("FIXME: Implement right side lookup");
            } else if (valueSpec.string() != null) {
                Object parsedValue = parseLiteral(propDef, valueSpec.string());
                return valueFilter(propDef, path, matchingRule, parsedValue);
            }
            throw new IllegalStateException();
        }

        abstract ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, Object value);

        abstract ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, ItemPath rightPath, PrismPropertyDefinition<?> rightDef);


    }


    private class SubstringFilterFactory extends PropertyFilterFactory {

        private final boolean anchorStart;
        private final boolean anchorEnd;



        public SubstringFilterFactory(boolean anchorStart, boolean anchorEnd) {
            this.anchorStart = anchorStart;
            this.anchorEnd = anchorEnd;
        }

        @Override
        ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                ItemPath rightPath, PrismPropertyDefinition<?> rightDef) {
            throw new UnsupportedOperationException();
        }

        @Override
        ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                Object value) {
            return SubstringFilterImpl.createSubstring(path, definition, context, matchingRule, value, anchorStart, anchorEnd);
        }
    };


    private static final Map<String, QName> ALIASES_TO_NAME = ImmutableMap.<String, QName>builder()
            .put("=", queryName("equal"))
            .put("<", queryName("less"))
            .put(">", queryName("greater"))
            .put("<=", queryName("lessOrEquals"))
            .put(">=", queryName("greaterOrEquals"))
            .build();


    private final Map<QName, ItemFilterFactory> filterFactories = ImmutableMap.<QName, ItemFilterFactory>builder()
            .put(queryName("equal"), new PropertyFilterFactory() {

                @Override
                public ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                        Object value) {
                    return EqualFilterImpl.createEqual(path, definition, matchingRule, context, value);
                }

                @Override
                public ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                        ItemPath rightPath, PrismPropertyDefinition<?> rightDef) {
                    return EqualFilterImpl.createEqual(path, definition, matchingRule, rightPath, rightDef);
                }
            })
            .put(queryName("contains"), new SubstringFilterFactory(false, false))
            .put(queryName("startsWith"), new SubstringFilterFactory(true, false))
            .put(queryName("endsWith"), new SubstringFilterFactory(false, true))
            .build()
            ;

    private final PrismContext context;

    public PrismQueryLanguageParser(PrismContext context) {
        this.context = context;
    }

    public <C extends Containerable> ObjectFilter parseQuery(Class<C> typeClass, String query) {
        return parseQuery(typeClass, AxiomQuerySource.from(query));
    }

    public <C extends Containerable> ObjectFilter parseQuery(Class<C> typeClass, AxiomQuerySource source) {
        ComplexTypeDefinition complexType = context.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(typeClass);
        if (complexType == null) {
            throw new IllegalArgumentException("Couldn't find definition for complex type " + typeClass);
        }

        ObjectFilter rootFilter = parseFilter(complexType, source.root());
        return rootFilter;
    }


    private static QName queryName(String localName) {
        return new QName(QUERY_NS,localName);
    }

    private ObjectFilter parseFilter(ComplexTypeDefinition complexType, FilterContext root) {
        if (root instanceof AndFilterContext) {
            return andFilter(complexType, (AndFilterContext) root);
        } else if (root instanceof OrFilterContext) {
            return orFilter(complexType, (OrFilterContext) root);
        } else if (root instanceof GenFilterContext) {
            return itemFilter(complexType, ((GenFilterContext) root).itemFilter());
        } else if (root instanceof SubFilterContext) {
            return parseFilter(complexType, ((SubFilterContext) root).subfilter().filter());
        }
        throw new IllegalStateException("Unsupported Filter Context");
    }

    private ObjectFilter andFilter(ComplexTypeDefinition complexType, AndFilterContext root) {
        ObjectFilter leftFilter = parseFilter(complexType,root.left);
        // FIXME: optimize right filters to single and
        ObjectFilter rightFilter = parseFilter(complexType,root.right);
        return context.queryFactory().createAndOptimized(ImmutableList.of(leftFilter, rightFilter));
    }


    private ObjectFilter orFilter(ComplexTypeDefinition complexType, OrFilterContext root) {
        ObjectFilter leftFilter = parseFilter(complexType,root.left);
        // FIXME: optimize right filters to single and
        ObjectFilter rightFilter = parseFilter(complexType,root.right);
        return context.queryFactory().createOrOptimized(ImmutableList.of(leftFilter, rightFilter));
    }

    private ObjectFilter itemFilter(ComplexTypeDefinition complexType, ItemFilterContext itemFilter) {
        // TODO Auto-generated method stub
        QName filterName = filterName(itemFilter.filterName());
        QName matchingRule = itemFilter.matchingRule() != null ? toFilterName(MATCHING_RULE_NS, itemFilter.matchingRule().prefixedName()) : null;
        ItemPath path = path(complexType, itemFilter.path());
        ItemDefinition<?> itemDefinition = complexType.findItemDefinition(path);
        ItemFilterFactory factory = filterFactories.get(filterName);
        Preconditions.checkArgument(factory != null, "Unknown filter %s", filterName);

        ObjectFilter filter = createItemFilter(factory,itemDefinition, path,matchingRule,itemFilter.subfilterOrValue());
        if(itemFilter.negation() != null) {
            return new NotFilterImpl(filter);
        }
        return filter;

    }

    private ObjectFilter createItemFilter(ItemFilterFactory factory, ItemDefinition<?> complexType, ItemPath path, QName matchingRule,
            SubfilterOrValueContext subfilterOrValue) {
        return factory.create(complexType, path, matchingRule, subfilterOrValue);
    }

    private ItemPath path(ComplexTypeDefinition complexType, PathContext path) {
        // FIXME: Implement proper parsing of decomposed item path from Antlr
        UniformItemPath ret = ItemPathHolder.parseFromString(path.getText());
        return ret;
    }

    private QName filterName(FilterNameContext filterName) {
        if ( filterName.filterNameAlias() != null) {
            return ALIASES_TO_NAME.get(filterName.filterNameAlias().getText());
        }
        return toFilterName(QUERY_NS, filterName.prefixedName());
    }

    private QName toFilterName(String defaultNs, PrefixedNameContext itemName) {
        String ns = defaultNs;
        // FIXME: Add namespace detection
        return new QName(ns,itemName.localName.getText());
    }

    ObjectFilter createEqual(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, ValueSpecificationContext value) {
        if(value.path() != null) {
            throw new UnsupportedOperationException("FIXME: Implement right side lookup");
        } else if (value.string() != null) {
            Object parsedValue = parseLiteral(definition, value.string());
            return EqualFilterImpl.createEqual(path, definition, matchingRule, context, parsedValue);
        }
        throw new IllegalStateException();
    }

    private Object parseLiteral(PrismPropertyDefinition<?> definition, StringContext string) {
        // FIXME: Use property definition for parsing (date, name, qname, etc)
        if (string instanceof DoubleQuoteStringContext) {
            return AxiomAntlrLiterals.convertDoubleQuote(string.getText());
        } else if (string instanceof SingleQuoteStringContext) {
            return AxiomAntlrLiterals.convertSingleQuote(string.getText());
        } else if (string instanceof MultilineStringContext) {
            return AxiomAntlrLiterals.convertMultiline(string.getText());
        }
        return null;
    }

}



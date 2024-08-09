/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.*;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;

import jakarta.xml.bind.JAXBElement;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.Nullable;

public class ExpressionUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);

    public enum ExpressionEvaluatorType {
        LITERAL,
        AS_IS,
        PATH,
        SCRIPT,
        GENERATE,
        ASSOCIATION_FROM_LINK,
        SHADOW_OWNER_REFERENCE_SEARCH
    }

    public enum Language {
        GROOVY("http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy"),
        PYTHON("http://midpoint.evolveum.com/xml/ns/public/expression/language#python"),
        VELOCITY("http://midpoint.evolveum.com/xml/ns/public/expression/language#velocity"),
        JAVASCRIPT("http://midpoint.evolveum.com/xml/ns/public/expression/language#ECMAScript");

        private final String language;

        Language(String language) {
            this.language = language;
        }

        public String getLanguage() {
            return language;
        }
    }

    public static final QName SHADOW_REF_KEY = new QName(SchemaConstants.NS_C, "shadowRef");
    private static final QName SHADOW_OID_KEY = new QName("oid");

    public static final String EXPRESSION_SCRIPT = "<script>\n" +
            "    <code>\n" +
            "        Insert your script here\n" +
            "    </code>\n" +
            "</script>";

    public static final String EXPRESSION_LITERAL = "<value>Insert value(s) here</value>";
    public static final String EXPRESSION_AS_IS = "<asIs/>";
    public static final String EXPRESSION_PATH = "<path>Insert path here</path>";
    public static final String EXPRESSION_GENERATE = "<generate>\n" +
            //"    <valuePolicyRef oid=\"Insert value policy oid\"/>\n" +
            "</generate>";

    public static final String ELEMENT_SCRIPT = "</script>";
    public static final String ELEMENT_GENERATE = "</generate>";
    public static final String ELEMENT_GENERATE_WITH_NS = "<generate";
    public static final String ELEMENT_PATH = "</path>";
    public static final String ELEMENT_VALUE = "</value>";
    public static final String ELEMENT_AS_IS = "<asIs/>";
    public static final String ELEMENT_AS_IS_WITH_NS = "<asIs";
    public static final String ELEMENT_ASSOCIATION_FROM_LINK = "<associationFromLink/>";
    public static final String ELEMENT_ASSOCIATION_FROM_LINK_WITH_NS = "<associationFromLink";
    public static final String ELEMENT_SHADOW_OWNER_REFERENCE_SEARCH = "<shadowOwnerReferenceSearch/>";
    public static final String ELEMENT_SHADOW_OWNER_REFERENCE_SEARCH_WITH_NS = "<shadowOwnerReferenceSearch";

    public static String getExpressionString(ExpressionEvaluatorType type, ObjectReferenceType policy) {
        if (ExpressionEvaluatorType.GENERATE.equals(type) && policy != null) {
            return "<generate>\n" +
                    "    <valuePolicyRef oid=\""
                    + policy.getOid() + "\"/>\n" +
                    "</generate>";
        }

        return EXPRESSION_GENERATE;
    }

    public static String getExpressionString(ExpressionEvaluatorType type, Language lang) {
        if (ExpressionEvaluatorType.SCRIPT.equals(type) && !Language.GROOVY.equals(lang)) {
            return "<script>\n"
                    + "    <language>" + lang.getLanguage() + "</language>\n"
                    + "    <code>\n"
                    + "        Insert your script here\n"
                    + "    </code>\n"
                    + "<script>";
        }

        return EXPRESSION_SCRIPT;
    }

    public static String getExpressionString(ExpressionEvaluatorType type) {
        if (type == null) {
            return "";
        }

        switch (type) {
            case AS_IS:
                return EXPRESSION_AS_IS;

            case GENERATE:
                return EXPRESSION_GENERATE;

            case LITERAL:
                return EXPRESSION_LITERAL;

            case PATH:
                return EXPRESSION_PATH;

            case SCRIPT:
                return EXPRESSION_SCRIPT;

            default:
                return "";
        }
    }

    public static ExpressionEvaluatorType getExpressionType(String expression) {
        if (expression.contains(ELEMENT_SHADOW_OWNER_REFERENCE_SEARCH) || expression.contains(ELEMENT_SHADOW_OWNER_REFERENCE_SEARCH_WITH_NS)) {
            return ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH;
        } else if (expression.contains(ELEMENT_AS_IS) || expression.contains(ELEMENT_AS_IS_WITH_NS)) {
            return ExpressionEvaluatorType.AS_IS;
        } else if (expression.contains(ELEMENT_GENERATE) || expression.contains(ELEMENT_GENERATE_WITH_NS)) {
            return ExpressionEvaluatorType.GENERATE;
        } else if (expression.contains(ELEMENT_PATH)) {
            return ExpressionEvaluatorType.PATH;
        } else if (expression.contains(ELEMENT_SCRIPT)) {
            return ExpressionEvaluatorType.SCRIPT;
        } else if (expression.contains(ELEMENT_VALUE)) {
            return ExpressionEvaluatorType.LITERAL;
        } else if (expression.contains(ELEMENT_ASSOCIATION_FROM_LINK) || expression.contains(ELEMENT_ASSOCIATION_FROM_LINK_WITH_NS)) {
            return ExpressionEvaluatorType.ASSOCIATION_FROM_LINK;
        }

        return null;
    }

    public static Language getExpressionLanguage(String expression) {
        if (expression.contains("<language>")) {
            if (expression.contains(Language.VELOCITY.getLanguage())) {
                return Language.VELOCITY;
            } else if (expression.contains(Language.PYTHON.getLanguage())) {
                return Language.PYTHON;
            } else if (expression.contains(Language.JAVASCRIPT.getLanguage())) {
                return Language.JAVASCRIPT;
            } else {
                return Language.GROOVY;
            }
        } else {
            return Language.GROOVY;
        }
    }

    @Nullable
    public static Language converLanguage(String languageValue) {
        if (StringUtils.isEmpty(languageValue)) {
            return null;
        }

        for (Language language : Language.values()) {
            if (languageValue.equals(language.getLanguage()) || languageValue.equalsIgnoreCase(language.name())) {
                return language;
            }
        }

        return Language.GROOVY;
    }

    public static String loadExpression(ExpressionType expression, PrismContext prismContext, Trace LOGGER) {
        if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
            return "";
        }
        List<JAXBElement<?>> evaluators = expression.getExpressionEvaluator();
        try {
            return serializeEvaluators(evaluators, prismContext);
        } catch (SchemaException e) {
            //TODO - how can we show this error to user?
            LoggingUtils.logUnexpectedException(LOGGER, "Could not load expressions from mapping.", e);
            return e.getMessage();
        }
    }

    private static String serializeEvaluators(List<JAXBElement<?>> evaluators, PrismContext prismContext) throws SchemaException {
        if (evaluators.size() == 1) {
            return serialize(evaluators.get(0), prismContext);
        } else {
            StringBuilder sb = new StringBuilder();
            for (JAXBElement<?> element : evaluators) {
                String subElement = serialize(element, prismContext);
                sb.append(subElement).append("\n");
            }
            return sb.toString();
        }
    }

    public static String serialize(JAXBElement<?> element, PrismContext prismContext) throws SchemaException {
        String xml;
        if (element.getValue() instanceof RawType) {
            RawType raw = (RawType) element.getValue();
            RootXNode rootNode = prismContext.xnodeFactory().root(element.getName(), raw.serializeToXNode());
            xml = prismContext.xmlSerializer().serialize(rootNode);
        } else {
            xml = prismContext.xmlSerializer().serialize(element);
        }
        return WebXmlUtil.stripNamespaceDeclarations(xml);
    }

    public static boolean isEmpty(ExpressionType expression) {
        return expression == null || expression.getExpressionEvaluator().isEmpty();
    }

    public static void parseExpressionEvaluators(String xml, ExpressionType expressionObject, PrismContext context) throws SchemaException {
        expressionObject.getExpressionEvaluator().clear();
        if (StringUtils.isNotBlank(xml)) {
            xml = WebXmlUtil.wrapInElement("expression", xml, true);
            LOGGER.trace("Expression to serialize: {}", xml);
            JAXBElement<?> newElement = context.parserFor(xml).xml().parseRealValueToJaxbElement();
            expressionObject.getExpressionEvaluator().addAll(((ExpressionType) (newElement.getValue())).getExpressionEvaluator());
        }
    }

    // TODO move somewhere else? generalize a bit?
    public static RootXNode parseSearchFilter(String data, PrismContext context) throws SchemaException {
        String xml = WebXmlUtil.wrapInElement("root", data, false);
        RootXNode rootXNode = context.parserFor(xml).xml().parseToXNode();
        if (rootXNode.getSubnode() instanceof MapXNode) {
            MapXNode mapXNode = (MapXNode) rootXNode.getSubnode();
            if (mapXNode.size() != 1) {
                throw new SchemaException("Content cannot be parsed as a search filter: " + mapXNode.debugDump());
            }
            return mapXNode.getEntryAsRoot(mapXNode.keySet().iterator().next());
        } else {
            throw new SchemaException("Content cannot be parsed as a search filter: " + DebugUtil.debugDump(rootXNode.getSubnode()));
        }
    }

    public static JAXBElement<?> findFirstEvaluatorByName(ExpressionType expression, QName elementName) {
        if (isEmpty(expression) || elementName == null) {
            return null;
        }
        for (JAXBElement<?> element : expression.getExpressionEvaluator()) {
            if (element != null && element.getName().equals(elementName)) {
                return element;
            }
        }
        return null;
    }

    public static @NotNull List<JAXBElement<?>> findAllEvaluatorsByName(ExpressionType expression, QName elementName) {
        List<JAXBElement<?>> elements = new ArrayList<>();
        if (isEmpty(expression) || elementName == null) {
            return elements;
        }
        for (JAXBElement<?> element : expression.getExpressionEvaluator()) {
            if (element != null && QNameUtil.match(element.getName(), elementName)) {
                elements.add(element);
            }
        }
        return elements;
    }

    public static void removeEvaluatorByName(ExpressionType expression, QName elementName) {
        if (isEmpty(expression) || elementName == null) {
            return;
        }
        expression.getExpressionEvaluator().removeIf(
                element -> element != null && element.getName().equals(elementName));
    }

    public static void removeShadowRefEvaluatorValue(ExpressionType expression, String shadowRefOid, PrismContext prismContext) {
        if (expression == null || StringUtils.isEmpty(shadowRefOid)) {
            return;
        }
        List<JAXBElement<?>> elementList = findAllEvaluatorsByName(expression, SchemaConstants.C_VALUE);
        if (CollectionUtils.isEmpty(elementList)) {
            return;
        }
        boolean removePerformed = false;
        Iterator<JAXBElement<?>> elementIterator = elementList.iterator();
        while (elementIterator.hasNext()) {
            JAXBElement element = elementIterator.next();
            if (element != null && element.getValue() instanceof RawType) {
                RawType raw = (RawType) element.getValue();
                if (raw.isParsed()) {
                    try {
                        if (raw.getParsedRealValue(ShadowAssociationValueType.class) != null) {
                            ShadowAssociationValueType assocValue = raw.getParsedRealValue(ShadowAssociationValueType.class);
                            var shadowRef = ShadowAssociationsUtil.getSingleObjectRefRelaxed(assocValue);
                            if (shadowRef != null && shadowRefOid.equals(shadowRef.getOid())) {
                                elementIterator.remove();
                                break;
                            }
                        }
                    } catch (SchemaException e) {
                        LoggingUtils.logExceptionAsWarning(LOGGER, "Could not remove association value", e);
                    }
                } else {
                    XNode node = raw.getXnode();
                    if (node instanceof MapXNode && ((MapXNode) node).containsKey(SHADOW_REF_KEY)) {
                        XNode shadowRefNodes = ((MapXNode) node).get(SHADOW_REF_KEY);
                        if (shadowRefNodes instanceof MapXNode && shadowRefOid.equals(getShadowRefNodeOid((MapXNode) shadowRefNodes))) {
                            prismContext.xnodeMutator().putToMapXNode((MapXNode) node, SHADOW_REF_KEY, null);
                            removePerformed = true;
                            //todo don't get why while using removeEvaluatorByName no changes are saved
                            //                   removeEvaluatorByName(expression, SchemaConstantsGenerated.C_VALUE);
                        } else if (shadowRefNodes instanceof ListXNode) {
                            Iterator<? extends XNode> it = ((ListXNode) shadowRefNodes).asList().iterator();
                            while (it.hasNext()) {
                                XNode shadowRefNode = it.next();
                                if (shadowRefNode instanceof MapXNode && shadowRefOid.equals(getShadowRefNodeOid((MapXNode) shadowRefNode))) {
                                    it.remove();
                                    removePerformed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (removePerformed) {
                break;
            }
        }
        expression.getExpressionEvaluator().clear();
        expression.getExpressionEvaluator().addAll(elementList);
    }

    public static boolean containsAssociationFromLinkElement(ExpressionType expression) {
        if (expression == null) {
            return false;
        }
        List<JAXBElement<?>> elementList = findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_FROM_LINK);
        if (CollectionUtils.isEmpty(elementList)) {
            return false;
        }
        return true;
    }

    private static String getShadowRefNodeOid(MapXNode shadowRefNode) {
        if (shadowRefNode != null && shadowRefNode.containsKey(SHADOW_OID_KEY)) {
            PrimitiveXNode shadowOidNode = (PrimitiveXNode) shadowRefNode.get(SHADOW_OID_KEY);
            return shadowOidNode != null && shadowOidNode.getValueParser() != null ? shadowOidNode.getValueParser().getStringValue() :
                    (shadowOidNode != null && shadowOidNode.getValue() != null ? (String) shadowOidNode.getValue() : null);

        }
        return "";
    }

    public static JAXBElement<?> createAssociationTargetSearchElement(PrismContext prismContext) {
        JAXBElement<Object> evaluator = new JAXBElement<>(
                SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH,
                Object.class,
                JAXBElement.GlobalScope.class);
        SearchObjectExpressionEvaluatorType searchObjectExpressionEvaluatorType = new SearchObjectExpressionEvaluatorType();

        XNodeFactory factory = prismContext.xnodeFactory();
        Map<QName, XNode> valuesMap = new HashMap<>();
        valuesMap.put(new QName(SchemaConstantsGenerated.NS_QUERY, "path"), factory.primitive());
        valuesMap.put(new QName(SchemaConstantsGenerated.NS_QUERY, "value"), factory.primitive());
        MapXNode values = factory.map(valuesMap);
        MapXNode filterClauseNode = factory.map(new QName(SchemaConstantsGenerated.NS_QUERY, "equal"), values);

        SearchFilterType filterType = new SearchFilterType();
        filterType.setFilterClauseXNode(filterClauseNode);
        searchObjectExpressionEvaluatorType.getFilter().add(filterType);

        evaluator.setValue(searchObjectExpressionEvaluatorType);
        return evaluator;
    }

    public static MapXNode getOrCreateAssociationTargetSearchValues(ExpressionType expression,
            PrismContext prismContext) {
        JAXBElement element = findFirstEvaluatorByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH);
        if (element == null) {
            element = createAssociationTargetSearchElement(prismContext);
        }
        SearchObjectExpressionEvaluatorType evaluator = (SearchObjectExpressionEvaluatorType) element.getValue();
        if (evaluator == null) {
            evaluator = new SearchObjectExpressionEvaluatorType();
        }
        List<SearchFilterType> filterTypeList = evaluator.getFilter();
        if (filterTypeList.isEmpty()) {
            filterTypeList.add(new SearchFilterType());
        }
        MapXNode filterClauseNode = filterTypeList.get(0).getFilterClauseXNode(); // TODO is this correct?
        if (filterClauseNode == null) {
            filterClauseNode = prismContext.xnodeFactory().map();
        }
        if (!filterClauseNode.containsKey(new QName(SchemaConstantsGenerated.NS_QUERY, "equal"))) {
            prismContext.xnodeMutator().putToMapXNode(
                    filterClauseNode,
                    new QName(SchemaConstantsGenerated.NS_QUERY, "equal"), null);
        }
        MapXNode values = (MapXNode) filterClauseNode.get(new QName(SchemaConstantsGenerated.NS_QUERY, "equal"));
        if (values == null) {
            values = prismContext.xnodeFactory().map();        // todo [med] this has no effect on the map node!
        }
        expression.getExpressionEvaluator().add(element);
        return values;
    }

    public static void updateAssociationTargetSearchValue(ExpressionType expression, String newPath, String newValue,
            PrismContext prismContext) throws SchemaException {
        SearchObjectExpressionEvaluatorType associationTargetSearchType = new SearchObjectExpressionEvaluatorType();
        EqualFilter<?> pathFilter = prismContext.queryFactory().createEqual(ItemPath.create(newPath), null, null, prismContext, newValue);

        SearchFilterType filterType = prismContext.getQueryConverter().createSearchFilterType(pathFilter);
        associationTargetSearchType.getFilter().clear(); // TODO is this correct?
        associationTargetSearchType.getFilter().add(filterType);
        JAXBElement<SearchObjectExpressionEvaluatorType> evaluator = new ObjectFactory().createAssociationTargetSearch(associationTargetSearchType);

        removeEvaluatorByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH);
        expression.getExpressionEvaluator().add(evaluator);
    }

    @NotNull
    public static List<ObjectReferenceType> getShadowRefValue(ExpressionType expressionType, PrismContext prismContext) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        if (expressionType != null) {
            for (var associationValue : getAssociationList(expressionType)) {
                var shadowRef = ShadowAssociationsUtil.getSingleObjectRefRelaxed(associationValue);
                if (shadowRef != null) {
                    rv.add(shadowRef.clone());
                }
            }
        }
        return rv;
    }

    public static boolean isShadowRefNodeExists(ExpressionType expression) {
        if (expression == null) {
            return false;
        }
        JAXBElement<?> element = ExpressionUtil.findFirstEvaluatorByName(expression, SchemaConstantsGenerated.C_VALUE);
        if (element == null) {
            return false;
        }
        if (element.getValue() instanceof RawType) {
            RawType raw = (RawType) element.getValue();
            PrismValue prismValue = raw.getAlreadyParsedValue();
            if (prismValue instanceof PrismContainerValue
                    && ((PrismContainerValue<?>) prismValue).getComplexTypeDefinition() != null
                    && ShadowAssociationValueType.class.equals(
                    ((PrismContainerValue<?>) prismValue).getComplexTypeDefinition().getCompileTimeClass())) {
                return true;
            }
        } else if (element.getValue() instanceof ShadowAssociationValueType) {
            return true;
        }
        return false;
    }

    /**
     * ((PrismContainerValue)prismValue).getComplexTypeDefinition()
     *
     * @return Immutable list of associations.
     */
    @NotNull
    private static List<ShadowAssociationValueType> getAssociationList(ExpressionType expression) {
        if (expression == null) {
            return Collections.emptyList();
        }
        List<ShadowAssociationValueType> rv = new ArrayList<>();
        try {
            for (JAXBElement<?> evaluatorJaxbElement : expression.getExpressionEvaluator()) {
                if (QNameUtil.match(evaluatorJaxbElement.getName(), SchemaConstantsGenerated.C_VALUE)) {
                    Object evaluatorValue = evaluatorJaxbElement.getValue();
                    if (evaluatorValue instanceof ShadowAssociationValueType) {
                        rv.add((ShadowAssociationValueType) evaluatorValue);
                    } else if (evaluatorValue instanceof RawType) {
                        rv.add(((RawType) evaluatorValue).getParsedRealValue(ShadowAssociationValueType.class));
                    } else if (evaluatorValue == null) {
                        // just ignore it
                    } else {
                        throw new SchemaException("Expected ShadowAssociationValueType, got " + MiscUtil.getClass(evaluatorValue));
                    }
                }
            }
        } catch (SchemaException e) {
            throw new SystemException(e.getMessage(), e);   // todo
        }
        return Collections.unmodifiableList(rv);
    }

    public static void addShadowRefEvaluatorValue(ExpressionType expression, String oid) {
        if (StringUtils.isNotEmpty(oid)) {
            expression.getExpressionEvaluator().add(
                    new JAXBElement<>(SchemaConstants.C_VALUE, ShadowAssociationValueType.class,
                            ShadowAssociationsUtil.createSingleRefRawValue(
                                    ItemName.from("", "TODO"), // FIXME provide association name here
                                    oid)));
        } else {
            expression.getExpressionEvaluator().add(
                    new JAXBElement<>(SchemaConstants.C_VALUE, ShadowAssociationValueType.class,
                            new ShadowAssociationValueType()));
        }
    }

    public static void updateShadowRefEvaluatorValue(ExpressionType expression, List<ObjectReferenceType> values) {
        if (expression == null) {
            expression = new ExpressionType();      // TODO ??? this is thrown away
        }
        removeEvaluatorByName(expression, SchemaConstantsGenerated.C_VALUE);
        for (ObjectReferenceType value : values) {
            expression.expressionEvaluator(
                    new JAXBElement<>(
                            SchemaConstantsGenerated.C_VALUE,
                            ShadowAssociationValueType.class,
                            ShadowAssociationsUtil.createSingleRefRawValue(
                                    ItemName.from("", "TODO"), // FIXME provide association name here
                                    value)));
        }
    }

    public static @NotNull List<String> getLiteralExpressionValues(ExpressionType expression) throws SchemaException {
        List<String> values = new ArrayList<>();
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_VALUE);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof RawType) {
                RawType raw = (RawType) element.getValue();
                if (raw != null) {
                    if (raw.getXnode() != null && raw.getXnode() instanceof PrimitiveXNode) {
                        PrimitiveXNode<?> valueNode = (PrimitiveXNode<?>) raw.getXnode();
                        if (valueNode != null && valueNode.getValue() != null) {
                            values.add(valueNode.getValue().toString());
                        } else if (valueNode.getValueParser() != null) {
                            values.add(valueNode.getValueParser().getStringValue());
                        }
                    } else if (raw.getParsedRealValue(String.class) != null) {
                        values.add(raw.getParsedRealValue(String.class));
                    }
                }
            }
        }
        return values;
    }

    public static ScriptExpressionEvaluatorType getScriptExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_SCRIPT);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof ScriptExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static ItemPathType getPathExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_PATH);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof ItemPathType path) {
                return path;
            }
        }
        return null;
    }

    public static AssociationFromLinkExpressionEvaluatorType getAssociationFromLinkExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_FROM_LINK);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof AssociationFromLinkExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static ShadowOwnerReferenceSearchExpressionEvaluatorType getShadowOwnerExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_SHADOW_OWNER_REFERENCE_SEARCH);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static GenerateExpressionEvaluatorType getGenerateExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_GENERATE);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof GenerateExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static AssociationSynchronizationExpressionEvaluatorType getAssociationSynchronizationExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof AssociationSynchronizationExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static AssociationConstructionExpressionEvaluatorType getAssociationConstructionExpressionValue(ExpressionType expression) throws SchemaException {
        List<JAXBElement<?>> elements = ExpressionUtil.findAllEvaluatorsByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION);
        for (JAXBElement<?> element : elements) {
            if (element.getValue() instanceof AssociationConstructionExpressionEvaluatorType evaluator) {
                return evaluator;
            }
        }
        return null;
    }

    public static void updateLiteralExpressionValue(ExpressionType expression, List<String> values, PrismContext prismContext) {
        if (expression == null) {
            expression = new ExpressionType();      // TODO ??? this is thrown away
        }
        removeEvaluatorByName(expression, SchemaConstantsGenerated.C_VALUE);
        for (String value : values) {
            PrimitiveXNode<String> newValueNode = prismContext.xnodeFactory().primitive(value);
            RawType raw = new RawType(newValueNode.frozen());
            JAXBElement element = new JAXBElement<>(SchemaConstantsGenerated.C_VALUE, RawType.class, raw);
            expression.expressionEvaluator(element);
        }
    }

    public static ExpressionType updateScriptExpressionValue(
            ExpressionType expression, ScriptExpressionEvaluatorType evaluator) throws SchemaException {
        return updateExpressionEvaluator(
                expression, evaluator, ScriptExpressionEvaluatorType.class, SchemaConstantsGenerated.C_SCRIPT);
    }

    public static ExpressionType updatePathEvaluator(ExpressionType expression, ItemPathType path) throws SchemaException {
        return updateExpressionEvaluator(
                expression, path, ItemPathType.class, SchemaConstantsGenerated.C_PATH);
    }

    public static ExpressionType updateAssociationFromLinkExpressionValue(
            ExpressionType expression, AssociationFromLinkExpressionEvaluatorType evaluator) throws SchemaException {
        return updateExpressionEvaluator(
                expression, evaluator, AssociationFromLinkExpressionEvaluatorType.class, SchemaConstantsGenerated.C_ASSOCIATION_FROM_LINK);
    }

    public static ExpressionType updateShadowOwnerReferenceSearchExpressionValue(
            ExpressionType expression, ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator) throws SchemaException {
        return updateExpressionEvaluator(
                expression, evaluator, ShadowOwnerReferenceSearchExpressionEvaluatorType.class, SchemaConstantsGenerated.C_SHADOW_OWNER_REFERENCE_SEARCH);
    }

    private static <E extends Object> ExpressionType updateExpressionEvaluator(
            ExpressionType expression, E evaluator, Class<E> clazz, ItemName evaluatorName) throws SchemaException {
        if (expression == null) {
            expression = new ExpressionType();
        }

        removeEvaluatorByName(expression, evaluatorName);

        if (evaluator == null) {
            return null;
        }

        JAXBElement<E> element =
                new JAXBElement<>(evaluatorName, clazz, evaluator);
        expression.expressionEvaluator(element);
        return expression;
    }

    public static void updateGenerateExpressionValue(
            ExpressionType expression, GenerateExpressionEvaluatorType evaluator) throws SchemaException {
        updateExpressionEvaluator(
                expression, evaluator, GenerateExpressionEvaluatorType.class, SchemaConstantsGenerated.C_GENERATE);
    }

    public static void updateAssociationSynchronizationExpressionValue(
            ExpressionType expression, AssociationSynchronizationExpressionEvaluatorType evaluator) throws SchemaException {
        updateExpressionEvaluator(
                expression, evaluator, AssociationSynchronizationExpressionEvaluatorType.class, SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION);
    }

    public static void updateAssociationConstructionExpressionValue(
            ExpressionType expression, AssociationConstructionExpressionEvaluatorType evaluator) throws SchemaException {
        updateExpressionEvaluator(
                expression, evaluator, AssociationConstructionExpressionEvaluatorType.class, SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION);
    }

    public static void addAsIsExpressionValue(ExpressionType expression) {
        if (expression == null) {
            return;
        }

        expression.getExpressionEvaluator().clear();

        AsIsExpressionEvaluatorType evaluator = new AsIsExpressionEvaluatorType();
        JAXBElement<AsIsExpressionEvaluatorType> element =
                new JAXBElement<>(SchemaConstantsGenerated.C_AS_IS, AsIsExpressionEvaluatorType.class, evaluator);
        expression.expressionEvaluator(element);
    }

    public static MapXNode getAssociationTargetSearchFilterValuesMap(ExpressionType expression) {
        if (expression == null) {
            return null;
        }
        JAXBElement element = ExpressionUtil.findFirstEvaluatorByName(expression, SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH);
        if (element != null && element.getValue() != null && element.getValue() instanceof SearchObjectExpressionEvaluatorType) {
            List<SearchFilterType> filters = ((SearchObjectExpressionEvaluatorType) element.getValue()).getFilter();
            if (filters.isEmpty()) {
                return null;
            }
            MapXNode filterValue = filters.get(0).getFilterClauseXNode(); // TODO is this correct?
            return filterValue != null && filterValue.containsKey(new QName(SchemaConstantsGenerated.NS_QUERY, "equal")) ?
                    (MapXNode) filterValue.get(new QName(SchemaConstantsGenerated.NS_QUERY, "equal")) : null;

        }
        return null;
    }

    public static String getTargetSearchExpPathValue(ExpressionType expression) {
        if (expression == null) {
            return null;
        }
        MapXNode filterNodeMap = getAssociationTargetSearchFilterValuesMap(expression);
        if (filterNodeMap == null || !filterNodeMap.containsKey(new QName(SchemaConstantsGenerated.NS_QUERY, "path"))) {
            return null;
        }
        PrimitiveXNode<ItemPathType> pathValue = (PrimitiveXNode<ItemPathType>) filterNodeMap.get(
                new QName(SchemaConstantsGenerated.NS_QUERY, "path"));
        return pathValue != null && pathValue.getValue() != null ? pathValue.getValue().toString() : null;
    }

    public static String getTargetSearchExpValue(ExpressionType expression) {
        if (expression == null) {
            return null;
        }
        MapXNode filterNodeMap = getAssociationTargetSearchFilterValuesMap(expression);
        if (filterNodeMap == null || !filterNodeMap.containsKey(new QName(SchemaConstantsGenerated.NS_QUERY, "value"))) {
            return null;
        }
        XNode node = filterNodeMap.get(new QName(SchemaConstantsGenerated.NS_QUERY, "value"));
        if (node instanceof ListXNode) {
            if (((ListXNode) node).size() > 0) {
                node = ((ListXNode) node).get(0);
            }
        }
        PrimitiveXNode valueNode = (PrimitiveXNode) node;
        if (valueNode == null) {
            return null;
        }
        if (valueNode.getValueParser() != null) {
            return valueNode.getValueParser().getStringValue();
        } else {
            return valueNode.getValue() != null ? valueNode.getValue().toString() : null;
        }
    }
}

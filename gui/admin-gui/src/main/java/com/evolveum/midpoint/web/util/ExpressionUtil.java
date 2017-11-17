/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;

/**
 *  @author shood
 * */
public class ExpressionUtil {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);

	public static enum ExpressionEvaluatorType{
        LITERAL,
        AS_IS,
        PATH,
        SCRIPT,
        GENERATE
    }

    public static enum Language{
        GROOVY("http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy"),
        XPATH("http://www.w3.org/TR/xpath/"),
        JAVASCRIPT("http://midpoint.evolveum.com/xml/ns/public/expression/language#ECMAScript");

        protected String language;

        Language(String language){
            this.language = language;
        }

        public String getLanguage() {
            return language;
        }
    }

    public static final String SCRIPT_START_NS = "<c:script xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">";
    public static final String SCRIPT_END_NS = "</c:script>";
    public static final String CODE_START_NS = "<c:code>";
    public static final String CODE_END_NS = "</c:code>";
    public static final String VALUE_START_NS = "<c:value xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">";
    public static final String VALUE_END_NS = "</c:value>";
    public static final String PATH_START_NS = "<c:path xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">";
    public static final String PATH_END_NS = "</c:path>";

    public static final String EXPRESSION_SCRIPT =
                    "<script>\n" +
                    "    <code>\n" +
                    "        Insert your script here\n" +
                    "    </code>\n" +
                    "</script>";

    public static final String EXPRESSION_LITERAL = "<value>Insert value(s) here</value>";
    public static final String EXPRESSION_AS_IS = "<asIs/>";
    public static final String EXPRESSION_PATH = "<path>Insert path here</path>";
    public static final String EXPRESSION_GENERATE =
                    "<generate>\n" +
                    //"    <valuePolicyRef oid=\"Insert value policy oid\"/>\n" +
                    "</generate>";

    public static final String ELEMENT_SCRIPT = "</script>";
    public static final String ELEMENT_GENERATE = "</generate>";
    public static final String ELEMENT_GENERATE_WITH_NS = "<generate";
    public static final String ELEMENT_PATH = "</path>";
    public static final String ELEMENT_VALUE = "</value>";
    public static final String ELEMENT_AS_IS = "<asIs/>";
    public static final String ELEMENT_AS_IS_WITH_NS = "<asIs";

    public static String getExpressionString(ExpressionEvaluatorType type, ObjectReferenceType policy){
        if(ExpressionEvaluatorType.GENERATE.equals(type) && policy != null){
            StringBuilder sb = new StringBuilder();
            sb.append("<generate>\n" +
                    "    <valuePolicyRef oid=\"").append(policy.getOid()).append("\"/>\n" +
                    "</generate>");

            return sb.toString();
        }

        return EXPRESSION_GENERATE;
    }

    public static String getExpressionString(ExpressionEvaluatorType type, Language lang){
        if(ExpressionEvaluatorType.SCRIPT.equals(type) && !Language.GROOVY.equals(lang)){
            StringBuilder sb = new StringBuilder();
            sb.append("<script>\n");
            sb.append("    <language>").append(lang.getLanguage()).append("</language>\n");
            sb.append("    <code>\n" +
                    "        Insert your script here\n" +
                    "    </code>\n" +
                    "<script>");

            return sb.toString();
        }

        return EXPRESSION_SCRIPT;
    }

    public static String getExpressionString(ExpressionEvaluatorType type){
        if(type == null){
            return "";
        }

        switch(type){
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

    public static ExpressionEvaluatorType getExpressionType(String expression){
        if(expression.contains(ELEMENT_AS_IS) || expression.contains(ELEMENT_AS_IS_WITH_NS)){
            return ExpressionEvaluatorType.AS_IS;
        } else if(expression.contains(ELEMENT_GENERATE) || expression.contains(ELEMENT_GENERATE_WITH_NS)){
            return ExpressionEvaluatorType.GENERATE;
        } else if(expression.contains(ELEMENT_PATH)){
            return ExpressionEvaluatorType.PATH;
        } else if(expression.contains(ELEMENT_SCRIPT)){
            return ExpressionEvaluatorType.SCRIPT;
        } else if(expression.contains(ELEMENT_VALUE)){
            return ExpressionEvaluatorType.LITERAL;
        }

        return null;
    }

    public static Language getExpressionLanguage(String expression){
        if(expression.contains("<language>")){
            if(expression.contains(Language.XPATH.getLanguage())){
                return Language.XPATH;
            } else if(expression.contains(Language.JAVASCRIPT.getLanguage())) {
                return Language.JAVASCRIPT;
            } else {
                return Language.GROOVY;
            }
        } else {
            return Language.GROOVY;
        }
    }

    public static String addNamespaces(String expression, ExpressionEvaluatorType type){
        String newExpression = expression;

        if(ExpressionEvaluatorType.PATH.equals(type)){
            newExpression = newExpression.replaceAll("<path>", PATH_START_NS);
            newExpression = newExpression.replaceAll("</path>", PATH_END_NS);
        } else if(ExpressionEvaluatorType.LITERAL.equals(type)){
            newExpression = newExpression.replaceAll("<value>", VALUE_START_NS);
            newExpression = newExpression.replaceAll("</value>", VALUE_END_NS);
        } else if(ExpressionEvaluatorType.SCRIPT.equals(type)){
            newExpression = newExpression.replaceAll("<code>", CODE_START_NS);
            newExpression = newExpression.replaceAll("</code>", CODE_END_NS);
            newExpression = newExpression.replaceAll("<script>", SCRIPT_START_NS);
            newExpression = newExpression.replaceAll("</script>", SCRIPT_END_NS);
        }

        return newExpression;
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
			LoggingUtils.logUnexpectedException(LOGGER, "Could not load expressions from mapping.", e, e.getStackTrace());
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

	private static String serialize(JAXBElement<?> element, PrismContext prismContext) throws SchemaException {
		String xml;
		if (element.getValue() instanceof RawType) {
			RawType raw = (RawType) element.getValue();
			RootXNode rootNode = new RootXNode(element.getName(), raw.serializeToXNode());
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
			LOGGER.info("Expression to serialize: {}", xml);
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

	public static JAXBElement findEvaluatorByName(ExpressionType expression, QName elementName){
        if (isEmpty(expression) || elementName == null){
            return null;
        }
        for (JAXBElement<?> element : expression.getExpressionEvaluator()){
            if (element != null && element.getName().equals(elementName)){
                return element;
            }
        }
        return null;
    }

}

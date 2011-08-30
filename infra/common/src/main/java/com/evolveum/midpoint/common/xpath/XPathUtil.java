/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common.xpath;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.ExpressionCodeHolder;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.util.MapXPathVariableResolver;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.xpath.MidPointXPathFunctionResolver;
import com.evolveum.midpoint.util.xpath.functions.CapitalizeFunction;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Vilo Repan
 */
public class XPathUtil {

    private static final Trace logger = TraceManager.getTrace(XPathUtil.class);

    public static Object evaluateExpression(Map<QName, Variable> variables, ExpressionCodeHolder expressionHolder, QName returnType) {
        return new XPathUtil().evaluateExpr(variables, expressionHolder, returnType);
    }

    protected Object evaluateExpr(Map<QName, Variable> variables, ExpressionCodeHolder expressionHolder, QName returnType) {
        logger.trace("Expression '{}' will be evaluated in context: variables = \n'{}' and namespaces = \n'{}'", new Object[]{expressionHolder.getExpressionAsString(), variables.values(), expressionHolder.getNamespaceMap()});

        Validate.notNull(expressionHolder);
//        Validate.notNull(variables);
//        Validate.notEmpty(variables);

        try {
            XPath xpath = setupXPath(variables, expressionHolder.getNamespaceMap());
            XPathExpression expr = xpath.compile(expressionHolder.getExpressionAsString());
            //TODO: we will probably need to update interface and add parameter nodeForEval - node on which do the xpath evaluation
            Node nodeForEval = variables.get(SchemaConstants.I_ACCOUNT) == null ? null : (Node) variables.get(SchemaConstants.I_ACCOUNT).getObject();
            if (null == nodeForEval) {
                nodeForEval = variables.get(SchemaConstants.I_USER) == null ? null : (Node) variables.get(SchemaConstants.I_USER).getObject();
            }
            Object evaluatedExpression = expr.evaluate(nodeForEval, returnType);
            logger.trace("Expression '{}' was evaluated to '{}' ", new Object[]{expressionHolder.getExpressionAsString(), evaluatedExpression});
            return evaluatedExpression;
        } catch (XPathExpressionException ex) {
            //TODO: implement properly
            throw new IllegalArgumentException("Expression (" + expressionHolder.getExpressionAsString() + ") evaluation failed", ex);
            //return "";
        }
    }

    protected XPath setupXPath() {
        //Note: probably no validation required
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        return xpath;
    }

    protected XPath setupXPath(Map<QName, Variable> variables, Map<String, String> namespaces) {
        //Note: probably no validation required
        XPath xpath = setupXPath();
        if (null != variables) {
            XPathVariableResolver variableResolver = new MapXPathVariableResolver(variables);
            xpath.setXPathVariableResolver(variableResolver);
        }
        xpath.setNamespaceContext(new MidPointNamespaceContext(namespaces));
        MidPointXPathFunctionResolver fc = new MidPointXPathFunctionResolver();
        fc.registerFunction(new QName("http://midpoint.evolveum.com/custom", "capitalize"), new CapitalizeFunction());
        xpath.setXPathFunctionResolver(fc);
        return xpath;
    }

    public NodeList matchedNodesByXPath(XPathHolder xpathType, Map<QName, Variable> variables, Node domObject) throws XPathExpressionException {
        Validate.notNull(xpathType, "xpathType is null");
        Validate.notNull(domObject, "domObject is null");
        try {
            XPath xpath = setupXPath(variables, xpathType.getNamespaceMap());
            XPathExpression expr = xpath.compile(xpathType.getXPath());
            NodeList result = (NodeList) expr.evaluate(domObject, XPathConstants.NODESET);

            return result;
        } catch (XPathExpressionException ex) {
            throw new IllegalStateException(ex);
        }

    }
//    protected static NodeList matchedNodesByXPath(Document doc, XPath xpath, String xpathString, Map namespaces) throws XPathExpressionException {
//
//        //we have to register all namespaces required by xpath,
//        //for every change it could be different set of namespaces
//        xpath.setNamespaceContext(new MidPointNamespaceContext(namespaces));
//        XPathExpression expr = xpath.compile(xpathString);
//        //Note: Here we are doing xpath evaluation not on document, but on the first child = root parentNode
//        NodeList result = (NodeList) expr.evaluate(doc.getFirstChild(), XPathConstants.NODESET);
//
//        return result;
//
//    }
	
		public static void createNodesDefinedByXPath(Document doc,
			XPathHolder xpathType) {
		Validate.notNull(doc, "Provided parameter doc was null");
		Validate.notNull(xpathType, "Provided parameter xpathType was null");

		List<XPathSegment> segments = xpathType.toSegments();

		if (null != segments) {
			// first we will find first element defined by xpath that does not
			// exists
			Node docChildToWhichAppend = (Element) doc.getFirstChild();
			int i;
			for (i = 0; i < segments.size(); i++) {
				NodeList nodes;
				try {
					XPathHolder path = null;
					path = new XPathHolder(segments.subList(0, i + 1));
					nodes = (new XPathUtil()).matchedNodesByXPath(path, null,
							doc.getFirstChild());
				} catch (XPathExpressionException ex) {
					// TODO: exception translation
					throw new IllegalArgumentException(ex);
				}

				if ((null == nodes) || (nodes.getLength() == 0)) {
					break;
				}
				docChildToWhichAppend = docChildToWhichAppend.getFirstChild();
			}

			// if there is at least element that does not exists (defined by
			// xpath), then create it and all remaining elements from xpath
			if (i < segments.size()) {
				Element parentElement = null;
				Element element = null;
				Element child;

				for (int j = i; j < segments.size(); j++) {
					QName qname = segments.get(j).getQName();
					if (null == parentElement) {
						parentElement = doc.createElementNS(
								qname.getNamespaceURI(), qname.getLocalPart());
						element = parentElement;
					} else {
						child = doc.createElementNS(qname.getNamespaceURI(),
								qname.getLocalPart());
						element.appendChild(child);
						element = child;
					}
				}
				docChildToWhichAppend.appendChild(parentElement);
			}
		}

	}
}

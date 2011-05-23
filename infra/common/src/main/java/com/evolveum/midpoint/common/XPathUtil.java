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

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.MapXPathVariableResolver;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;
import com.evolveum.midpoint.xpath.MidPointNamespaceContext;
import com.evolveum.midpoint.xpath.MidPointXPathFunctionResolver;
import com.evolveum.midpoint.xpath.functions.CapitalizeFunction;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Vilo Repan
 */
public class XPathUtil {

    private static final Trace logger = TraceManager.getTrace(XPathUtil.class);

    public static Object evaluateExpression(Map<QName, Variable> variables, ExpressionHolder expressionHolder, QName returnType) {
        return new XPathUtil().evaluateExpr(variables, expressionHolder, returnType);
    }

    protected Object evaluateExpr(Map<QName, Variable> variables, ExpressionHolder expressionHolder, QName returnType) {
        logger.trace("Expression '{}' will be evaluated in context: variables = '{}' and namespaces = '{}'", new Object[]{expressionHolder.getExpressionAsString(), variables.values(), expressionHolder.getNamespaceMap()});

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

    public NodeList matchedNodesByXPath(XPathType xpathType, Map<QName, Variable> variables, Node domObject) throws XPathExpressionException {
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
}

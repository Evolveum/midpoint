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

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.util.XPathUtil;
import com.evolveum.midpoint.util.jaxb.JAXBUtil;
import com.evolveum.midpoint.web.bean.XPathVariableBean;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Katuska
 */
@Controller("xpathDebugPageController")
@Scope("session")
public class XPathDebugPageController implements Serializable {

	public static final String PAGE_NAVIGATION_XPATH_DEBUG = "/config/xpathDebug?faces-redirect=true";
	private static final long serialVersionUID = 7295076387943631763L;
	private static final Trace TRACE = TraceManager.getTrace(XPathDebugPageController.class);
	@Autowired
	private transient ModelPortType port;
	private String expresion;
	private List<SelectItem> type;
	private XPathVariableBean variable1;
	private XPathVariableBean variable2;
	private XPathVariableBean variable3;
	private XPathVariableBean variable4;
	private List<SelectItem> returnTypeList;
	private String returnType;
	private List<XPathVariableBean> variables = new ArrayList<XPathVariableBean>();
	private String result;

	public String prepareXpathDebugPage() {
		if (variable1 == null) {
			variable1 = new XPathVariableBean();
		}
		if (variable2 == null) {
			variable2 = new XPathVariableBean();
		}
		if (variable3 == null) {
			variable3 = new XPathVariableBean();
		}
		if (variable4 == null) {
			variable4 = new XPathVariableBean();
		}
		return PAGE_NAVIGATION_XPATH_DEBUG;

	}

	public ExpressionHolder getExpressionHolderFromExpresion() {
		TRACE.debug("getExpressionHolder start");

		Document doc = DOMUtil.getDocument();
		Element element = doc.createElement("valueExpresion");
		element.setTextContent(expresion);
		ExpressionHolder expressionHolder = new ExpressionHolder(element);
		TRACE.debug("expression holder: {}", expressionHolder.getFullExpressionAsString());
		TRACE.debug("getExpressionHolder end");
		return expressionHolder;
	}

	public QName getQNameForVariable(String variable) {
		TRACE.debug("getQNameForVariable start");
		ExpressionHolder expressionHolder = getExpressionHolderFromExpresion();
		Map<String, String> namespaceMap = expressionHolder.getNamespaceMap();

		if (variable.contains(":")) {
			String[] variableNS = variable.split(":");
			String namespace = namespaceMap.get(variableNS[0]);
			return new QName(namespace, variableNS[1]);
		} else {
			QName qname = new QName(variable);
			return qname;
		}
	}

	public Map<QName, Variable> getVariableValue() throws JAXBException {
		TRACE.debug("getVariableValue start");
		variables.add(variable1);
		variables.add(variable2);
		variables.add(variable3);
		variables.add(variable4);
		Map<QName, Variable> variableMap = new HashMap<QName, Variable>();
		for (XPathVariableBean variable : variables) {
			if (StringUtils.isNotEmpty(variable.getVariableName())) {
				if (variable.getType().equals("Object")) {
					try {
						ObjectContainerType objectContainer = port.getObject(variable.getValue(),
								new PropertyReferenceListType());
						ObjectType objectType = objectContainer.getObject();
						// Variable only accepts String or Node, but here we
						// will get a JAXB object. Need to convert it.
						Element jaxbToDom = JAXBUtil.jaxbToDom(objectType, SchemaConstants.I_OBJECT, null);
						// TODO: May need to add xsi:type attribute here
						variableMap.put(getQNameForVariable(variable.getVariableName()), new Variable(
								jaxbToDom, false));
					} catch (FaultMessage ex) {
						TRACE.error("Failed to get variable value");
						TRACE.error("Exception was: ", ex.getFaultInfo().getMessage());
					}
				}
				if (variable.getType().equals("String")) {
					variableMap.put(getQNameForVariable(variable.getVariableName()),
							new Variable(variable.getValue(), false));
				}
			}
		}
		// logger.info("variable value {}",
		// variableMap.get(QNameUtil.uriToQName("http://xxx.com/")));
		// logger.info("getVariableValue end");
		return variableMap;
	}

	public String evaluate() throws JAXBException {
		TRACE.debug("evaluate start");
		if (expresion == null || expresion.isEmpty()) {
			FacesUtils.addErrorMessage("Expresion cannot be null.");
			return null;
		}

		ExpressionHolder expressionHolder = getExpressionHolderFromExpresion();
		if (returnType.equals("Boolean")) {
			Boolean boolResult = (Boolean) XPathUtil.evaluateExpression(getVariableValue(), expressionHolder,
					XPathConstants.BOOLEAN);
			result = String.valueOf(boolResult);
		}
		if (returnType.equals("Number")) {
			Double doubleResult = (Double) XPathUtil.evaluateExpression(getVariableValue(), expressionHolder,
					XPathConstants.NUMBER);
			result = String.valueOf(doubleResult);
		}
		if (returnType.equals("String") || returnType.equals("DomObjectModel")) {
			result = (String) XPathUtil.evaluateExpression(getVariableValue(), expressionHolder,
					XPathConstants.STRING);
		}

		if (returnType.equals("Node")) {
			Node nodeResult = (Node) XPathUtil.evaluateExpression(getVariableValue(), expressionHolder,
					XPathConstants.NODE);
			result = DOMUtil.printDom(nodeResult).toString();
		}
		if (returnType.equals("NodeList")) {
			NodeList nodeListResult = (NodeList) XPathUtil.evaluateExpression(getVariableValue(),
					expressionHolder, XPathConstants.NODESET);
			StringBuffer strBuilder = new StringBuffer();
			for (int i = 0; i < nodeListResult.getLength(); i++) {
				strBuilder.append(DOMUtil.printDom(nodeListResult.item(i)));
			}
			result = strBuilder.toString();
		}

		TRACE.debug("result is: {}", result);
		TRACE.debug("evaluate end");
		return null;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public List<SelectItem> getType() {
		if (type == null) {
			type = new ArrayList<SelectItem>();
			type.add(new SelectItem("Object"));
			type.add(new SelectItem("String"));
		}
		return type;
	}

	public void setType(List<SelectItem> type) {
		this.type = type;
	}

	public String getExpresion() {
		return expresion;
	}

	public void setExpresion(String expresion) {
		this.expresion = expresion;
	}

	public XPathVariableBean getVariable1() {
		return variable1;
	}

	public void setVariable1(XPathVariableBean variable1) {
		this.variable1 = variable1;
	}

	public XPathVariableBean getVariable2() {
		return variable2;
	}

	public void setVariable2(XPathVariableBean variable2) {
		this.variable2 = variable2;
	}

	public XPathVariableBean getVariable3() {
		return variable3;
	}

	public void setVariable3(XPathVariableBean variable3) {
		this.variable3 = variable3;
	}

	public XPathVariableBean getVariable4() {
		return variable4;
	}

	public void setVariable4(XPathVariableBean variable4) {
		this.variable4 = variable4;
	}

	public List<XPathVariableBean> getVariables() {
		return variables;
	}

	public void setVariables(List<XPathVariableBean> variables) {
		this.variables = variables;
	}

	public List<SelectItem> getReturnTypeList() {
		if (type == null) {

			returnTypeList = new ArrayList<SelectItem>();
			returnTypeList.add(new SelectItem("String"));
			returnTypeList.add(new SelectItem("Number"));
			returnTypeList.add(new SelectItem("Node"));
			returnTypeList.add(new SelectItem("NodeList"));
			returnTypeList.add(new SelectItem("Boolean"));
			returnTypeList.add(new SelectItem("DomObjectModel"));
		}
		return returnTypeList;
	}

	public void setReturnTypeList(List<SelectItem> returnTypeList) {
		this.returnTypeList = returnTypeList;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
}

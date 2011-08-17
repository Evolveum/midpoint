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
package com.evolveum.midpoint.web.controller.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.xpath.XPathConstants;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.xpath.XPathUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.ExpressionHolder;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.BrowserBean;
import com.evolveum.midpoint.web.bean.XPathVariableBean;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * 
 * @author Katuska
 */
@Controller("debugXPath")
@Scope("session")
public class XPathDebugController implements Serializable {

	public static final String PAGE_NAVIGATION_XPATH_DEBUG = "/config/debugXPath?faces-redirect=true";
	private static final long serialVersionUID = 7295076387943631763L;
	private static final String PARAM_VARIABLE_NAME = "variableName";
	private static final String PARAM_OBJECT_OID = "objectOid";
	private static final Trace TRACE = TraceManager.getTrace(XPathDebugController.class);
	private static final List<SelectItem> returnTypes = new ArrayList<SelectItem>();
	private static final List<SelectItem> types = new ArrayList<SelectItem>();
	static {
		returnTypes.add(new SelectItem("String"));
		returnTypes.add(new SelectItem("Number"));
		returnTypes.add(new SelectItem("Node"));
		returnTypes.add(new SelectItem("NodeList"));
		returnTypes.add(new SelectItem("Boolean"));
		returnTypes.add(new SelectItem("DomObjectModel"));

		types.add(new SelectItem("Object"));
		types.add(new SelectItem("String"));
	}
	@Autowired
	private transient ModelPortType port;
	private String expression;
	private List<XPathVariableBean> variables = new ArrayList<XPathVariableBean>();
	private boolean selectAll;
	private String returnType;
	private String result;
	// browsing
	private String variableName;
	private boolean showBrowser;
	private BrowserBean browser;

	public String cleanupController() {
		expression = null;
		variables = null;
		selectAll = false;
		returnType = null;
		result = null;

		return PAGE_NAVIGATION_XPATH_DEBUG;
	}

	private ExpressionHolder getExpressionHolderFromExpresion() {
		TRACE.debug("getExpressionHolder start");

		Document doc = DOMUtil.getDocument();
		Element element = doc.createElement("valueExpresion");
		element.setTextContent(expression);
		ExpressionHolder expressionHolder = new ExpressionHolder(element);
		TRACE.debug("expression holder: {}", expressionHolder.getFullExpressionAsString());
		TRACE.debug("getExpressionHolder end");
		return expressionHolder;
	}

	private QName getQNameForVariable(String variable) {
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

	private Map<QName, Variable> getVariableValue() throws JAXBException {
		TRACE.debug("getVariableValue start");
		Map<QName, Variable> variableMap = new HashMap<QName, Variable>();
		for (XPathVariableBean variable : getVariables()) {
			if (StringUtils.isNotEmpty(variable.getVariableName())) {
				if (variable.getType().equals("Object")) {
					try {
						ObjectType objectType = port.getObject(
								ObjectTypes.OBJECT.getObjectTypeUri(),
								variable.getValue(),
								new PropertyReferenceListType(), new Holder<OperationResultType>(
										new OperationResultType()));
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

	public String evaluate() {
		TRACE.debug("evaluate start");
		if (expression == null || expression.isEmpty()) {
			FacesUtils.addErrorMessage("Expresion cannot be null.");
			return null;
		}

		try {
			ExpressionHolder expressionHolder = getExpressionHolderFromExpresion();
			if (returnType.equals("Boolean")) {
				Boolean boolResult = (Boolean) XPathUtil.evaluateExpression(getVariableValue(),
						expressionHolder, XPathConstants.BOOLEAN);
				result = String.valueOf(boolResult);
			}
			if (returnType.equals("Number")) {
				Double doubleResult = (Double) XPathUtil.evaluateExpression(getVariableValue(),
						expressionHolder, XPathConstants.NUMBER);
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
		} catch (JAXBException ex) {
			FacesUtils.addErrorMessage("JAXB error occured, reason: " + ex.getMessage(), ex);
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

	public List<SelectItem> getTypes() {
		return types;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public List<XPathVariableBean> getVariables() {
		if (variables == null) {
			variables = new ArrayList<XPathVariableBean>();
			addVariablePerformed();
		}
		return variables;
	}

	public List<SelectItem> getReturnTypes() {
		return returnTypes;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public void addVariablePerformed() {
		XPathVariableBean variable = new XPathVariableBean();
		variable.setType("String");

		getVariables().add(variable);
	}

	public void deleteVariablesPerformed() {
		List<XPathVariableBean> toDelete = new ArrayList<XPathVariableBean>();
		for (XPathVariableBean bean : getVariables()) {
			if (bean.isSelected()) {
				toDelete.add(bean);
			}
		}

		getVariables().removeAll(toDelete);
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectPerformed(ValueChangeEvent evt) {
		this.selectAll = ControllerUtil.selectPerformed(evt, getVariables());
	}

	public void selectAllPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getVariables());
	}

	public BrowserBean getBrowser() {
		if (browser == null) {
			browser = new BrowserBean();
			browser.setModel(port);
		}
		return browser;
	}

	public boolean isShowBrowser() {
		return showBrowser;
	}

	public void browse() {
		variableName = FacesUtils.getRequestParameter(PARAM_VARIABLE_NAME);
		if (StringUtils.isEmpty(variableName)) {
			FacesUtils.addErrorMessage("Variable name not defined.");
			return;
		}

		browser.setModel(port);
		showBrowser = true;
	}

	public String okAction() {
		if (StringUtils.isEmpty(variableName)) {
			FacesUtils.addErrorMessage("Variable name not defined.");
			return null;
		}
		String objectOid = FacesUtils.getRequestParameter(PARAM_OBJECT_OID);
		if (StringUtils.isEmpty(objectOid)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return null;
		}

		for (XPathVariableBean bean : getVariables()) {
			if (variableName.equals(bean.getVariableName())) {
				bean.setValue(objectOid);
			}
		}
		browseCleanup();

		return null;
	}

	private void browseCleanup() {
		variableName = null;
		showBrowser = false;
		browser.cleanup();
	}

	public String cancelAction() {
		browseCleanup();

		return null;
	}
}

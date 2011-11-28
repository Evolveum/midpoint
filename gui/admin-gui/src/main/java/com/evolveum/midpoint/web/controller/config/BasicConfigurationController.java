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
 */
package com.evolveum.midpoint.web.controller.config;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.util.FacesUtils;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("basicConfiguration")
@Scope("session")
public class BasicConfigurationController implements Serializable {

	private static final long serialVersionUID = 7851989332171757365L;
	private String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<ui:composition template=\"/resources/templates/template.xhtml\" "
			+ "xmlns=\"http://www.w3.org/1999/xhtml\">\n</ui:composition>";

	private boolean editable = true;

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String action() {
		OperationResult result1 = new OperationResult("Small operation");
		result1.recordSuccess();
		FacesUtils.addMessage(result1);
		
		FacesUtils.addSuccessMessage("Standard success message.");

		OperationResult result2 = new OperationResult("Middle operation");
		result2.recordWarning("Some warning happened.");
		result2.getSubresults().add(result1);
		FacesUtils.addMessage(result2);
		
		FacesUtils.addWarnMessage("Standard warning message.");

		OperationResult result3 = new OperationResult("Top operation " + BasicConfigurationController.class.getName() + ".action");
		result3.recordFatalError("Error happened. Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.");
		result3.getSubresults().add(result2);
		FacesUtils.addMessage(result3);
		
		FacesUtils.addErrorMessage("Standard error message.");

		return null;
	}
}

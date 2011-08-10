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
package com.evolveum.midpoint.web.jsf.messages;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.application.Resource;
import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.result.OperationResultStatus;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.icesoft.faces.component.ext.HtmlMessages;

/**
 * 
 * @author lazyman
 * 
 */
@FacesComponent("MidPointFacesMessages")
public class MidPointFacesMessages extends HtmlMessages {

	public static final String IMAGE_BUTTON = "ImageButton";

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("ul", null);
		writer.writeAttribute("id", getId(), null);

		int index = 0;
		Iterator<FacesMessage> iterator = context.getMessages();
		while (iterator.hasNext()) {
			FacesMessage message = iterator.next();
			if (message.isRendered() && !isRedisplay()) {
				continue;
			}
			message.rendered();

			if (message instanceof MidPointMessage) {
				writeMidPointMessage((MidPointMessage) message, context, index);
				index++;
			} else {
				writer.startElement("li", null);
				writer.startElement("span", null);
				writer.writeAttribute("class", getMessageSeverityClass(message), null);
				writer.writeText(getSummary(message), null);
				writer.endElement("span");
				writer.endElement("li");
			}
		}
		writer.endElement("ul");
	}

	private String getSummary(OperationResult result) {
		return getSummaryMessage(result.getMessage());
	}

	private String getSummary(FacesMessage message) {
		return getSummaryMessage(message.getSummary());
	}

	private String getSummaryMessage(String message) {
		if (StringUtils.isEmpty(message)) {
			return FacesUtils.translateKey("No message.");
		}

		return message;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
	}

	private String getMessageSeverityClass(FacesMessage message) {
		String severityStyleClass = "";
		if (message.getSeverity() == FacesMessage.SEVERITY_INFO) {
			severityStyleClass = (String) getAttributes().get("infoClass");
		} else if (message.getSeverity() == FacesMessage.SEVERITY_WARN) {
			severityStyleClass = (String) getAttributes().get("warnClass");
		} else if (message.getSeverity() == FacesMessage.SEVERITY_ERROR) {
			severityStyleClass = (String) getAttributes().get("errorClass");
		} else if (message.getSeverity() == FacesMessage.SEVERITY_FATAL) {
			severityStyleClass = (String) getAttributes().get("fatalClass");
		}

		return severityStyleClass;
	}

	private void writeMidPointMessage(MidPointMessage message, FacesContext context, int index)
			throws IOException {
		OperationResult result = message.getResult();
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("li", null);
		writer.startElement("span", null);
		writer.writeAttribute("class", getMessageSeverityClass(message), null);

		// main message
		writer.startElement("span", null);
		writer.writeText(getSummary(message), null);
		writer.endElement("span");

		String divId = getClientId() + index;

		// button
		writer.startElement("span", null);
		writer.writeAttribute("class", "messages-display-details", null);
		writer.startElement("img", null);
		writer.writeAttribute("id", divId + IMAGE_BUTTON, null);
		Resource show = context.getApplication().getResourceHandler().createResource("add.png", "images");
		Resource hide = context.getApplication().getResourceHandler().createResource("delete.png", "images");
		if (show != null && hide != null) {
			writer.writeAttribute("src", show.getRequestPath(), null);
			StringBuilder script = new StringBuilder();
			script.append("displayMessageDetails('");
			script.append(divId);
			script.append("', '");
			script.append(show.getRequestPath());
			script.append("', '");
			script.append(hide.getRequestPath());
			script.append("');");
			writer.writeAttribute("onclick", script.toString(), null);
		}
		writer.endElement("img");
		writer.endElement("span");

		// message details
		writer.startElement("div", null);
		writer.writeAttribute("class", "messages-details", null);
		writer.writeAttribute("id", divId, null);
		writeMessageDetailBold(FacesUtils.translateKey(result.getOperation()), writer);
		writeMessageDetailNormal(result.getMessageCode(), writer);
		writeOperationResult(result, context);

		writer.endElement("div");

		writer.endElement("span");
		writer.endElement("li");
	}

	private String getOperationResultStatusClass(OperationResultStatus status) {
		String styleClass = "";
		switch (status) {
			case FATAL_ERROR:
			case PARTIAL_ERROR:
				styleClass = "messages-line-error";
				break;
			case SUCCESS:
				styleClass = "messages-line-info";
				break;
			case UNKNOWN:
			case WARNING:
				styleClass = "messages-line-warn";
		}

		return styleClass;
	}

	private void writeOperationResult(OperationResult result, FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();

		writer.startElement("ul", null);
		writer.writeAttribute("class", "messages-details-advanced", null);

		writer.startElement("li", null);
		writer.writeAttribute("class", getOperationResultStatusClass(result.getStatus()), null);
		writeMessageDetailBold(FacesUtils.translateKey(result.getOperation()), writer);
		writeMessageDetailNormal(result.getMessageCode(), writer);
		writeMessageDetailNormal(getSummary(result), writer);
		writeMessageDetailNormal("(" + result.getToken() + ")", writer);

		if (!result.getSubresults().isEmpty()) {
			for (OperationResult subResult : result.getSubresults()) {
				writeOperationResult(subResult, context);
			}
		}

		writer.endElement("li");
		writer.endElement("ul");
	}

	private void writeMessageDetailNormal(Object detail, ResponseWriter writer) throws IOException {
		writeMessageDetail(detail, writer, "message-detail");
	}

	private void writeMessageDetailBold(Object detail, ResponseWriter writer) throws IOException {
		writeMessageDetail(detail, writer, "message-detail-bold");
	}

	private void writeMessageDetail(Object detail, ResponseWriter writer, String style) throws IOException {
		if (detail == null) {
			return;
		}

		writer.startElement("span", null);
		writer.writeAttribute("class", style, null);
		writer.writeText(detail, null);
		writer.endElement("span");
	}
}

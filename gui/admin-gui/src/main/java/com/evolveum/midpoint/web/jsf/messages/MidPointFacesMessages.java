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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;
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
	public static final String IMAGE_BUTTON_CONTENT = "ImageButtonContent";
	private int errorNum = 0;

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
			return FacesUtils.translateKey("operation.noMessage");
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

	/**
	 * @param message
	 * @param context
	 * @param index
	 * @throws IOException
	 */
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
		Resource show_add = context.getApplication().getResourceHandler().createResource("add.png", "images");
		Resource hide_delete = context.getApplication().getResourceHandler()
				.createResource("delete.png", "images");
		if (show_add != null && hide_delete != null) {
			writer.writeAttribute("src", show_add.getRequestPath(), null);
			StringBuilder script = new StringBuilder();
			script.append("displayMessageDetails('");
			script.append(divId);
			script.append("', '");
			script.append(show_add.getRequestPath());
			script.append("', '");
			script.append(hide_delete.getRequestPath());
			script.append("');");
			writer.writeAttribute("onclick", script.toString(), null);
		}
		writer.endElement("img");
		writer.endElement("span");

		// message details
		writer.startElement("div", null);
		writer.writeAttribute("class", "messages-details", null);
		writer.writeAttribute("id", divId, null);
		// writeMessageDetailBold(FacesUtils.translateKey("operation." +
		// result.getOperation()), writer);
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

		writeMessageDetailBold(FacesUtils.translateKey("operation." + result.getOperation()), writer);

		// message error details
		writer.startElement("span", null);
		writer.writeAttribute("class", "messages-details-error_button", null);
		writer.writeAttribute("id", "errorNumber" + errorNum, null);
		StringBuilder script = new StringBuilder();
		script.append("displayMessageErrorDetails('");
		script.append("errorNumber" + errorNum);
		script.append("');");
		writer.writeAttribute("onclick", script.toString(), null);
		writer.write("[Details]");
		writer.endElement("span");

		writer.startElement("div", null);
		writer.writeAttribute("class", "messages-details-error", null);
		writer.writeAttribute("id", "errorNumber" + errorNum + "_block", null);
		writer.writeAttribute("style", "display: none;", null);
		writeMessageDetailNormal(result.getMessageCode(), writer);
		writeMessageDetailNormal(getSummary(result), writer);
		writeMessageDetailNormal("(" + result.getToken() + ")", writer);
		writeMessageDetailsParams(result.getParams(), writer);
		writeMessageDetailsContext(result.getContext(), writer);
		writeMessageDetailsDetails(result.getDetail(), writer);
		writeMessageDetailsCause(result.getCause(), writer, "errorNumber" + errorNum);
		writer.endElement("div");
		errorNum++;

		writer.endElement("li");
		writer.endElement("ul");

		if (!result.getSubresults().isEmpty()) {
			for (OperationResult subResult : result.getSubresults()) {
				writeOperationResult(subResult, context);
			}
		}

	}

	private void writeMessageDetailsParams(Map<String, Object> map, ResponseWriter writer) throws IOException {
		Set<Entry<String, Object>> set = map.entrySet();
		Iterator<Entry<String, Object>> it = set.iterator();

		while (it.hasNext()) {
			Map.Entry<String, Object> me = (Map.Entry<String, Object>) it.next();
			// System.out.println(me.getKey() + " : " + me.getValue());
			writer.startElement("ul", null);
			writer.startElement("li", null);
			writer.write("&raquo; Params &gt;&gt;  " + me.getKey() + " : " + me.getValue());
			writer.endElement("li");
			writer.endElement("ul");
		}
	}

	private void writeMessageDetailsContext(Map<String, Object> map, ResponseWriter writer)
			throws IOException {
		Set<Entry<String, Object>> set = map.entrySet();
		Iterator<Entry<String, Object>> it = set.iterator();

		while (it.hasNext()) {
			Map.Entry<String, Object> me = (Map.Entry<String, Object>) it.next();
			// System.out.println(me.getKey() + " : " + me.getValue());
			writer.startElement("ul", null);
			writer.startElement("li", null);
			writer.write("&raquo; Context &gt;&gt;  " + me.getKey() + " : " + me.getValue());
			writer.endElement("li");
			writer.endElement("ul");
		}
	}

	private void writeMessageDetailsDetails(List<String> list, ResponseWriter writer) throws IOException {
		if (list.isEmpty()) {
			return;
		}

		Iterator<String> it = list.iterator();

		while (it.hasNext()) {
			// System.out.println("TEST LIST: " + (String) it.next());
			writer.startElement("ul", null);
			writer.startElement("li", null);
			writer.write("&raquo; Details &gt;&gt;  " + (String) it.next());
			writer.endElement("li");
			writer.endElement("ul");
		}
	}

	private void writeMessageDetailsCause(Throwable th, ResponseWriter writer, String causeNum)
			throws IOException {
		if (th != null) {
			writer.startElement("ul", null);
			writer.startElement("li", null);
			writer.write("&raquo; Cause &gt;&gt; ");
			writer.writeText(th.getMessage(), null);
			writer.startElement("span", null);
			writer.writeAttribute("class", "messages-details-cause_button", null);
			writer.writeAttribute("id", "causeNumber" + causeNum, null);
			StringBuilder script = new StringBuilder();
			script.append("displayMessageCauseDetails('");
			script.append("causeNumber" + causeNum);
			script.append("');");
			writer.writeAttribute("onclick", script.toString(), null);
			writer.write(" [More]");
			writer.endElement("span");
			writer.startElement("div", null);
			writer.writeAttribute("class", "messages-details-cause", null);
			writer.writeAttribute("id", "causeNumber" + causeNum + "_block", null);
			writer.writeAttribute("style", "display: none;", null);

			writer.startElement("span", null);
			PrintWriter printWriter = new PrintWriter(writer);
			th.printStackTrace(printWriter);
			writer.writeText(writer.toString(), null);
			writer.endElement("span");
			writer.endElement("div");
			writer.endElement("li");
			writer.endElement("ul");
		}

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

		if (style == "message-detail") {
			writer.startElement("ul", null);
			writer.startElement("li", null);
			writer.startElement("span", null);
			writer.writeAttribute("class", style, null);
			writer.write("&raquo; ");
			writer.writeText(detail, null);
			writer.endElement("span");
			writer.endElement("li");
			writer.endElement("ul");
		} else {
			writer.startElement("span", null);
			writer.writeAttribute("class", style, null);
			writer.writeText(detail, null);
			writer.endElement("span");
		}
	}
}

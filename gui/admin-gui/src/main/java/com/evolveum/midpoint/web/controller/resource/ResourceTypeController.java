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

package com.evolveum.midpoint.web.controller.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.dto.GuiResourceDto;
import com.evolveum.midpoint.web.dto.GuiTestResultDto;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.DiagnosticMessageDto;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.web.model.TaskStatusDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.icesoft.faces.component.ext.RowSelectorEvent;

/**
 * Depreacted, break class into smaller pieces according to pages
 * 
 * @author Katuska
 */
@Deprecated
@Controller
@Scope("session")
public class ResourceTypeController implements Serializable {

	public static final String PAGE_NAVIGATION_TEST = "/resource/testResource?faces-redirect=true";
	public static final String PAGE_NAVIGATION_LIST = "/resource/listResources?faces-redirect=true";
	public static final String PAGE_NAVIGATION_LIST_ACCOUNTS = "/resource/listResourcesAccounts?faces-redirect=true";
	public static final String PAGE_NAVIGATION_IMPORT = "/resource/resourceImportStatus?faces-redirect=true";
	private static final long serialVersionUID = 1494194832160260941L;
	private static final Trace TRACE = TraceManager.getTrace(ResourceTypeController.class);
	@Autowired
	private transient ModelPortType model;
	private ObjectTypeCatalog objectTypeCatalog;
	private List<GuiResourceDto> resources;
	private GuiResourceDto resourceDto;
	private List<AccountShadowDto> accounts;
	private GuiTestResultDto guiTestResult;
	private TaskStatusDto status;

	public String backPerformed() {
		for (GuiResourceDto res : resources) {
			res.setSelected(false);
		}
		resourceDto = null;
		accounts = null;
		return PAGE_NAVIGATION_LIST;
	}

	private void parseForUsedConnector(GuiResourceDto guiResource) {
		// String connectorUsed = "";
		if (guiResource.getConfiguration().size() > 0) {
			for (Element element : guiResource.getConfiguration()) {
				NamedNodeMap attributes = element.getFirstChild().getAttributes();
				if (attributes.getNamedItem("connectorName") != null) {
					guiResource.setConnectorUsed(attributes.getNamedItem("connectorName").getTextContent());
				}
				if (attributes.getNamedItem("bundleVersion") != null) {
					guiResource
							.setConnectorVersion(attributes.getNamedItem("bundleVersion").getTextContent());
				}
			}
		}
		// return connectorUsed;
	}

	public String listAccounts() {

		if (resourceDto == null) {
			FacesUtils.addErrorMessage("Resource must be selected");
			return "";
		}

		try { // Call Web Service Operation
			String objectType = Utils.getObjectType("AccountType");

			OperationalResultType operationalResult = new OperationalResultType();
			Holder<OperationalResultType> holder = new Holder<OperationalResultType>(operationalResult);
			ObjectListType result = model.listResourceObjects(resourceDto.getOid(), objectType,
					new PagingType(), new Holder<OperationResultType>(new OperationResultType()));
			List<ObjectType> objects = result.getObject();
			accounts = new ArrayList<AccountShadowDto>();

			for (ObjectType o : objects) {
				AccountShadowDto account = new AccountShadowDto();
				account.setXmlObject(o);
				accounts.add(account);
			}
			for (AccountShadowDto account : accounts) {
				TRACE.debug("account oid {}", account.getOid());
				TRACE.debug("account name {}", account.getName());
				TRACE.debug("account version {}", account.getVersion());
			}

		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("List accounts failed.");
			FacesUtils.addErrorMessage("Exception was: " + message);
			TRACE.error("List accounts failed");
			TRACE.error("Exception was: ", ex);
		}
		return PAGE_NAVIGATION_LIST_ACCOUNTS;
	}

	private String getResults(boolean test) {
		// accept == test passed, cancel == test faild
		String result = (test) ? "accept" : "cancel";
		TRACE.debug("test result: {}", result);
		return result;
	}

	private String getEachResult(TestResultType testResultType) {
		// error == test not tested
		String result = (testResultType != null) ? getResults(testResultType.isSuccess()) : "error";
		return result;

	}

	public String testConnection() {

		if (resourceDto == null) {
			FacesUtils.addErrorMessage("Resource must be selected");
			return "";
		}

		try { // Call Web Service Operation
			ResourceTestResultType result = model.testResource(resourceDto.getOid(),
					new Holder<OperationResultType>(new OperationResultType()));
			guiTestResult = new GuiTestResultDto();
			guiTestResult.setConfigurationValidation(getEachResult(result.getConfigurationValidation()));
			guiTestResult.setConnectorConnection(getEachResult(result.getConnectorConnection()));
			guiTestResult.setConnectionInitialization(getEachResult(result.getConnectorInitialization()));
			guiTestResult.setConnectionSanity(getEachResult(result.getConnectorSanity()));
			guiTestResult.setConnectionSchema(getEachResult(result.getConnectorSchema()));
			if (result.getExtraTest() != null) {
				guiTestResult.setExtraTestResult(getEachResult(result.getExtraTest().getResult()));
				guiTestResult.setExtraTestName(result.getExtraTest().getName());
			} else {
				guiTestResult.setExtraTestResult("error");
				guiTestResult.setExtraTestName("error");
			}

		} catch (FaultMessage ex) {
			FacesUtils.addErrorMessage("Test resource failed: " + FacesUtils.getMessageFromFault(ex));
			TRACE.error("Test resources failed", ex);
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("Test failed, reason: " + ex.getMessage());
			TRACE.error("Unkonwn error during test connection on resource: " + resourceDto.getOid(), ex);
		}

		return PAGE_NAVIGATION_TEST;
	}

	public void importFromResource() {
		if (resourceDto == null) {
			FacesUtils.addErrorMessage("Resource must be selected");
			return;
		}

		try { // Call Web Service Operation

			// TODO: HACK: this should be determined from the resource schema.
			// But ICF always generates the name for __ACCOUNT__ like this.
			String objectClass = "Account";

			TRACE.debug("Calling launchImportFromResource({})", resourceDto.getOid());
			model.launchImportFromResource(resourceDto.getOid(), objectClass,
					new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("Launching import from resource failed: " + message);
			TRACE.error("Launching import from resources failed.", ex);
		} catch (RuntimeException ex) {
			// Due to insane preferrence of runtime exception in "modern" Java
			// we need to catch all of them. These may happen in JBI layer and
			// we are not even sure which exceptions to cacht.
			// To a rough hole a rough patch.
			FacesUtils.addErrorMessage("Launching import from resource failed. Runtime error: "
					+ ex.getClass().getSimpleName() + ": " + ex.getMessage());
			TRACE.error("Launching import from resources failed. Runtime error.", ex);
		}

		// TODO switch to a page that will show progress
		return;
	}

	private TaskStatusDto convertTaskStatusResults(TaskStatusType taskStatusType) {
		TaskStatusDto taskStatus = new TaskStatusDto();
		if (taskStatusType.getFinishTime() != null) {
			taskStatus.setFinishTime(taskStatusType.getFinishTime().toXMLFormat());
		}
		DiagnosticsMessageType lastError = null;
		if ((lastError = taskStatusType.getLastError()) != null) {
			DiagnosticMessageDto diagnosticMessage = new DiagnosticMessageDto();

			if (lastError.getDetails() != null) {
				diagnosticMessage.setDetails(lastError.getDetails());
			}
			if (lastError.getMessage() != null) {
				diagnosticMessage.setMessage(lastError.getMessage());
			}
			if (lastError.getTimestamp() != null) {
				diagnosticMessage.setTimestamp(lastError.getTimestamp().toXMLFormat());
			}
			taskStatus.setLastError(diagnosticMessage);
		}

		if (taskStatusType.getLaunchTime() != null) {
			taskStatus.setLaunchTime(taskStatusType.getLaunchTime().toXMLFormat());
		}
		if (taskStatusType.getNumberOfErrors() != null) {
			taskStatus.setNumberOfErrors(String.valueOf(taskStatusType.getNumberOfErrors()));
		}
		if (taskStatusType.getProgress() != null) {
			taskStatus.setProgress(String.valueOf(taskStatusType.getProgress()));
		}
		taskStatus.setLastStatus(taskStatusType.getLastStatus());
		taskStatus.setName(taskStatusType.getName());
		String running = (taskStatusType.isRunning()) ? "YES" : "NO";
		taskStatus.setRunning(running);

		return taskStatus;
	}

	public String getImportStatus() {

		if (resourceDto == null) {
			FacesUtils.addErrorMessage("Resource must be selected");
			return "";
		}

		try { // Call Web Service Operation
			TaskStatusType taskStatus = model.getImportStatus(resourceDto.getOid(),
					new Holder<OperationResultType>(new OperationResultType()));
			status = convertTaskStatusResults(taskStatus);
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("Getting import status failed: " + message);
			TRACE.error("Getting import status failed.", ex);
		} catch (RuntimeException ex) {
			// Due to insane preferrence of runtime exception in "modern" Java
			// we need to catch all of them. These may happen in JBI layer and
			// we are not even sure which exceptions to cacht.
			// To a rough hole a rough patch.
			FacesUtils.addErrorMessage("Getting import status failed:. Runtime error: "
					+ ex.getClass().getSimpleName() + ": " + ex.getMessage());
			TRACE.error("Getting import status failed:. Runtime error.", ex);
		}

		return PAGE_NAVIGATION_IMPORT;
	}

	public void resourceSelectionListener(RowSelectorEvent event) {
		String resourceId = resources.get(event.getRow()).getOid();

		try { // Call Web Service Operation

			ObjectType result = model.getObject(resourceId, new PropertyReferenceListType(),
					new Holder<OperationResultType>(new OperationResultType()));
			resourceDto = new GuiResourceDto((ResourceType) result);
			parseForUsedConnector(resourceDto);
		} catch (Exception ex) {
			TRACE.error("Select resource failed");
			TRACE.error("Exception was: ", ex);
		}
	}

	public String listResources() {

		try { // Call Web Service Operation
			String objectType = Utils.getObjectType("ResourceType");
			// TODO: more reasonable handling of paging info
			PagingType paging = new PagingType();
			ObjectListType result = model.listObjects(objectType, paging, new Holder<OperationResultType>(
					new OperationResultType()));
			List<ObjectType> objects = result.getObject();
			resources = new ArrayList<GuiResourceDto>();

			for (ObjectType o : objects) {
				ResourceDto resource = new ResourceDto();
				resource.setXmlObject(o);
				GuiResourceDto guiResource = new GuiResourceDto((ResourceType) resource.getXmlObject());
				parseForUsedConnector(guiResource);
				resources.add(guiResource);

			}

		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("List resources failed.");
			FacesUtils.addErrorMessage("Exception was: " + message);
			TRACE.error("List resources failed");
			TRACE.error("Exception was: ", ex);
			return null;
		}

		return PAGE_NAVIGATION_LIST;

	}

	public ObjectTypeCatalog getObjectTypeCatalog() {
		return objectTypeCatalog;
	}

	public void setObjectTypeCatalog(ObjectTypeCatalog objectTypeCatalog) {
		this.objectTypeCatalog = objectTypeCatalog;
	}

	public List<GuiResourceDto> getResources() {
		return resources;
	}

	public void setResources(List<GuiResourceDto> resources) {
		this.resources = resources;
	}

	public List<AccountShadowDto> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<AccountShadowDto> accounts) {
		this.accounts = accounts;
	}

	public GuiResourceDto getResourceDto() {
		return resourceDto;
	}

	public void setResourceDto(GuiResourceDto resourceDto) {
		this.resourceDto = resourceDto;
	}

	public GuiTestResultDto getGuiTestResult() {
		return guiTestResult;
	}

	public void setGuiTestResult(GuiTestResultDto guiTestResult) {
		this.guiTestResult = guiTestResult;
	}

	public TaskStatusDto getStatus() {
		return status;
	}

	public void setStatus(TaskStatusDto status) {
		this.status = status;
	}

}

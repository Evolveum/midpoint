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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.icefaces.component.fileentry.FileEntry;
import org.icefaces.component.fileentry.FileEntryEvent;
import org.icefaces.component.fileentry.FileEntryResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author katuska
 */
@Controller("import")
@Scope("session")
public class ImportController implements Serializable {

	private static final long serialVersionUID = -4206532259499809326L;
	private static final Trace logger = TraceManager.getTrace(ImportController.class);
	private int fileProgress;
	private String xmlObject;
	private boolean overwrite = false;
	@Autowired(required = true)
	transient RepositoryPortType repositoryService;

	public String setImportPage() {
		xmlObject = "";

		return "/importPage";
	}

	private void addObjectsToRepository(List<ObjectType> objects) {
		if (objects == null) {
			return;
		}

		setImportPage();

		for (ObjectType object : objects) {
			ObjectContainerType objectContainer = new ObjectContainerType();
			objectContainer.setObject(object);
			try {
				if (overwrite) {
					if (repositoryService.getObject(object.getOid(), new PropertyReferenceListType()) != null) {
						repositoryService.deleteObject(object.getOid());
					}
				}
				repositoryService.addObject(objectContainer);
				FacesUtils.addSuccessMessage("Added object: " + object.getName());
			} catch (FaultMessage ex) {
				String failureMesage = FacesUtils.getBundleKey("msg", "import.jaxb.failed");
				FacesUtils.addErrorMessage(failureMesage + " " + FacesUtils.getMessageFromFault(ex));
				FacesUtils.addErrorMessage("Failed to add object " + object.getName());
				logger.error("Exception was: {}", ex, ex);
			} catch (Exception ex) {
				String failureMessage = FacesUtils.getBundleKey("msg", "import.jaxb.failed");
				FacesUtils.addErrorMessage(failureMessage + ":" + ex.getMessage());
				FacesUtils.addErrorMessage("Failed to add object " + object.getName());
				logger.error("Add object failed");
				logger.error("Exception was: {}", ex, ex);
			}

		}
		xmlObject = "";
	}

	public String addObjects() {
		if (StringUtils.isEmpty(xmlObject)) {
			String loginFailedMessage = FacesUtils.getBundleKey("msg", "import.null.failed");
			FacesUtils.addErrorMessage(loginFailedMessage);
			return "";
		}

		try {
			InputStream stream = IOUtils.toInputStream(xmlObject, "utf-8");
			uploadObjects(stream);
			stream.close();
		} catch (IOException ex) {
			FacesUtils.addErrorMessage("Couldn't load object from xml, reason: " + ex.getMessage());
		}

		return "/importPage";
	}

	public void uploadFile(FileEntryEvent event) {
		logger.info("uploadFile start");
		FileEntry fileEntry = (FileEntry) event.getSource();
		FileEntryResults results = fileEntry.getResults();
		for (FileEntryResults.FileInfo fi : results.getFiles()) {
			logger.info("file name {}", fi.getFileName());

			File file = fi.getFile();
			if (file == null || !file.exists() || !file.canRead()) {
				FacesUtils.addErrorMessage("Can't read file '" + fi.getFileName() + "'.");
				return;
			}

			try {
				InputStream stream = new BufferedInputStream(new FileInputStream(file));
				uploadObjects(stream);
				stream.close();
			} catch (IOException ex) {
				FacesUtils.addErrorMessage("Couldn't load object from file '" + file.getName()
						+ "', reason: " + ex.getMessage());
			}
		}

		logger.info("uploadFile end");

	}

	private void uploadObjects(InputStream input) {
		final List<ObjectType> objects = new ArrayList<ObjectType>();
		Validator validator = new Validator(new ObjectHandler() {

			@Override
			public void handleObject(ObjectType object, List<ValidationMessage> objectErrors) {
				objects.add(object);
			}
		});
		List<ValidationMessage> messages = validator.validate(input);

		if (messages != null && !messages.isEmpty()) {
			StringBuilder builder;
			for (ValidationMessage message : messages) {
				builder = new StringBuilder();
				builder.append(message.getType());
				builder.append(": Object with oid '");
				builder.append(message.getOid());
				builder.append("' is not valid, reason: ");
				builder.append(message.getMessage());
				builder.append(".");
				if (!StringUtils.isEmpty(message.getProperty())) {
					builder.append(" Property: ");
					builder.append(message.getProperty());
				}
				FacesUtils.addErrorMessage(builder.toString());
			}
			return;
		} else {
			addObjectsToRepository(objects);
		}
	}

	public int getFileProgress() {
		return fileProgress;
	}

	public void setFileProgress(int fileProgress) {
		this.fileProgress = fileProgress;
	}

	public String getXmlObject() {
		return xmlObject;
	}

	public void setXmlObject(String xmlObject) {
		this.xmlObject = xmlObject;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
}

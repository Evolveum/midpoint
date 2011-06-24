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

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author katuska
 */
@Controller("import")
@Scope("session")
public class ImportController implements Serializable {

	private static final long serialVersionUID = -4206532259499809326L;
	private static final Trace TRACE = TraceManager.getTrace(ImportController.class);
	@Autowired(required = true)
	private transient RepositoryManager repositoryManager;
	private String editor;
	private boolean showFileUpload = false;
	private boolean overwrite = false;

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	public boolean isShowFileUpload() {
		return showFileUpload;
	}

	public void setShowFileUpload(boolean showUploadFile) {
		this.showFileUpload = showUploadFile;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public String upload() {
		if (showFileUpload) {
			return null;
		}

		if (StringUtils.isEmpty(editor)) {
			FacesUtils.addErrorMessage("Editor is null.");
			return null;
		}

		InputStream stream = null;
		try {
			stream = IOUtils.toInputStream(editor, "utf-8");
			if (uploadStream(stream)) {
				clearController();
			}
		} catch (IOException ex) {
			FacesUtils.addErrorMessage("Couldn't load object from xml, reason: " + ex.getMessage());
			// TODO: logging
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}

		return null;
	}

	public void uploadFile(FileEntryEvent event) {
		FileEntry fileEntry = (FileEntry) event.getSource();
		FileEntryResults results = fileEntry.getResults();
		for (FileEntryResults.FileInfo info : results.getFiles()) {
			File file = info.getFile();
			if (file == null || !file.exists() || !file.canRead()) {
				FacesUtils.addErrorMessage("Can't read file '" + info.getFileName() + "'.");
				return;
			}

			InputStream stream = null;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
				if (uploadStream(stream)) {
					clearController();
				}
			} catch (IOException ex) {
				FacesUtils.addErrorMessage("Couldn't load object from file '" + file.getName() + "'.", ex);
				// TODO: logging
			} finally {
				if (stream != null) {
					IOUtils.closeQuietly(stream);
				}
			}
		}
	}

	private void clearController() {
		showFileUpload = false;
		overwrite = false;
		editor = null;
	}

	private boolean addObjectsToRepository(List<ObjectType> objects) {
		if (objects == null) {
			return false;
		}

		for (ObjectType object : objects) {
			try {
				if (overwrite && !repositoryManager.deleteObject(object.getOid())) {
					FacesUtils.addWarnMessage("Couldn't delete object with oid '" + object.getOid() + "'.");
				}

				repositoryManager.addObject(object);
			} catch (Exception ex) {
				LoggingUtils.logException(TRACE, "Couldn't import object {}", ex, object.getName());
				FacesUtils.addErrorMessage("Couldn't import object '" + object.getName() + "'", ex);
			}
		}

		return true;
	}

	private boolean uploadStream(InputStream input) {
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
			return false;
		}

		return addObjectsToRepository(objects);
	}
}

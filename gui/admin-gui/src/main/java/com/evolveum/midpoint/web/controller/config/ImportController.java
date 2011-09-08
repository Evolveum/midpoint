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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.icefaces.component.fileentry.FileEntry;
import org.icefaces.component.fileentry.FileEntryEvent;
import org.icefaces.component.fileentry.FileEntryResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ImportOptionsType;

/**
 * 
 * @author katuska
 */
@Controller("import")
@Scope("session")
public class ImportController implements Serializable {

	private static final long serialVersionUID = -4206532259499809326L;
	private static final Trace LOGGER = TraceManager.getTrace(ImportController.class);
	@Autowired(required = true)
	private transient ModelService model;
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
			LoggingUtils.logException(LOGGER, "Couldn't transform string from editor to input stream", ex);
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
				LoggingUtils.logException(LOGGER, "Couldn't load file {} as input stream", ex,
						file.getAbsolutePath());
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

	private boolean uploadStream(InputStream input) {

		OperationResult parentResult = new OperationResult(ImportController.class.getName() + ".uploadStream");

		ImportOptionsType options = MiscUtil.getDefaultImportOptions();
		options.setOverwrite(overwrite);
		model.importObjectsFromStream(input, options, null, parentResult);

		if (!parentResult.isSuccess()) {
			parentResult.computeStatus("Failed to import objects form file. Reason: "
					+ parentResult.getMessage());
			FacesUtils.addMessage(parentResult);
		}

		return parentResult.isAcceptable();

	}
}

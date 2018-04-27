/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.authentication.NodeAuthenticationEvaluator;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ReportPeerQueryInterceptor extends HttpServlet {

	private static final long serialVersionUID = 7612750211021974750L;
	private static String MIDPOINT_HOME = System.getProperty("midpoint.home");
	private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
	private static String DEFAULTMIMETYPE = "application/octet-stream";
	
	private static final String OPERATION_GET_REPORT = "File retrieval";
	private static final String OPERATION_DELETE_REPORT = "File deletion";
	
	private static final Trace LOGGER = TraceManager.getTrace(ReportPeerQueryInterceptor.class);

	@Autowired
	private NodeAuthenticationEvaluator nodeAuthenticator;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!checkRequest(request, response, OPERATION_GET_REPORT)) {
			return;
		}

		String fileName = getFileName(request);

		if (containsProhibitedQueryString(fileName)) {
			LOGGER.debug("Query parameter contains a prohibited character sequence. The parameter: {} ", fileName);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		StringBuilder buildfilePath = new StringBuilder(EXPORT_DIR).append(fileName);
		String filePath = buildfilePath.toString();

		File loadedFile = new File(filePath);

		if (!isFileAndExists(loadedFile, fileName, response, OPERATION_GET_REPORT)) {
			return;
		}

		FileInputStream fileInputStream = new FileInputStream(filePath);

		ServletContext context = getServletContext();
		String mimeType = context.getMimeType(filePath);

		if (mimeType == null) {
			// MIME mapping not found
			mimeType = DEFAULTMIMETYPE;
		}

		response.setContentType(mimeType);
		response.setContentLength((int) loadedFile.length());

		StringBuilder headerValue = new StringBuilder("attachment; filename=\"%s\"").append(fileName);
		response.setHeader("Content-Disposition", headerValue.toString());

		OutputStream outputStream = response.getOutputStream();

		byte[] buffer = new byte[1024];
		int len;
		while ((len = fileInputStream.read(buffer)) > -1) {
			outputStream.write(buffer, 0, len);
		}
		IOUtils.closeQuietly(fileInputStream);
		IOUtils.closeQuietly(outputStream);
		LOGGER.trace("The file {} has been dispatched to the client.", fileName);

	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {

		if (!checkRequest(request, response, OPERATION_DELETE_REPORT)) {
			return;
		}

		String fileName = getFileName(request);
		StringBuilder buildfilePath = new StringBuilder(EXPORT_DIR).append(fileName);
		String filePath = buildfilePath.toString();
		File reportFile = new File(filePath);

		if (!isFileAndExists(reportFile, fileName, response, OPERATION_DELETE_REPORT)) {
			return;
		}

		reportFile.delete();
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);

		LOGGER.trace("Deletion of the file {} has finished.", fileName);

	}

	private boolean checkRequest(HttpServletRequest request, HttpServletResponse response, String operation) {

		String userAgent = request.getHeader("User-Agent");
		if (!ReportTypeUtil.HEADER_USERAGENT.equals(userAgent)) {
			LOGGER.debug("Invalid user-agent: {}", userAgent);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		if (!nodeAuthenticator.authenticate(request.getRemoteHost(), request.getRemoteAddr(), operation)) {
			LOGGER.debug("Unknown node, host: {} ", request.getRemoteHost());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		return true;
	}

	private boolean isFileAndExists(File reportFile, String fileName, HttpServletResponse response, String operation)
			throws IOException {

		if (!reportFile.exists()) {
			LOGGER.warn(operation + " not successful. The file: {} was not found on the filesystem.", fileName);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		if (reportFile.isDirectory()) {
			LOGGER.warn(operation
					+ " not successful. The file is actually a directory with the name: {}. This operation is prohibited.",
					fileName);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		return true;

	}

	private String getFileName(HttpServletRequest request) throws UnsupportedEncodingException {
		String fileName = request.getParameter(ReportTypeUtil.FILENAMEPARAMETER);
		return URLDecoder.decode(fileName, ReportTypeUtil.URLENCODING);
	}

	protected Boolean containsProhibitedQueryString(String queryParameter) {
		if (queryParameter.contains("/../")) {
			return true;
		}
		return false;
	}

}

/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.List;

public class ReportPeerQueryInterceptor extends HttpServlet {

    private static final long serialVersionUID = 7612750211021974750L;
    private static String MIDPOINT_HOME = System.getProperty("midpoint.home");
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
    private static String HEADER_USERAGENT = "mp-cluster-peer-client";          // TODO deduplicate w.r.t. ReportNodeUtils
    private static String DEFAULTMIMETYPE = "application/octet-stream";
    private static String FILENAMEPARAMETER = "fname";                          // TODO deduplicate w.r.t. ReportNodeUtils
    private static String URLENCODING = "UTF-8";

    private static final String INTERCEPTOR_CLASS = ReportPeerQueryInterceptor.class.getName() + ".";
    private static final String OPERATION_LIST_NODES = INTERCEPTOR_CLASS + "listNodes";

    private static final Trace LOGGER = TraceManager.getTrace(ReportPeerQueryInterceptor.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userAgent = request.getHeader("User-Agent");
        String remoteHost = request.getRemoteHost();
        String remoteAddress = request.getRemoteAddr();
        String fileName = request.getParameter(FILENAMEPARAMETER);
        fileName = URLDecoder.decode(fileName, URLENCODING);
        if (!HEADER_USERAGENT.equals(userAgent)) {
            LOGGER.debug("Invalid user-agent: {}", userAgent);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (!isKnownNode(remoteHost, remoteAddress, "File retrieval")) {
            LOGGER.debug("Unknown node, host: {} ", remoteHost);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (containsProhibitedQueryString(fileName)) {
            LOGGER.debug("Query parameter contains a prohibited character sequence. The parameter: {} ", fileName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            StringBuilder buildfilePath = new StringBuilder(EXPORT_DIR).append(fileName);
            String filePath = buildfilePath.toString();

            File loadedFile = new File(filePath);
            if (!loadedFile.exists()) {
                LOGGER.warn("Download operation not successful. The file: {} was not found on the filesystem", fileName);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else if (loadedFile.isDirectory()) {
                LOGGER.warn("Download operation not successful. Attempt to download a directory with the name: {} this operation is prohibited", fileName);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else {
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
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        String userAgent = request.getHeader("User-Agent");
        String remoteHost = request.getRemoteHost();
        String remoteAddress = request.getRemoteAddr();
        if (!HEADER_USERAGENT.equals(userAgent)) {
            LOGGER.debug("Invalid user-agent: {}", userAgent);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (!isKnownNode(remoteHost, remoteAddress, "File deletion")) {
            LOGGER.debug("Unknown node, host: {} ", remoteHost);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            String fileName = request.getParameter(FILENAMEPARAMETER);
            fileName = URLDecoder.decode(fileName, URLENCODING);
            StringBuilder buildfilePath = new StringBuilder(EXPORT_DIR).append(fileName);
            String filePath = buildfilePath.toString();
            File reportFile = new File(filePath);
            if (!reportFile.exists()) {
                LOGGER.warn("Delete operation not successful. The file: {} was not found on the filesystem.", fileName);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if (reportFile.isDirectory()) {
                LOGGER.warn("Delete operation not successful. Attempt to Delete a directory with the name: {}. This operation is prohibited.", fileName);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else {
                reportFile.delete();
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            LOGGER.trace("Deletion of the file {} has finished.", fileName);
        }
    }

    private RepositoryService getRepositoryService() {
        return MidPointApplication.get().getRepositoryService();
    }

    protected TaskManager getTaskManager() {
        return MidPointApplication.get().getTaskManager();
    }

    private Boolean isKnownNode(String remoteName, String remoteAddress, String operation) {
        LOGGER.debug("Checking if {} is a known node", remoteName);
        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        try {
            List<PrismObject<NodeType>> knownNodes = getRepositoryService().searchObjects(NodeType.class, null, null, result);
            for (PrismObject<NodeType> node : knownNodes) {
                NodeType actualNode = node.asObjectable();
                if (remoteName != null && remoteName.equalsIgnoreCase(actualNode.getHostname())) {
                    LOGGER.trace("The node {} was recognized as a known node (remote host name {} matched). Attempting to execute the requested operation: {} ",
                            actualNode.getName(), actualNode.getHostname(), operation);
                    return true;
                }
                if (actualNode.getIpAddress().contains(remoteAddress)) {
                    LOGGER.trace("The node {} was recognized as a known node (remote host address {} matched). Attempting to execute the requested operation: {} ",
                            actualNode.getName(), remoteAddress, operation);
                    return true;
                }
            }
        } catch (RuntimeException|SchemaException e) {
            LOGGER.error("Unhandled exception when listing nodes");
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", e);
        }
        return false;
    }

    protected Boolean containsProhibitedQueryString(String queryParameter) {
        if (queryParameter.contains("/../")) {
            return true;
        }
        return false;
    }

}

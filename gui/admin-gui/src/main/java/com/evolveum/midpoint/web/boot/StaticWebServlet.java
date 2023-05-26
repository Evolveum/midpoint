/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 */
public class StaticWebServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(StaticWebServlet.class);

    private File base;

    public StaticWebServlet(File base) {
        super();
        this.base = base;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {
        serveResource(request, response, true);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        serveResource(request, response, false);
    }

    protected void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException, ServletException {
        String relativePath = request.getPathInfo();
        LOGGER.trace("Serving relative path {}", relativePath);

        String requestUri = request.getRequestURI();
        if (relativePath == null || relativePath.length() == 0 || "/".equals(relativePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, requestUri);
            return;
        }

        File file = new File(base, relativePath);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, requestUri);
            return;
        }

        String contentType = getServletContext().getMimeType(file.getName());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(file.length()));

        LOGGER.trace("Serving file {}", file.getPath());

        ServletOutputStream outputStream = response.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);

        try {
            IOUtils.copy(fileInputStream, outputStream);
        } catch (IOException e) {
            throw e;
        } finally {
            fileInputStream.close();
            outputStream.close();
        }

    }


}

/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot;

import java.io.IOException;
import jakarta.servlet.ServletException;

import com.evolveum.midpoint.task.api.TaskManager;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import org.apache.commons.lang3.StringUtils;

/**
 * @author lskublik
 */
public class NodeIdHeaderValve extends ValveBase {

    private TaskManager taskManager;

    public NodeIdHeaderValve(TaskManager taskManager) {
        super();

        this.taskManager = taskManager;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        String nodeId = taskManager.getNodeId();
        if (StringUtils.isNotBlank(nodeId)) {
            response.addHeader("X-Served-By", nodeId);
        }

        getNext().invoke(request, response);
    }

}

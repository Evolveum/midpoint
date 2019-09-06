/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;

/**
 * @author mederly
 */
public class TaskDtoProviderOptions implements Serializable {

    // default values must be 'most informative'

    /**
     * Whether to ask for things requiring cluster communication (e.g. on which node is the task really executing)
     */
    private boolean useClusterInformation = true;
    private boolean resolveObjectRef = true;
    private boolean resolveOwnerRef = true;            // currently unused
    private boolean getNextRunStartTime = true;
    private boolean retrieveModelContext = true;
    private boolean retrieveWorkflowContext = true;
    private boolean getTaskParent = true;
    private boolean retrieveSiblings = true;
    private boolean retrieveOperationResult = true;
	private boolean createHandlerDto = true;

    public static TaskDtoProviderOptions minimalOptions() {
        TaskDtoProviderOptions options = new TaskDtoProviderOptions();
        options.setUseClusterInformation(false);
        options.setResolveObjectRef(false);
        options.setResolveOwnerRef(false);
        options.setGetNextRunStartTime(false);
        options.setRetrieveModelContext(false);
        options.setRetrieveWorkflowContext(false);
        options.setGetTaskParent(false);
		options.setRetrieveSiblings(false);
		options.setRetrieveOperationResult(false);
		options.setCreateHandlerDto(false);
        return options;
    }

    public static TaskDtoProviderOptions fullOptions() {
        return new TaskDtoProviderOptions();
    }

    public boolean isUseClusterInformation() {
        return useClusterInformation;
    }

    public void setUseClusterInformation(boolean useClusterInformation) {
        this.useClusterInformation = useClusterInformation;
    }

    public boolean isResolveObjectRef() {
        return resolveObjectRef;
    }

    public void setResolveObjectRef(boolean resolveObjectRef) {
        this.resolveObjectRef = resolveObjectRef;
    }

    public boolean isResolveOwnerRef() {
        return resolveOwnerRef;
    }

    public void setResolveOwnerRef(boolean resolveOwnerRef) {
        this.resolveOwnerRef = resolveOwnerRef;
    }

    public boolean isGetNextRunStartTime() {
        return getNextRunStartTime;
    }

    public void setGetNextRunStartTime(boolean getNextRunStartTime) {
        this.getNextRunStartTime = getNextRunStartTime;
    }

    public boolean isRetrieveModelContext() {
        return retrieveModelContext;
    }

    public void setRetrieveModelContext(boolean retrieveModelContext) {
        this.retrieveModelContext = retrieveModelContext;
    }

	public boolean isRetrieveWorkflowContext() {
		return retrieveWorkflowContext;
	}

	public void setRetrieveWorkflowContext(boolean retrieveWorkflowContext) {
		this.retrieveWorkflowContext = retrieveWorkflowContext;
	}

	public boolean isGetTaskParent() {
        return getTaskParent;
    }

    public void setGetTaskParent(boolean getTaskParent) {
        this.getTaskParent = getTaskParent;
    }

	public boolean isRetrieveSiblings() {
		return retrieveSiblings;
	}

	public void setRetrieveSiblings(boolean retrieveSiblings) {
		this.retrieveSiblings = retrieveSiblings;
	}

	public boolean isRetrieveOperationResult() {
		return retrieveOperationResult;
	}

	public void setRetrieveOperationResult(boolean retrieveOperationResult) {
		this.retrieveOperationResult = retrieveOperationResult;
	}

	public boolean isCreateHandlerDto() {
		return createHandlerDto;
	}

	public void setCreateHandlerDto(boolean createHandlerDto) {
		this.createHandlerDto = createHandlerDto;
	}
}

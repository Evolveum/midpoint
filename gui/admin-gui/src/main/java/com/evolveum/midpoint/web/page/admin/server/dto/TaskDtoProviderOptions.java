package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;

/**
 * @author mederly
 */
public class TaskDtoProviderOptions implements Serializable {

    // default values must be 'most informative'
    private boolean useClusterInformation = true;
    private boolean resolveObjectRef = true;
    private boolean resolveOwnerRef = true;            // currently unused
    private boolean getNextRunStartTime = true;
    private boolean retrieveModelContext = true;
    private boolean getTaskParent = true;

    public static TaskDtoProviderOptions minimalOptions() {
        TaskDtoProviderOptions options = new TaskDtoProviderOptions();
        options.setUseClusterInformation(false);
        options.setResolveObjectRef(false);
        options.setResolveOwnerRef(false);
        options.setGetNextRunStartTime(false);
        options.setRetrieveModelContext(false);
        options.setGetTaskParent(false);
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

    public boolean isGetTaskParent() {
        return getTaskParent;
    }

    public void setGetTaskParent(boolean getTaskParent) {
        this.getTaskParent = getTaskParent;
    }
}

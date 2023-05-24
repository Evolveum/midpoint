/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.io.Serializable;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

public class CapabilitiesDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean activation;

    private boolean activationLockoutStatus;

    private boolean activationStatus;

    private boolean activationValidity;

    private boolean auxiliaryObjectClasses;

    private boolean countObjects;

    private boolean pagedSearch;

    private boolean password;

    private boolean credentials;

    private boolean liveSync;

    private boolean testConnection;

    private boolean create;

    private boolean update;

    private boolean delete;

    private boolean read;

    private boolean script;

    private boolean runAs;

    public CapabilitiesDto(ResourceType resource){
        activation = ResourceTypeUtil.isActivationCapabilityEnabled(resource, null);
        activationLockoutStatus = ResourceTypeUtil.isActivationLockoutStatusCapabilityEnabled(resource, null);
        activationStatus = ResourceTypeUtil.isActivationStatusCapabilityEnabled(resource, null);
        activationValidity = ResourceTypeUtil.isActivationValidityFromCapabilityEnabled(resource, null);
        activationValidity = ResourceTypeUtil.isActivationValidityToCapabilityEnabled(resource, null);
        auxiliaryObjectClasses = ResourceTypeUtil.isAuxiliaryObjectClassCapabilityEnabled(resource);
        countObjects = ResourceTypeUtil.isCountObjectsCapabilityEnabled(resource);
        pagedSearch = ResourceTypeUtil.isPagedSearchCapabilityEnabled(resource);
        password = ResourceTypeUtil.isPasswordCapabilityEnabled(resource, null);
        credentials = ResourceTypeUtil.isCredentialsCapabilityEnabled(resource, null);
        liveSync = ResourceTypeUtil.isLiveSyncCapabilityEnabled(resource);
        testConnection = ResourceTypeUtil.isTestConnectionCapabilityEnabled(resource);
        create = ResourceTypeUtil.isCreateCapabilityEnabled(resource);
        update = ResourceTypeUtil.isUpdateCapabilityEnabled(resource);
        delete = ResourceTypeUtil.isDeleteCapabilityEnabled(resource);
        read = ResourceTypeUtil.isReadCapabilityEnabled(resource);
        script = ResourceTypeUtil.isScriptCapabilityEnabled(resource);
        runAs = ResourceTypeUtil.isCapabilityEnabled(resource, RunAsCapabilityType.class);
    }

    public boolean isActivation() {
        return activation;
    }

    public void setActivation(boolean activation) {
        this.activation = activation;
    }

    public boolean isActivationLockoutStatus() {
        return activationLockoutStatus;
    }

    public void setActivationLockoutStatus(boolean activationLockoutStatus) {
        this.activationLockoutStatus = activationLockoutStatus;
    }

    public boolean isActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(boolean activationStatus) {
        this.activationStatus = activationStatus;
    }

    public boolean isActivationValidity() {
        return activationValidity;
    }

    public void setActivationValidity(boolean activationValidity) {
        this.activationValidity = activationValidity;
    }

    public boolean isAuxiliaryObjectClasses() {
        return auxiliaryObjectClasses;
    }

    public void setAuxiliaryObjectClasses(boolean auxiliaryObjectClasses) {
        this.auxiliaryObjectClasses = auxiliaryObjectClasses;
    }

    public boolean isCountObjects() {
        return countObjects;
    }

    public void setCountObjects(boolean countObjects) {
        this.countObjects = countObjects;
    }

    public boolean isPagedSearch() {
        return pagedSearch;
    }

    public void setPagedSearch(boolean pagedSearch) {
        this.pagedSearch = pagedSearch;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public boolean isCredentials() {
        return credentials;
    }

    public void setCredentials(boolean credentials) {
        this.credentials = credentials;
    }

    public boolean isLiveSync() {
        return liveSync;
    }

    public void setLiveSync(boolean liveSync) {
        this.liveSync = liveSync;
    }

    public boolean isTestConnection() {
        return testConnection;
    }

    public void setTestConnection(boolean testConnection) {
        this.testConnection = testConnection;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isScript() {
        return script;
    }

    public void setScript(boolean script) {
        this.script = script;
    }

    public boolean isRunAs() {
        return runAs;
    }

    public void setRunAs(boolean runAs) {
        this.runAs = runAs;
    }

}

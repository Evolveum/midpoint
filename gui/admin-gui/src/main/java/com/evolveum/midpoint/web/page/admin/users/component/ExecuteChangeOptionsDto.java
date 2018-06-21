/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsDto implements Serializable {

    public static final String F_FORCE = "force";
    public static final String F_RECONCILE = "reconcile";
    public static final String F_RECONCILE_AFFECTED = "reconcileAffected";
    public static final String F_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";
    public static final String F_KEEP_DISPLAYING_RESULTS = "keepDisplayingResults";

    private boolean force;
    private boolean reconcile;
    private boolean reconcileAffected;
    private boolean executeAfterAllApprovals = true;
    private boolean keepDisplayingResults;

    public ExecuteChangeOptionsDto() {
    }

    public static ExecuteChangeOptionsDto createFromSystemConfiguration() {
        return new ExecuteChangeOptionsDto(MidPointApplication.get().getSystemConfigurationIfAvailable());
    }

    private ExecuteChangeOptionsDto(SystemConfigurationType config) {
        if (config != null && config.getRoleManagement() != null && config.getRoleManagement().isDefaultExecuteAfterAllApprovals() != null) {
            executeAfterAllApprovals = config.getRoleManagement().isDefaultExecuteAfterAllApprovals();
        }
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isReconcile() {
        return reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }

    public boolean isReconcileAffected() {
        return reconcileAffected;
    }

    public void setReconcileAffected(boolean reconcileAffected) {
        this.reconcileAffected = reconcileAffected;
    }

    public boolean isExecuteAfterAllApprovals() {
        return executeAfterAllApprovals;
    }

    public void setExecuteAfterAllApprovals(boolean executeAfterAllApprovals) {
        this.executeAfterAllApprovals = executeAfterAllApprovals;
    }

    public boolean isKeepDisplayingResults() {
        return keepDisplayingResults;
    }

    public void setKeepDisplayingResults(boolean keepDisplayingResults) {
        this.keepDisplayingResults = keepDisplayingResults;
    }

    @NotNull
    public ModelExecuteOptions createOptions() {
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setForce(isForce());
        options.setReconcile(isReconcile());
        options.setReconcileAffected(isReconcileAffected());
        options.setExecuteImmediatelyAfterApproval(!isExecuteAfterAllApprovals());

        return options;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Options{force=").append(isForce());
        builder.append(",reconcile=").append(isReconcile());
        builder.append(",reconcileAffected=").append(isReconcileAffected());
        builder.append(",keepDisplayingResults=").append(isKeepDisplayingResults());
        builder.append('}');

        return builder.toString();
    }
}

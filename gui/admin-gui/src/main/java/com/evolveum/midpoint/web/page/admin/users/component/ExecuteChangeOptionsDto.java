/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsDto implements Serializable {

    public static final String F_FORCE = "force";
    public static final String F_RECONCILE = "reconcile";
    public static final String F_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";
    public static final String F_KEEP_DISPLAYING_RESULTS = "keepDisplayingResults";
    public static final String F_TRACING = "tracing";
    public static final String F_TRACING_CHOICES = "tracingChoices";
    public static final String F_SAVE_IN_BACKGROUND = "saveInBackground";

    private boolean force;
    private boolean reconcile;
    private boolean executeAfterAllApprovals = true;
    private boolean keepDisplayingResults;
    private boolean saveInBackground;
    private TracingProfileType tracing;
    private List<TracingProfileType> tracingChoices;

    public ExecuteChangeOptionsDto() {
    }

    public static ExecuteChangeOptionsDto createFromSystemConfiguration() {
        return new ExecuteChangeOptionsDto(MidPointApplication.get().getSystemConfigurationIfAvailable());
    }

    private ExecuteChangeOptionsDto(SystemConfigurationType config) {
        if (config != null && config.getRoleManagement() != null && config.getRoleManagement().isDefaultExecuteAfterAllApprovals() != null) {
            executeAfterAllApprovals = config.getRoleManagement().isDefaultExecuteAfterAllApprovals();
        }
        tracingChoices = new ArrayList<>();
        if (config != null && config.getInternals() != null && config.getInternals().getTracing() != null) {
            for (TracingProfileType profile : config.getInternals().getTracing().getProfile()) {
                if (!Boolean.FALSE.equals(profile.isVisible())) {
                    tracingChoices.add(profile.clone());
                }
            }
        }
        if (tracingChoices.isEmpty()) {
            tracingChoices.add(new TracingProfileType(MidPointApplication.get().getPrismContext()).name("Minimal"));
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

    public TracingProfileType getTracing() {
        return tracing;
    }

    public void setTracing(TracingProfileType tracing) {
        this.tracing = tracing;
    }

    public List<TracingProfileType> getTracingChoices() {
        return tracingChoices;
    }

    public void setTracingChoices(List<TracingProfileType> tracingChoices) {
        this.tracingChoices = tracingChoices;
    }

    public boolean isSaveInBackground() {
        return saveInBackground;
    }

    public void setSaveInBackground(boolean saveInBackground) {
        this.saveInBackground = saveInBackground;
    }

    @NotNull
    public ModelExecuteOptions createOptions(PrismContext ignored) {
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.force(isForce());
        options.reconcile(isReconcile());
        options.executeImmediatelyAfterApproval(!isExecuteAfterAllApprovals());
        options.tracingProfile(tracing);
        return options;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Options{force=").append(isForce());
        builder.append(",reconcile=").append(isReconcile());
        builder.append(",keepDisplayingResults=").append(isKeepDisplayingResults());
        builder.append(",saveInBackground=").append(isSaveInBackground());
        builder.append(",tracing=").append(tracing);
        builder.append('}');

        return builder.toString();
    }
}

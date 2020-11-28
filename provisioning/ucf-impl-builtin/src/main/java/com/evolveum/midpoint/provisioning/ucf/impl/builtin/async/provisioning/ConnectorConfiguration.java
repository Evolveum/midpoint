/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
@SuppressWarnings({ "unused" })
public class ConnectorConfiguration {

    /**
     * List of targets to which operation requests have to be sent. There can be more than one.
     * We support simple failover here. In the future we can support load balancing as well as
     * failover with specified priorities.
     */
    private AsyncProvisioningTargetsType targets;

    /**
     * What predefined transformation has to be used (if any)?
     */
    private PredefinedOperationRequestTransformationType predefinedTransformation;

    /**
     * Expression that converts operation into operation request. It is applied for all targets.
     * In the future we can add transformers specific for individual targets.
     */
    private ExpressionType transformExpression;

    /**
     * Do we support operation execution acknowledgements? If not, each operation is considered
     * to be immediately executed, so no pending operations records are created. But if yes,
     * pending operations are created, and upon receipt of a confirmation, such operation is
     * marked as executed. Just like in manual resources.
     *
     * The default is "false". The acknowledgements are very experimental, and not implemented fully.
     */
    private boolean operationExecutionConfirmation;

    @ConfigurationItem
    public AsyncProvisioningTargetsType getTargets() {
        return targets;
    }

    public void setTargets(AsyncProvisioningTargetsType targets) {
        this.targets = targets;
    }

    @ConfigurationItem
    public PredefinedOperationRequestTransformationType getPredefinedTransformation() {
        return predefinedTransformation;
    }

    public void setPredefinedTransformation(PredefinedOperationRequestTransformationType predefinedTransformation) {
        this.predefinedTransformation = predefinedTransformation;
    }

    @ConfigurationItem
    public ExpressionType getTransformExpression() {
        return transformExpression;
    }

    public void setTransformExpression(ExpressionType transformExpression) {
        this.transformExpression = transformExpression;
    }

    @ConfigurationItem
    public boolean isOperationExecutionConfirmation() {
        return operationExecutionConfirmation;
    }

    public void setOperationExecutionConfirmation(boolean operationExecutionConfirmation) {
        this.operationExecutionConfirmation = operationExecutionConfirmation;
    }

    public void validate() {
        if (getAllTargets().isEmpty()) {
            throw new IllegalStateException("No asynchronous provisioning targets were configured");
        }
    }

    /**
     * Converts "user friendly" targets configuration (using item names to indicate types)
     * into a homogeneous list of targets. Sorts them according to the order property.
     */
    @NotNull
    List<AsyncProvisioningTargetType> getAllTargets() {
        List<AsyncProvisioningTargetType> allTargets = new ArrayList<>();
        if (targets != null) {
            allTargets.addAll(targets.getJms());
            allTargets.addAll(targets.getArtemis());
            allTargets.addAll(targets.getOther());
        }
        allTargets.sort(Comparator.nullsLast(Comparator.comparing(AsyncProvisioningTargetType::getOrder)));
        return allTargets;
    }

    boolean hasTargetsChanged(ConnectorConfiguration other) {
        // we can consider weaker comparison here in the future
        return other == null || !Objects.equals(other.targets, targets);
    }
}

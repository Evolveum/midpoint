
/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public interface ModelContext<F extends ObjectType> extends Serializable, DebugDumpable {

    String getRequestIdentifier();

    ModelState getState();

    ModelElementContext<F> getFocusContext();

    @NotNull
    ModelElementContext<F> getFocusContextRequired();

    @NotNull
    Collection<? extends ModelProjectionContext> getProjectionContexts();

    ModelProjectionContext findProjectionContextByKeyExact(@NotNull ProjectionContextKey key);

    @NotNull Collection<? extends ModelProjectionContext> findProjectionContexts(@NotNull ProjectionContextFilter filter);

    ModelExecuteOptions getOptions();

    @NotNull
    PartialProcessingOptionsType getPartialProcessingOptions();

    Class<F> getFocusClass();

    void reportProgress(ProgressInformation progress);

    DeltaSetTriple<? extends EvaluatedAssignment> getEvaluatedAssignmentTriple();

    @NotNull
    Stream<? extends EvaluatedAssignment> getEvaluatedAssignmentsStream();

    @NotNull
    Collection<? extends EvaluatedAssignment> getNonNegativeEvaluatedAssignments();

    @NotNull
    Collection<? extends EvaluatedAssignment> getAllEvaluatedAssignments();

    ObjectTemplateType getFocusTemplate();

    PrismObject<SystemConfigurationType> getSystemConfiguration();  // beware, may be null - use only as a performance optimization

    String getChannel();

    int getAllChanges() throws SchemaException;

    // For diagnostic purposes (this is more detailed than rule-related part of LensContext debugDump,
    // while less detailed than that part of detailed LensContext debugDump).
    default String dumpAssignmentPolicyRules(int indent) {
        return dumpAssignmentPolicyRules(indent, false);
    }

    String dumpAssignmentPolicyRules(int indent, boolean alsoMessages);

    default String dumpObjectPolicyRules(int indent) {
        return dumpObjectPolicyRules(indent, false);
    }

    String dumpObjectPolicyRules(int indent, boolean alsoMessages);

    Map<String, Collection<? extends Containerable>> getHookPreviewResultsMap();

    @NotNull <T> List<T> getHookPreviewResults(@NotNull Class<T> clazz);

    @Nullable
    PolicyRuleEnforcerPreviewOutputType getPolicyRuleEnforcerPreviewOutput();

    default boolean isSimulation() {
        return !getTaskExecutionMode().isFullyPersistent();
    }

    @NotNull
    ObjectTreeDeltas<F> getTreeDeltas();

    Collection<ProjectionContextKey> getHistoricResourceObjects();

    Long getSequenceCounter(String sequenceOid);

    void setSequenceCounter(String sequenceOid, long counter);

    String getTaskTreeOid(Task task, OperationResult result);

    ExpressionProfile getPrivilegedExpressionProfile();

    @NotNull TaskExecutionMode getTaskExecutionMode();
}

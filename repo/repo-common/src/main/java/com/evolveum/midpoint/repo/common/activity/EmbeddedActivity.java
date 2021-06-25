/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

import org.jetbrains.annotations.NotNull;

public class EmbeddedActivity<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        extends Activity<WD, AH> {

    /**
     * Supplier for the execution objects.
     */
    @NotNull private final ExecutionSupplier<WD, AH> executionSupplier;

    /** TODO */
    @NotNull private final CandidateIdentifierFormatter candidateIdentifierFormatter;

    @NotNull private final Activity<WD, AH> parent;

    private EmbeddedActivity(@NotNull ActivityDefinition<WD> definition, @NotNull ExecutionSupplier<WD, AH> executionSupplier,
            @NotNull CandidateIdentifierFormatter candidateIdentifierFormatter, @NotNull ActivityTree tree, Activity<WD, AH> parent) {
        super(definition, tree);
        this.executionSupplier = executionSupplier;
        this.candidateIdentifierFormatter = candidateIdentifierFormatter;
        this.parent = parent;
    }

    public static <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> EmbeddedActivity<WD, AH> create(
            @NotNull ActivityDefinition<WD> definition,
            @NotNull ExecutionSupplier<WD, AH> executionSupplier,
            @NotNull CandidateIdentifierFormatter candidateIdentifierFormatter,
            @NotNull Activity<WD, AH> parent) {
        return new EmbeddedActivity<>(definition, executionSupplier, candidateIdentifierFormatter, parent.getTree(), parent);
    }

    @NotNull
    @Override
    public AH getHandler() {
        return parent.getHandler();
    }

    @Override
    protected @NotNull ExecutionSupplier<WD, AH> getLocalExecutionSupplier() {
        return executionSupplier;
    }

    @Override
    protected @NotNull CandidateIdentifierFormatter getCandidateIdentifierFormatter() {
        return candidateIdentifierFormatter;
    }

    @NotNull
    @Override
    public Activity<WD, AH> getParent() {
        return parent;
    }
}

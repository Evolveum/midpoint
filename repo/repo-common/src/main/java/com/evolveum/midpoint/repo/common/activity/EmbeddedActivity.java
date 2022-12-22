/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

/**
 * A sub-activity that does not have its own (explicit) definition. Typical examples are sub-activities of reconciliation,
 * cleanup, or focus validity scan activity.
 */
public class EmbeddedActivity<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        extends Activity<WD, AH> {

    /**
     * Supplier for the run objects. Usually a simple closure pointing to appropriate constructor.
     */
    @NotNull private final ActivityRunSupplier<WD, AH> activityRunSupplier;

    /**
     * A piece of code that is to be executed _before_ this activity is executed.
     *
     * Currently executed for both local and distributing runs. (Not for delegating ones.)
     *
     * TODO better name, better explanation/reasoning - it is quite a hack now
     */
    @Nullable private final PreRunnable<WD, AH> preRunnable;

    /** Suggests identifier for this activity (when generating). */
    @NotNull private final CandidateIdentifierFormatter candidateIdentifierFormatter;

    /** Defines the activity state, e.g. its data type and persistence characteristics. */
    @NotNull private final ActivityStateDefinition<?> activityStateDefinition;

    @NotNull private final Activity<WD, AH> parent;

    private EmbeddedActivity(
            @NotNull ActivityDefinition<WD> definition,
            @NotNull ActivityRunSupplier<WD, AH> activityRunSupplier,
            @Nullable PreRunnable<WD, AH> preRunnable,
            @NotNull CandidateIdentifierFormatter candidateIdentifierFormatter,
            @NotNull ActivityStateDefinition<?> activityStateDefinition,
            @NotNull ActivityTree tree,
            @NotNull Activity<WD, AH> parent) {
        super(definition, tree);
        this.activityRunSupplier = activityRunSupplier;
        this.preRunnable = preRunnable;
        this.candidateIdentifierFormatter = candidateIdentifierFormatter;
        this.activityStateDefinition = activityStateDefinition;
        this.parent = parent;
    }

    /**
     * Creates an embedded activity.
     *
     * @param definition Definition to be used. Should be freely modifiable (typically cloned,
     * e.g. via {@link ActivityDefinition#cloneWithoutId()})!
     */
    public static <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> EmbeddedActivity<WD, AH> create(
            @NotNull ActivityDefinition<WD> definition,
            @NotNull ActivityRunSupplier<WD, AH> activityRunSupplier,
            @Nullable PreRunnable<WD, AH> preRunnable,
            @NotNull CandidateIdentifierFormatter candidateIdentifierFormatter,
            @NotNull ActivityStateDefinition<?> activityStateDefinition,
            @NotNull Activity<WD, AH> parent) {
        return new EmbeddedActivity<>(definition, activityRunSupplier, preRunnable, candidateIdentifierFormatter,
                activityStateDefinition, parent.getTree(), parent);
    }

    @NotNull
    @Override
    public AH getHandler() {
        return parent.getHandler();
    }

    @Override
    protected @NotNull ActivityRunSupplier<WD, AH> getLocalRunSupplier() {
        return activityRunSupplier;
    }

    @Override
    protected @NotNull CandidateIdentifierFormatter getCandidateIdentifierFormatter() {
        return candidateIdentifierFormatter;
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getActivityStateDefinition() {
        return activityStateDefinition;
    }

    @NotNull
    @Override
    public Activity<WD, AH> getParent() {
        return parent;
    }

    public @Nullable PreRunnable<WD, AH> getPreRunnable() {
        return preRunnable;
    }
}

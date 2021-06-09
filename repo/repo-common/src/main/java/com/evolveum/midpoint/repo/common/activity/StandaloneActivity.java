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

public class StandaloneActivity<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        extends Activity<WD, AH> {

    /**
     * Handler for the activity.
     */
    @NotNull private final AH handler;

    /**
     * Reference to the parent activity (if not root).
     */
    private final Activity<?, ?> parent;

    private StandaloneActivity(@NotNull ActivityDefinition<WD> definition, @NotNull AH handler,
            @NotNull ActivityTree tree, Activity<?, ?> parent) {
        super(definition, tree);
        this.handler = handler;
        this.parent = parent;
    }

    static <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> StandaloneActivity<WD, AH> createRoot(
            @NotNull ActivityDefinition<WD> definition, @NotNull AH handler, @NotNull ActivityTree tree) {
        return new StandaloneActivity<>(definition, handler, tree, null);
    }

    public static <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> StandaloneActivity<WD, AH> createNonRoot(
            @NotNull ActivityDefinition<WD> definition, @NotNull AH handler, @NotNull Activity<?, ?> parent) {
        return new StandaloneActivity<>(definition, handler, parent.getTree(), parent);
    }

    @NotNull
    @Override
    public AH getHandler() {
        return handler;
    }

    @Override
    protected @NotNull ExecutionSupplier<WD, AH> getLocalExecutionSupplier() {
        return handler;
    }

    @Override
    protected @NotNull CandidateIdentifierFormatter getCandidateIdentifierFormatter() {
        return handler;
    }

    @Override
    public Activity<?, ?> getParent() {
        return parent;
    }
}

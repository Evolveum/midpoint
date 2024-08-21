/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Limited-use class that bundles a shadow with its state during post-processing.
 */
@Experimental
public record RepoShadowWithState(
        @NotNull RepoShadow shadow,
        @NotNull ShadowState state) implements DebugDumpable {

    static RepoShadowWithState existing(RepoShadow shadow) {
        return new RepoShadowWithState(shadow, ShadowState.EXISTING);
    }

    @Contract("!null -> new; null -> null")
    static RepoShadowWithState existingOptional(RepoShadow shadow) {
        return shadow != null ?
                new RepoShadowWithState(shadow, ShadowState.EXISTING) :
                null;
    }

    static RepoShadowWithState discovered(RepoShadow shadow) {
        return new RepoShadowWithState(shadow, ShadowState.DISCOVERED);
    }

    public RepoShadowWithState classified() {
        if (state == ShadowState.DISCOVERED) {
            return new RepoShadowWithState(shadow, ShadowState.DISCOVERED_CLASSIFIED);
        } else if (state == ShadowState.EXISTING) {
            return new RepoShadowWithState(shadow, ShadowState.EXISTING_CLASSIFIED);
        } else {
            throw new IllegalStateException("Cannot classify shadow in state " + state);
        }
    }

    public RepoShadowWithState withShadow(RepoShadow newShadow) {
        return new RepoShadowWithState(newShadow, state);
    }

    public @NotNull ShadowType getBean() {
        return shadow.getBean();
    }

    @Override
    public String debugDump(int indent) {
        return shadow.debugDump(indent); // FIXME add state
    }

    /**
     * FIXME Rethink these states! (Currently, they are used only for marks computation.
     *  Maybe they should look differently if they will be used for other purposes as well.)
     */
    public enum ShadowState {

        /** Shadow is to be created on the resource. */
        TO_BE_CREATED,

        /** Shadow already exists in the repository. */
        EXISTING,

        /** Shadow was just discovered on the resource, and was created in the repository. */
        DISCOVERED,

        /** Shadow was first classified, right after discovery. */
        DISCOVERED_CLASSIFIED,

        /** Existing shadow was classified or re-classified. */
        EXISTING_CLASSIFIED;

        public boolean isClassified() {
            return this == DISCOVERED_CLASSIFIED || this == EXISTING_CLASSIFIED;
        }
    }
}

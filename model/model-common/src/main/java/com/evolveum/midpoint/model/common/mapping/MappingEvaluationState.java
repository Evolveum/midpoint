/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping;

/**
 * @author semancik
 *
 */
public enum MappingEvaluationState {
    /**
     * Nothing is initialized, we are just preparing the mapping.
     */
    UNINITIALIZED,

    /**
     * Prepared for evaluation. Declarative part of mapping is
     * set and parsed. Variables are set. Source are set, but they can still change.
     * The purpose of this state is that we can check if a mapping is activated
     * (i.e. if the input changes will "trigger" the mapping). And we can sort
     * mapping evaluation according to their dependencies.
     */
    PREPARED,

    /**
     * Mapping was evaluated, results are produced.
     */
    EVALUATED;
}

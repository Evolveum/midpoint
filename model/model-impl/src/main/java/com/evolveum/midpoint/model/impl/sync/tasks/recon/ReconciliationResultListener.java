/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

/**
 * Testability interface. It is used in the tests to check correctness
 * of reconciliation run.
 *
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface ReconciliationResultListener {

    void process(ReconciliationResult reconResult);

}

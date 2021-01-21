/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
public interface ReconciliationTaskResultListener {

    void process(ReconciliationTaskResult reconResult);

}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Here are tests that check basic functionality of specified tasks:
 *
 * - recomputation,
 * - import,
 * - reconciliation,
 * - live sync,
 * - cleanup, etc.
 *
 * Aspects checked are:
 *
 * - basic ability to run (with legacy / new work definition),
 * - ability to be suspended and resumed,
 * - progress reporting,
 * - statistics reporting,
 * - state management (i.e. keeping persistent state),
 * - error handling.
 *
 * With scenarios containing slow resources, slow processing, various distribution modes (single thread, multiple threads,
 * multiple workers), and so on.
 *
 * We _do not_ check details of the task functionality, except for cases when there is no dedicated test for such kind
 * of tasks.
 *
 * As a special case, we use NoOp activity to test some extra features
 * - see {@link com.evolveum.midpoint.model.intest.tasks.TestNoOpTask}.
 */
package com.evolveum.midpoint.model.intest.tasks;

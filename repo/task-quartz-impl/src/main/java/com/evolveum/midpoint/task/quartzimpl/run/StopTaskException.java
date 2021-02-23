/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Signals that we should exit task execution.
 * (Useful for tightly-bound recurring tasks.)
 */
@Experimental
class StopTaskException extends Exception {
}

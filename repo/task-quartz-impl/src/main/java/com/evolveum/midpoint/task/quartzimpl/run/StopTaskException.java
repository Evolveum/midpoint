/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.hooks;

/**
 * @author semancik
 *
 */
public enum HookOperationMode {

    /**
     * The hook operates in background. If the hook returns then the hook operation is
     * completed. The caller reclaims ownership of the task. The thread may be used to
     * continue execution of the task and to complete the task.
     */
    FOREGROUND,

    /**
     * The hook operates in background. The hook will use its own thread for execution.
     * After the hook is done with executing it will "return" control to the next handler
     * in the task handler stack.
     * The ownership of the task remains inside the hook. The thread returned from the
     * hook method must be used only to return back to the caller (e.g. GUI) and inform
     * him that the task was switched to background.
     */
    BACKGROUND,

    /**
     * Hook completes with an error (TODO).
     */
    ERROR;

}

/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

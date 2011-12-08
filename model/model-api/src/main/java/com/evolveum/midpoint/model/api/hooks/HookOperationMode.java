/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
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
	BACKGROUND;

}

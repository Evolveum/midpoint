/*
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
package com.evolveum.midpoint.task.api;

import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public interface TaskHandler {
	
	public TaskRunResult run(Task task);
	
	public Long heartbeat(Task task);
	
	// TODO: fix signature
	public void refreshStatus(Task task);

    /**
     * Returns a category name for a given task. In most cases, the name would be independent of concrete task.
     * @param task a task, whose category is to be determined; if getCategoryNames() returns null, this method
     *             has to accept null value as this parameter, and return the (one) category name that it gives
     *             to all tasks
     * @return a user-understandable name, like "LiveSync" or "Workflow"
     */
    public String getCategoryName(Task task);

    /**
     * Returns names of task categories provided by this handler. Usually it will be one-item list.
     * @return a list of category names; may be null - in that case the category info is given by getCategoryName(null)
     */
    public List<String> getCategoryNames();

}

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

package com.evolveum.midpoint.wf.api;

import java.util.Map;

/**
 * An interface through which external observers can be notified about work item related events.
 *
 * EXPERIMENTAL. This interface will probably change in near future.
 *
 * @author mederly
 */
public interface WorkItemListener {

    /**
     * This method is called by wf module when a work item is created.
     *
     * @param workItemName name of the work item
     * @param assigneeOid OID of the user to which the work item is assigned
     * @param processInstanceName name of the process instance
     * @param processVariables process instance variables (todo fixme this is dangerous, reconsider)
     */
    public void onWorkItemCreation(String workItemName, String assigneeOid, String processInstanceName, Map<String, Object> processVariables);

    /**
     * This method is called by wf module when a work item is completed.
     *
     * @param workItemName name of the work item
     * @param assigneeOid OID of the user to which the work item is assigned
     * @param processInstanceName name of the process instance
     * @param processVariables process instance variables (todo fixme this is dangerous, reconsider)
     * @param decision decision of the user
     */
    public void onWorkItemCompletion(String workItemName, String assigneeOid, String processInstanceName, Map<String, Object> processVariables, String decision);
}

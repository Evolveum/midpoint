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

package com.evolveum.midpoint.wf.processes.general;

/**
 * @author mederly
 */
public class Constants {

    /**
     * This is a name of default panel that displays information about wf process instance running this process.
     * TODO this is a dependency on gui module; however, it is not clear how to do this more cleanly
     * TODO instead of class names here should be something more abstract
     */
    //public static final String DEFAULT_PANEL_NAME = "ItemApprovalDetailsDefaultPanel";
    public static final String DEFAULT_PANEL_NAME = "com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalPanel";
}

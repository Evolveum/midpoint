/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 15:11
 * To change this template use File | Settings | File Templates.
 */
public class StartProcessInstruction {

    private Map<String,Object> processVariables = new HashMap<String,Object>();
    private String processName;
    private PolyStringType taskName;
    private boolean simple;

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public void setProcessName(String name) {
        processName = name;
    }

    public String getProcessName() {
        return processName;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public void addProcessVariable(String name, Object value) {
        processVariables.put(name, value);
    }

    public PolyStringType getTaskName() {
        return taskName;
    }

    public void setTaskName(PolyStringType taskName) {
        this.taskName = taskName;
    }

    public String toString() {
        return "ProcessStartCommand: processName = " + processName + ", simple: " + simple + ", variables: " + processVariables;
    }

}

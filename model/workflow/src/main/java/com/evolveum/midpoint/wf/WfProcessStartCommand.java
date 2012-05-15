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

package com.evolveum.midpoint.wf;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 15:11
 * To change this template use File | Settings | File Templates.
 */
public class WfProcessStartCommand {

    private Map<String,String> processVariables = new HashMap<String,String>();
    private String processName;
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

    public Map<String, String> getProcessVariables() {
        return processVariables;
    }

    public void addProcessVariable(String name, String value) {
        processVariables.put(name, value);
    }

    public String toString() {
        return "ProcessStartCommand: processName = " + processName + ", simple: " + simple + ", variables: " + processVariables;
    }

}

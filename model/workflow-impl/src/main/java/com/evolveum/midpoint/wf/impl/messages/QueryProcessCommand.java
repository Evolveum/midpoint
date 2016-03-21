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

package com.evolveum.midpoint.wf.impl.messages;

/**
 * Command to query a process instance.
 */
public class QueryProcessCommand {

    private String pid;
    private String taskOid;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTaskOid() {
        return taskOid;
    }

    public void setTaskOid(String taskOid) {
        this.taskOid = taskOid;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pid=" + pid + ", task=" + taskOid + "]";
    }
}

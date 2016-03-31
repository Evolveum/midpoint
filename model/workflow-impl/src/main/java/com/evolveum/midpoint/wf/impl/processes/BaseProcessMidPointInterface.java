/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processes;

import org.springframework.beans.factory.BeanNameAware;

import java.util.Map;

/**
 * @author mederly
 */
public abstract class BaseProcessMidPointInterface implements ProcessMidPointInterface, BeanNameAware{

    private String beanName;

    // Variable reflecting the process status, like "your request was approved by
    // engineering group, and is being sent to the management". Stored into wfStatus task extension property.
    // [String]
    public static final String VARIABLE_WF_STATE = "wfState";

    // Basic decision returned from a workflow process.
    // for most work items it is simple __APPROVED__ or __REJECTED__, but in principle this can be any string value
    public static final String VARIABLE_WF_ANSWER = "wfAnswer";

    @Override
    public String getAnswer(Map<String, Object> variables) {
        return (String) variables.get(VARIABLE_WF_ANSWER);
    }

    @Override
    public String getState(Map<String, Object> variables) {
        return (String) variables.get(VARIABLE_WF_STATE);
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

}

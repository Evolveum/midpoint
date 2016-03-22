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

import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author mederly
 */
@Component
public class ProcessInterfaceFinder implements BeanFactoryAware {

    private BeanFactory beanFactory;

    public ProcessMidPointInterface getProcessInterface(Map<String, Object> variables) {
        String interfaceBeanName = (String) variables.get(CommonProcessVariableNames.VARIABLE_PROCESS_INTERFACE_BEAN_NAME);
        if (interfaceBeanName == null) {
            throw new IllegalStateException("No " + CommonProcessVariableNames.VARIABLE_PROCESS_INTERFACE_BEAN_NAME + " variable found");
        }
        return beanFactory.getBean(interfaceBeanName, ProcessMidPointInterface.class);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}

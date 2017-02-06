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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public abstract class BaseProcessMidPointInterface implements ProcessMidPointInterface, BeanNameAware {

	@Autowired
	protected PrismContext prismContext;

	private String beanName;

    @Override
    public String getAnswer(Map<String, Object> variables) {
        return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_WF_ANSWER, String.class);
    }

    @Override
    public String getState(Map<String, Object> variables) {
        return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_WF_STATE, String.class);
    }

	@Override
	public Integer getStageNumber(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_STAGE_NUMBER, Integer.class);
	}

	@Override
	public Integer getStageCount(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_STAGE_COUNT, Integer.class);
	}

	@Override
	public String getStageName(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_STAGE_NAME, String.class);
	}

	@Override
	public String getStageDisplayName(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_STAGE_DISPLAY_NAME, String.class);
	}

	@Override
	public Integer getEscalationLevelNumber(Map<String, Object> variables) {
		return ActivitiUtil.getEscalationLevelNumber(variables);
	}

	@Override
	public String getEscalationLevelName(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_ESCALATION_LEVEL_NAME, String.class);
	}

	@Override
	public String getEscalationLevelDisplayName(Map<String, Object> variables) {
		return ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_ESCALATION_LEVEL_DISPLAY_NAME, String.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<InformationType> getAdditionalInformation(Map<String, Object> variables) {
		List<InformationType> info = ActivitiUtil.getVariable(variables, CommonProcessVariableNames.ADDITIONAL_INFORMATION,
				List.class, prismContext);
		return info != null ? info : Collections.emptyList();
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

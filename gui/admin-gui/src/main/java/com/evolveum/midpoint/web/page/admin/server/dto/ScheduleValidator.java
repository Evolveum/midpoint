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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ScheduleValidator extends AbstractFormValidator {

    private static final Trace LOGGER = TraceManager.getTrace(ScheduleValidator.class);

    private AjaxCheckBox recurring;
    private AjaxCheckBox bound;
    private TextField<Integer> interval;
    private TextField<String> cron;
    private TaskManager taskManager;

    public ScheduleValidator(TaskManager manager, AjaxCheckBox recurring, AjaxCheckBox bound, TextField<Integer> interval, TextField<String> cron) {
        //System.out.println("new ScheduleValidator: recurring = " + recurring + ", bound = " + bound);
        taskManager = manager;
        this.recurring = recurring;
        this.bound = bound;
        this.interval = interval;
        this.cron = cron;
    }

    @Override
	public FormComponent<?>[] getDependentFormComponents() {
    	List<FormComponent<?>> dependentComponents = new ArrayList<>();
    	if (interval.isEnabled()) {
    		dependentComponents.add(interval);
    	}
    	
    	if (recurring.isEnabled()) {
    		dependentComponents.add(recurring);
    	}
    	
    	if (bound.isEnabled()) {
    		dependentComponents.add(bound);
    	}
    	
    	return dependentComponents.toArray(new FormComponent<?>[]{});    // todo is this correct? (cron should not be here, as it is not always present...)
	}

	@Override
	public void validate(Form<?> form) {

//        if (recurring == null)
//            System.out.println("recurring: = null");
//        else
//            System.out.println("recurring: " + recurring.getModelObject());
//
//        if (bound == null)
//            System.out.println("bound: = null");
//        else
//            System.out.println("bound: " + bound.getModelObject());
//
//        if (interval == null)
//            System.out.println("interval: = null");
//        else
//            System.out.println("interval: " + interval.getModelObject());
//
//        if (cron == null)
//            System.out.println("cron: = null");
//        else
//            System.out.println("cron: " + cron.getModelObject());
//
//        System.out.println("===");

        if (recurring.getModelObject()) {

			Integer intervalValue = interval.getModelObject();
			if (intervalValue != null && intervalValue <= 0) {
                error(interval, "pageTask.scheduleValidation.intervalNotPositive");
            }

			if (bound.getModelObject()) {

				if (intervalValue == null) {
				    error(interval, "pageTask.scheduleValidation.noInterval");
			    }

            } else {

				String cronValue = cron.getModelObject();
				if (intervalValue != null && !StringUtils.isEmpty(cronValue)) {
                    error(interval, "pageTask.scheduleValidation.bothIntervalAndCron");
                }

                if (!StringUtils.isEmpty(cronValue)) {
                    ParseException pe = taskManager.validateCronExpression(cronValue);
                    if (pe != null) {
                        error(cron, "pageTask.scheduleValidation.invalidCronSpecification");
                        LOGGER.warn("Invalid cron-like specification: " + cronValue + ": " + pe);
                    }
                }
            }
		}
	}

}

/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InternalsConfigDto;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(url = "/admin/config/internals", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_INTERNALS_URL,
                label = "PageInternals.auth.configInternals.label", description = "PageInternals.auth.configInternals.description")})
public class PageInternals extends PageAdminConfiguration {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageInternals.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_OFFSET = "offset";
    private static final String ID_BUTTON_SAVE = "save";
    private static final String ID_BUTTON_RESET = "reset";
    private static final String ID_DEBUG_UTIL_FORM = "debugUtilForm";
    private static final String ID_SAVE_DEBUG_UTIL = "saveDebugUtil";
    private static final String ID_INTERNALS_CONFIG_FORM = "internalsConfigForm";
    private static final String ID_UPDATE_INTERNALS_CONFIG = "updateInternalsConfig";
    private static final String ID_CONSISTENCY_CHECKS = "consistencyChecks";
    private static final String ID_ENCRYPTION_CHECKS = "encryptionChecks";
    private static final String ID_READ_ENCRYPTION_CHECKS = "readEncryptionChecks";
    private static final String ID_TOLERATE_UNDECLARED_PREFIXES = "tolerateUndeclaredPrefixes";
    private static final String ID_DETAILED_DEBUG_DUMP = "detailedDebugDump";
    
    private static final String ID_TRACES_FORM = "tracesForm";
    private static final String ID_TRACES_TABLE = "tracesTable";
    private static final String ID_TRACE_TOGGLE = "traceToggle";
    private static final String ID_UPDATE_TRACES = "updateTraces";
    
    private static final String ID_COUNTERS_TABLE = "countersTable";
    private static final String ID_COUNTER_LABEL = "counterLabel";
    private static final String ID_COUNTER_VALUE = "counterValue";
    
    private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-8";

    @SpringBean(name = "clock")
    private Clock clock;

    private LoadableModel<XMLGregorianCalendar> model;
    private IModel<InternalsConfigDto> internalsModel;
    private Map<InternalOperationClasses,Boolean> tracesMap;

    public PageInternals() {
        model = new LoadableModel<XMLGregorianCalendar>() {
			private static final long serialVersionUID = 1L;

			@Override
            protected XMLGregorianCalendar load() {
                return clock.currentTimeXMLGregorianCalendar();
            }
        };

        internalsModel = new Model<>(new InternalsConfigDto());
        tracesMap = new HashMap<>();
        for (InternalOperationClasses op: InternalOperationClasses.values()) {
        	tracesMap.put(op, InternalMonitor.isTrace(op));
        }

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        DatePanel offset = new DatePanel(ID_OFFSET, model);
        mainForm.add(offset);

        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageInternals.button.changeTime")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);

        AjaxSubmitButton resetButton = new AjaxSubmitButton(ID_BUTTON_RESET, createStringResource("PageInternals.button.resetTimeChange")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                resetPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(resetButton);

        initDebugUtilForm();
        initInternalsConfigForm();
        initTraces();
        initCounters();
    }

	private void initDebugUtilForm() {
        Form form = new Form(ID_DEBUG_UTIL_FORM);
        form.setOutputMarkupId(true);
        add(form);

        CheckFormGroup detailed = new CheckFormGroup(ID_DETAILED_DEBUG_DUMP,
                new PropertyModel<Boolean>(internalsModel, InternalsConfigDto.F_DETAILED_DEBUG_DUMP),
                createStringResource("PageInternals.detailedDebugDump"), LABEL_SIZE, INPUT_SIZE);
        form.add(detailed);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_SAVE_DEBUG_UTIL,
                createStringResource("PageBase.button.update")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateDebugPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(update);
    }

    private void initInternalsConfigForm() {
        Form form = new Form(ID_INTERNALS_CONFIG_FORM);
        form.setOutputMarkupId(true);
        add(form);
        
        form.add(createCheckbox(ID_CONSISTENCY_CHECKS, InternalsConfigDto.F_CONSISTENCY_CHECKS));
        form.add(createCheckbox(ID_ENCRYPTION_CHECKS, InternalsConfigDto.F_ENCRYPTION_CHECKS));
        form.add(createCheckbox(ID_READ_ENCRYPTION_CHECKS, InternalsConfigDto.F_READ_ENCRYPTION_CHECKS));
        form.add(createCheckbox(ID_TOLERATE_UNDECLARED_PREFIXES, InternalsConfigDto.F_TOLERATE_UNDECLARED_PREFIXES));
        
        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE_INTERNALS_CONFIG,
                createStringResource("PageBase.button.update")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateInternalConfig(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(update);
    }
    
    private void initTraces() {
        Form form = new Form(ID_TRACES_FORM);
        form.setOutputMarkupId(true);
        add(form);
        
        ListView<InternalOperationClasses> tracesTable = new ListView<InternalOperationClasses>(ID_TRACES_TABLE, Arrays.asList(InternalOperationClasses.values())) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<InternalOperationClasses> item) {
				InternalOperationClasses operationClass = item.getModelObject();
				CheckFormGroup checkFormGroup = new CheckFormGroup(ID_TRACE_TOGGLE,
		                new PropertyModel<Boolean>(tracesMap, operationClass.getKey()),
		                createStringResource("InternalOperationClasses."+operationClass.getKey()), LABEL_SIZE, INPUT_SIZE);
				item.add(checkFormGroup);
			}
        
        };
        form.add(tracesTable);
        
        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE_TRACES,
                createStringResource("PageBase.button.update")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateTraces(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(update);
    }
    
    private void updateTraces(AjaxRequestTarget target){
        for (Entry<InternalOperationClasses, Boolean> entry: tracesMap.entrySet()) {
        	InternalMonitor.setTrace(entry.getKey(), entry.getValue());
        }

        LOGGER.trace("Updated traces: {}", tracesMap);
        success(getString("PageInternals.message.tracesUpdate"));
        target.add(getFeedbackPanel(), getInternalsConfigForm());
    }
    
    private CheckFormGroup createCheckbox(String id, String propName) {
    	return new CheckFormGroup(id,
                new PropertyModel<Boolean>(internalsModel, propName),
                createStringResource("PageInternals."+propName), LABEL_SIZE, INPUT_SIZE);
    }
    
    private void initCounters() {
    	
    	ListView<InternalCounters> countersTable = new ListView<InternalCounters>(ID_COUNTERS_TABLE, Arrays.asList(InternalCounters.values())) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<InternalCounters> item) {
				InternalCounters counter = item.getModelObject();
				Label label = new Label(ID_COUNTER_LABEL, createStringResource("InternalCounters."+counter.getKey()));
				item.add(label);
		    	
		    	Label valueLabel = new Label(ID_COUNTER_VALUE, new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						long val = InternalMonitor.getCount(counter);
						return Long.toString(val);
					}
				});
		    	item.add(valueLabel);
			}
    		
    	};
    	add(countersTable);
	}

    private Form getMainForm(){
        return (Form) get(ID_MAIN_FORM);
    }

    private Form getDebugUtilForm(){
        return (Form) get(ID_DEBUG_UTIL_FORM);
    }

    private Form getInternalsConfigForm(){
        return (Form) get(ID_INTERNALS_CONFIG_FORM);
    }

    private void updateDebugPerformed(AjaxRequestTarget target){
        internalsModel.getObject().saveDebugUtil();

        LOGGER.trace("Updated debug util, detailedDebugDump={}", DebugUtil.isDetailedDebugDump());
        success(getString("PageInternals.message.debugUpdatePerformed", DebugUtil.isDetailedDebugDump()));
        target.add(getFeedbackPanel(), getDebugUtilForm());
    }

    private void updateInternalConfig(AjaxRequestTarget target){
        internalsModel.getObject().saveInternalsConfig();

        LOGGER.trace("Updated internals config, consistencyChecks={},encryptionChecks={},readEncryptionChecks={}, QNameUtil.tolerateUndeclaredPrefixes={}",
                new Object[]{InternalsConfig.consistencyChecks, InternalsConfig.encryptionChecks,
                        InternalsConfig.readEncryptionChecks, QNameUtil.isTolerateUndeclaredPrefixes()});
        success(getString("PageInternals.message.internalsConfigUpdate", InternalsConfig.consistencyChecks,
                InternalsConfig.encryptionChecks, InternalsConfig.readEncryptionChecks, QNameUtil.isTolerateUndeclaredPrefixes()));
        target.add(getFeedbackPanel(), getInternalsConfigForm());
    }

    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(PageInternals.class.getName() + ".changeTime");
        XMLGregorianCalendar offset = model.getObject();
        if (offset != null) {
            clock.override(offset);
        }

        result.recordSuccess();
        showResult(result);
        target.add(getFeedbackPanel(), getMainForm());
    }

    private void resetPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(PageInternals.class.getName() + ".changeTimeReset");
        clock.resetOverride();
        model.reset();
        result.recordSuccess();
        showResult(result);
        target.add(getMainForm());
        target.add(getFeedbackPanel());
    }
}

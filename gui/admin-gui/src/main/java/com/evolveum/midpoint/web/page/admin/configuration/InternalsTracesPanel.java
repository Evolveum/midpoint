/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.MidpointForm;

public class InternalsTracesPanel extends BasePanel<Map<String,Boolean>>{

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InternalsTracesPanel.class);

    private static final String ID_FORM = "form";
        private static final String ID_TRACES_TABLE = "tracesTable";
        private static final String ID_TRACE_TOGGLE = "traceToggle";
        private static final String ID_UPDATE_TRACES = "updateTraces";
        private static final String LABEL_SIZE = "col-md-4";
        private static final String INPUT_SIZE = "col-md-8";

    private Map<String,Boolean> tracesMap;

    public InternalsTracesPanel(String id, Map<String,Boolean> traces) {
        super(id);
        this.tracesMap = traces;
    }

    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        Form form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        ListView<InternalOperationClasses> tracesTable = new ListView<InternalOperationClasses>(ID_TRACES_TABLE, Arrays.asList(InternalOperationClasses.values())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InternalOperationClasses> item) {
                InternalOperationClasses operationClass = item.getModelObject();
                CheckFormGroup checkFormGroup = new CheckFormGroup(ID_TRACE_TOGGLE,
                    new PropertyModel<>(tracesMap, operationClass.getKey()),
                        createStringResource("InternalOperationClasses."+operationClass.getKey()), LABEL_SIZE, INPUT_SIZE);
                item.add(checkFormGroup);
            }

        };
        form.add(tracesTable);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE_TRACES,
                createStringResource("PageBase.button.update")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                updateTraces(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        form.add(update);
    }

    private void updateTraces(AjaxRequestTarget target){
        for (Entry<String, Boolean> entry: tracesMap.entrySet()) {
            InternalOperationClasses ioc = findInternalOperationClass(entry.getKey());
            if (ioc == null) {
                continue;
            }
            InternalMonitor.setTrace(ioc, entry.getValue());
        }

        LOGGER.trace("Updated traces: {}", tracesMap);
        success(getString("PageInternals.message.tracesUpdate"));
        target.add(getPageBase().getFeedbackPanel(), getInternalsConfigForm());
    }

    private InternalOperationClasses findInternalOperationClass(String key) {

        if (key == null) {
            return null;
        }

        for (InternalOperationClasses ioc : InternalOperationClasses.values()) {
            if (key.equals(ioc.getKey())) {
                return ioc;
            }
        }

        return null;

    }

    private Form getInternalsConfigForm(){
        return (Form) get(ID_FORM);
    }
}

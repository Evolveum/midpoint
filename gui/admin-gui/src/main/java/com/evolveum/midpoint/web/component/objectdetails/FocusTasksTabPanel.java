/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.CasesListPanel;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author mederly
 * @author semancik
 */
public class FocusTasksTabPanel<F extends FocusType>
        extends AbstractObjectTabPanel<F> {
    private static final long serialVersionUID = 1L;

    protected static final String ID_TASK_TABLE = "taskTable";
    protected static final String ID_LABEL = "label";
    protected boolean tasksExist = false;
    private static final Trace LOGGER = TraceManager.getTrace(FocusTasksTabPanel.class);

    public FocusTasksTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusModel, boolean tasksExist) {
        super(id, mainForm, focusModel);
        this.tasksExist = tasksExist;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_LABEL, getPageBase().createStringResource(tasksExist ?
                "pageAdminFocus.task.descriptionHasTasks" : "pageAdminFocus.task.descriptionNoTasks"));
        label.setOutputMarkupId(true);
        add(label);

        CasesListPanel casesPanel = new CasesListPanel(ID_TASK_TABLE) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter getCasesFilter() {
                String oid = null;
                if (getObjectWrapper() == null || StringUtils.isEmpty(getObjectWrapper().getOid())) {
                    oid = "non-existent"; // TODO !!!!!!!!!!!!!!!!!!!!
                } else {
                    oid = getObjectWrapper().getOid();
                }
                return QueryUtils.filterForCasesOverUser(getPageBase().getPrismContext().queryFor(CaseType.class), oid)
                        .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                        .buildFilter();
            }

            @Override
            protected boolean isDashboard(){
                return true;
            }
        };
        casesPanel.add(new VisibleBehaviour(() -> tasksExist));
        casesPanel.setOutputMarkupId(true);
        add(casesPanel);
    }
}

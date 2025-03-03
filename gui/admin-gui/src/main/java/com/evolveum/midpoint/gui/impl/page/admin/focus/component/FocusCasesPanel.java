/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.page.admin.server.CasesTablePanel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serial;

/**
 * @author semancik
 */
@PanelType(name = "focusCases")
@PanelInstance(identifier = "focusCases",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = FocusType.class,
        display = @PanelDisplay(label = "pageAdminFocus.cases", icon = GuiStyleConstants.EVO_CASE_OBJECT_ICON, order = 70))
@Counter(provider = FocusCasesCounter.class)
public class FocusCasesPanel<F extends FocusType>
        extends AbstractObjectMainPanel<F, FocusDetailsModels<F>> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TASK_TABLE = "taskTable";

    public FocusCasesPanel(String id, FocusDetailsModels<F> focusModel, ContainerPanelConfigurationType config) {
        super(id, focusModel, config);
    }

    protected void initLayout() {
        CasesTablePanel casesPanel = new CasesTablePanel(ID_TASK_TABLE, getPanelConfiguration()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter getCasesFilter() {
                String oid = getObjectWrapper().getOid();
                return QueryUtils.filterForCasesOverObject(getPageBase().getPrismContext().queryFor(CaseType.class), oid)
                        .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                        .buildFilter();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_FOCUS_DETAILS_CASES_PANEL;
            }

            //fix for #10473. Implemented in the same way as for Assignments panel (the search is remembered for Cases table
            // on the details pages of a particular focus type but session storage is not shared between different
            // focus types)
            @Override
            protected String getStorageKey() {
                String objectListKey = SessionStorage.KEY_OBJECT_LIST;
                String casesKey = SessionStorage.KEY_FOCUS_CASES_TABLE;
                String focusTypeName = getObjectDetailsModels().getObjectType().getClass().getSimpleName();
                return objectListKey + "_" + focusTypeName + "_" + casesKey;
            }

        };
        casesPanel.setOutputMarkupId(true);
        add(casesPanel);
    }
}

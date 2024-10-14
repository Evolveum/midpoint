/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.mining;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisDetectedPatternTileTable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleSuggestions", matchUrlForSecurity = "/admin/roleSuggestions")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_SUGGESTIONS_ALL_URL,
                        label = "PageAdminRoleSuggestions.auth.allRoleSuggestions.label",
                        description = "PageAdminRoleSuggestions.auth.allRoleSuggestions.description"),
        })
@CollectionInstance(identifier = "allRoleSuggestions", applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.role.suggestions.list", singularLabel = "Container.role.suggestions", icon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON))
public class PageRoleSuggestions extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageRoleSuggestions.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECTS = DOT_CLASS + "loadObjects";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageRoleSuggestions() {
        this(null);
    }

    public PageRoleSuggestions(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        @NotNull RoleAnalysisDetectedPatternTileTable components = loadTable();
        components.setOutputMarkupId(true);
        mainForm.add(components);
    }

    private @NotNull LoadableDetachableModel<List<DetectedPattern>> loadRoleSuggestions() {
        PageBase pageBase = (PageBase) getPage();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_OBJECTS);
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECTS);

        List<DetectedPattern> detectedPatterns = roleAnalysisService.getAllRoleSuggestions(task, result);

        return new LoadableDetachableModel<>() {
            @Override
            protected List<DetectedPattern> load() {
                return detectedPatterns;
            }
        };
    }

    private @NotNull RoleAnalysisDetectedPatternTileTable loadTable() {

        RoleAnalysisDetectedPatternTileTable components = new RoleAnalysisDetectedPatternTileTable(ID_TABLE, (PageBase) getPage(),
                loadRoleSuggestions()) {

            @Override
            protected void onRefresh(@NotNull AjaxRequestTarget target) {
                target.add(this);
            }

            @Override
            protected boolean displaySessionNameColumn() {
                return true;
            }

            @Override
            protected boolean displayClusterNameColumn() {
                return true;
            }

            @Contract(" -> new")
            @Override
            protected @NotNull Model<ViewToggle> defaultViewToggleModel() {
                return Model.of(ViewToggle.TABLE);
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

}

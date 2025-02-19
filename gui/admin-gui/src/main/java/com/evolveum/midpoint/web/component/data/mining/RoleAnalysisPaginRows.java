/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.mining;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental.RoleAnalysisTableSettingPanel;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;

public class RoleAnalysisPaginRows extends Fragment {

    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_FORM = "form";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGING = "paging";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_PAGE_SIZE = "pageSize";

    private IModel<DisplayValueOption> displayValueOptionModel;

    public RoleAnalysisPaginRows(
            String id,
            String markupId,
            RoleAnalysisTable markupProvider,
            IModel<DisplayValueOption> displayValueOptionModel,
            DataTable<?, ?> table) {
        super(id, markupId, markupProvider);
        this.displayValueOptionModel = displayValueOptionModel;
        setOutputMarkupId(true);
        initLayout(table);
    }

    private void initLayout(final DataTable<?, ?> dataTable) {
        WebMarkupContainer buttonToolbar = createButtonToolbar(ID_BUTTON_TOOLBAR);
        add(buttonToolbar);

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(new VisibleBehaviour(this::isPagingVisible));

        final Label count = new Label(ID_COUNT, () -> CountToolbar.createCountString(dataTable));
        count.setOutputMarkupId(true);
        footerContainer.add(count);

        NavigatorPanel nb2 = new NavigatorPanel(ID_PAGING, dataTable, true) {

            @Override
            protected void onPageChanged(AjaxRequestTarget target, long page) {
                target.add(count);
                target.appendJavaScript(applyTableScaleScript());

            }

            @Override
            protected boolean isCountingDisabled() {
                if (dataTable.getDataProvider() instanceof SelectableBeanContainerDataProvider) {
                    return !((SelectableBeanContainerDataProvider<?>) dataTable.getDataProvider()).isUseObjectCounting();
                }
                return super.isCountingDisabled();
            }

            @Override
            protected String getPaginationCssClass() {
                return "pagination-sm";
            }
        };
        footerContainer.add(nb2);

        Form<?> form = new MidpointForm<>(ID_FORM);
        footerContainer.add(form);
        PagingSizePanel menu = new PagingSizePanel(ID_PAGE_SIZE) {

            @Override
            protected List<Integer> getCustomPagingSizes() {
                return List.of(new Integer[] { 50, 100, 150, 200 });
            }

            @Override
            protected void onPageSizeChangePerformed(Integer newValue, AjaxRequestTarget target) {
                Table table = findParent(Table.class);
                UserProfileStorage.TableId tableId = table.getTableId();

                if (tableId != null && table.enableSavePageSize()) {
                    table.setItemsPerPage(newValue);
                }

                refreshTableRows(target);
            }
        };
        // todo nasty hack, we should decide whether paging should be normal or "small"
        menu.setSmall(true);
        form.add(menu);
        add(footerContainer);
    }

    protected WebMarkupContainer createButtonToolbar(String id) {
        RepeatingView repeatingView = new RepeatingView(id);
        repeatingView.setOutputMarkupId(true);

        //Shouldn't it be reset?
        CompositedIconBuilder refreshIconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_REFRESH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton refreshIcon = buildRefreshTableButton(repeatingView, refreshIconBuilder);
        refreshIcon.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
        repeatingView.add(refreshIcon);

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                "fa fa-cog", LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton tableSetting = buildTableSettingButton(repeatingView, iconBuilder,
                displayValueOptionModel);
        tableSetting.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
        repeatingView.add(tableSetting);

        return repeatingView;

//        return new WebMarkupContainer(id);
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildRefreshTableButton(
            @NotNull RepeatingView repeatingView,
            @NotNull CompositedIconBuilder refreshIconBuilder ) {
        AjaxCompositedIconSubmitButton refreshIcon = new AjaxCompositedIconSubmitButton(
                repeatingView.newChildId(), refreshIconBuilder.build(), createStringResource("Refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                resetTable(target);
            }

        };

        refreshIcon.titleAsLabel(true);
        return refreshIcon;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildTableSettingButton(@NotNull RepeatingView repeatingView,
            @NotNull CompositedIconBuilder iconBuilder,
            @NotNull IModel<DisplayValueOption> displayValueOptionModel) {
        AjaxCompositedIconSubmitButton tableSetting = new AjaxCompositedIconSubmitButton(
                repeatingView.newChildId(), iconBuilder.build(), createStringResource("Table settings")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                var pageBase = (PageBase) getPage();
                var parentPopup = this.findParent(Popupable.class);
                var isUsedInPopup = parentPopup != null;
                RoleAnalysisTableSettingPanel selector = new RoleAnalysisTableSettingPanel(
                        pageBase.getMainPopupBodyId(),
                        createStringResource("RoleAnalysisPathTableSelector.title"),
                        displayValueOptionModel) {

                    @Override
                    public void performAfterFinish(AjaxRequestTarget target) {
                        if (isUsedInPopup) {
                            pageBase.replaceMainPopup(parentPopup, target);
                        } else {
                            pageBase.hideMainPopup(target);
                        }
                        resetTable(target);
                    } //TODO probably too heavy to reset mining chunk,
                      // however we would need a mechanism how to differentiate when the chunk mode was changed
                    // vs. when sorting or something else was changed which might not need the hard reset
                };
                if (isUsedInPopup) {
                    pageBase.replaceMainPopup(selector, target);
                } else {
                    pageBase.showMainPopup(selector, target);
                }
            }

        };

        tableSetting.titleAsLabel(true);
        return tableSetting;
    }

    protected boolean isPagingVisible() {
        return true;
    }

    private StringResourceModel createStringResource(String key, Object ... objects) {
        return new StringResourceModel(key).setModel(new Model<String>()).setDefaultValue(key)
                .setParameters(objects);
    }

    protected void refreshTableRows(AjaxRequestTarget target) {

    }

    protected void resetTable(AjaxRequestTarget target) {

    }
}

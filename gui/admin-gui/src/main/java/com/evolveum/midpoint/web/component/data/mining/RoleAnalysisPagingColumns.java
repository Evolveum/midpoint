/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.mining;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class RoleAnalysisPagingColumns extends Fragment {

    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_FORM = "form";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGING = "paging";
    private static final String ID_PROCESS_SELECTIONS_BUTTON = "process_selections_id";
    private static final String ID_COL_COUNT_LABEL = "colCountOnPage";
    private static final String ID_COL_PER_PAGE = "label_dropdown";

    private int pageSize = 100;
    private int fromCol = 1;
    private int toCol = 100;

    public RoleAnalysisPagingColumns(String id, String markupId, DataTable<?, String> table, RoleAnalysisTable markupProvider) {
        super(id, markupId, markupProvider);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(new VisibleBehaviour(this::isPagingVisible));

        Form<?> form = new MidpointForm<>(ID_FORM);
        footerContainer.add(form);

        Form<?> formBsProcess = new MidpointForm<>("form_bs_process");
        footerContainer.add(formBsProcess);

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE,
                LayeredIconCssStyle.IN_ROW_STYLE);

        AjaxCompositedIconSubmitButton editButton = new AjaxCompositedIconSubmitButton(ID_PROCESS_SELECTIONS_BUTTON,
                iconBuilder.build(), this::getEditCreateButtonTitle) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSubmitEditButton(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        editButton.add(new AttributeModifier("style", "min-width: 150px;"));
        editButton.add(new VisibleBehaviour(() -> getMigrationButtonVisibility() && !isMultipleSelectedPatterns()));
        editButton.titleAsLabel(true);
        editButton.setOutputMarkupId(true);
        editButton.add(AttributeModifier.append("class", "btn btn-default btn-sm"));

        formBsProcess.add(editButton);

        List<Integer> integers = List.of(new Integer[] { 100, 200, 400 });
        DropDownChoice<Integer> colPerPage = new DropDownChoice<>(ID_COL_COUNT_LABEL,
                new Model<>(pageSize), integers);
        colPerPage.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onChangeSize(colPerPage.getModelObject(), target);
            }
        });
        colPerPage.setOutputMarkupId(true);
        form.add(colPerPage);

        Label colPerPageLabel = new Label(ID_COL_PER_PAGE,
                createStringResource("RoleAnalysisPagingColumns.cols.per.page.title"));
        colPerPageLabel.setOutputMarkupId(true);
        footerContainer.add(colPerPageLabel);

        Label count = new Label(ID_COUNT, createTitleModel());
        count.setOutputMarkupId(true);
        footerContainer.add(count);

        NavigatorPanel colNavigator = new NavigatorPanel(ID_PAGING, null, true) {

            @Override
            protected boolean isComponent() {
                return false;
            }

            @Override
            protected void onPageChanged(AjaxRequestTarget target, long page) {
                target.add(this);
                onChange(target, (int) page);
            }

            @Override
            protected long getCurrentPage() {
                return fromCol / pageSize;
            }

            @Override
            protected long getPageCount() {
                long totalColumns = getColumnCount();
                long totalPages = totalColumns / pageSize;
                if (pageSize * totalPages < totalColumns) {
                    ++totalPages;
                }
                return totalPages;
            }
        };
        footerContainer.add(colNavigator);

        add(footerContainer);
    }

    private IModel<String> createTitleModel() {
        return () -> fromCol + " to " + Math.min(getColumnCount(), toCol) + " of " + getColumnCount();
    }

    protected boolean isPagingVisible() {
        return true;
    }

    public void onChange(AjaxRequestTarget target, int currentPage) {
        fromCol = currentPage * pageSize + 1;
        toCol = fromCol + pageSize;

        refreshTable(fromCol, toCol, target);
    }

    public void onChangeSize(int value, AjaxRequestTarget target) {
        pageSize = value;
        fromCol = 1;
        toCol = Math.min(value, getColumnCount());

        refreshTable(fromCol, toCol, target);
    }

    protected void onSubmitEditButton(AjaxRequestTarget target) {

    }

    protected @Nullable List<DetectedPattern> getSelectedPatterns() {
        return null;
    }

    private StringResourceModel createStringResource(String key, Object... objects) {
        return WebComponentUtil.getPageBase(RoleAnalysisPagingColumns.this).createStringResource(key,
                objects);
    }

    protected int getColumnCount() {
        return 0;
    }

    protected boolean getMigrationButtonVisibility() {
        return false;
    }

    private boolean isMultipleSelectedPatterns() {
        List<DetectedPattern> selectedPatterns = getSelectedPatterns();
        return selectedPatterns != null && selectedPatterns.size() > 1;
    }

    protected void refreshTable(long fromCol, long toCol, AjaxRequestTarget target) {
    }

    private @Nullable String getEditCreateButtonTitle() {
        List<DetectedPattern> selectedCandidateRoles = getSelectedCandidateRoles();

        if (selectedCandidateRoles.isEmpty()) {
            return createStringResource("RoleMining.button.title.candidate").getString();
        }

        if (selectedCandidateRoles.size() == 1) {
            String targetName = selectedCandidateRoles.get(0).getIdentifier();
            return createStringResource("RoleMining.button.title.edit.candidate", targetName).getString();
        }

        return null;
    }

    @NotNull
    private List<DetectedPattern> getSelectedCandidateRoles() {
        List<DetectedPattern> selectedPatterns = getSelectedPatterns();
        List<DetectedPattern> selectedCandidateRoles = new ArrayList<>();
        if (selectedPatterns != null) {
            for (DetectedPattern selectedPattern : selectedPatterns) {
                if (selectedPattern.getPatternType() == BasePattern.PatternType.CANDIDATE) {
                    selectedCandidateRoles.add(selectedPattern);
                }
            }
        }
        return selectedCandidateRoles;
    }

}

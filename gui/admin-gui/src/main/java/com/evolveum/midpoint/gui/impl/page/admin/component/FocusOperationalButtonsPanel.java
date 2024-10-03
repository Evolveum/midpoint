/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusOperationalButtonsPanel<F extends FocusType> extends AssignmentHolderOperationalButtonsPanel<F> {

    private static final String ID_EXECUTE_OPTIONS_LEGEND = "executeOptionsLegend";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";

    private final LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;
    private final boolean isSelfprofile;

    public FocusOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<F>> model, LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel, boolean isSelfProfile) {
        super(id, model);
        this.isSelfprofile = isSelfProfile;
        this.executeOptionsModel = executeOptionsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel) {

            @Override
            protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
                target.add(FocusOperationalButtonsPanel.this);
            }
        };
        optionsPanel.setOutputMarkupId(true);
        optionsPanel.add(new VisibleBehaviour(() ->  getOptionsPanelVisibility()));
        add(optionsPanel);

        Label legend = new Label(ID_EXECUTE_OPTIONS_LEGEND,
                getPageBase().createStringResource("FocusOperationalButtonsPanel.options.tracing." + optionsPanel.isTracingEnabled()));
        add(legend);
    }

    @Override
    protected void addButtons(RepeatingView repeatingView) {
        createPreviewButton(repeatingView);
        super.addButtons(repeatingView);
    }

    private void createPreviewButton(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_PREVIEW, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton preview = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(),
                getPageBase().createStringResource("pageAdminFocus.button.previewChanges")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                previewPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        preview.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_PREVIEW_CHANGES_URL)
                        && !getModelObject().isReadOnly()
                        && isObjectStatusAndAuthorizationVerifiedForModification(); //todo cannot use here save button visibility because of forcedPreview configuration
                                                                                    //which acts in opposite ways for save and preview buttons
            }
        });
        preview.titleAsLabel(true);
        preview.add(AttributeAppender.append("class", "btn btn-info btn-sm"));
        repeatingView.add(preview);
    }

    @Override
    protected boolean isDeleteButtonVisible() {
        return super.isDeleteButtonVisible() && !isSelfprofile;
    }

    protected boolean getOptionsPanelVisibility() {
        if (isSelfprofile) {
            return false;
        }
        if (getModelObject().isReadOnly()) {
            return false;
        }
        return ItemStatus.NOT_CHANGED != getModelObject().getStatus()
                || getModelObject().canModify();
    }

    public ExecuteChangeOptionsDto getExecuteChangeOptions() {
        ExecuteChangeOptionsPanel optionsPanel = (ExecuteChangeOptionsPanel) get(ID_EXECUTE_OPTIONS);
        return optionsPanel != null ? optionsPanel.getModelObject() : new ExecuteChangeOptionsDto();
    }

    //TODO make abstract
    protected void previewPerformed(AjaxRequestTarget target) {

    }

    @Override
    protected boolean isChangeArchetypeButtonVisible() {
        if (isSelfprofile) {
            return false;
        }
        return super.isChangeArchetypeButtonVisible();
    }

    @Override
    protected boolean isEditRawButtonVisible() {
        if (isSelfprofile) {
            return false;
        }
        return super.isEditRawButtonVisible();
    }
}

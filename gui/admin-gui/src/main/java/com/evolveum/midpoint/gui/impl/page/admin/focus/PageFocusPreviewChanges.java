/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.preview.PreviewChangesTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.preview.PreviewChangesTabPanel.PreviewData;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/previewFocusChanges", matchUrlForSecurity = "/admin/previewFocusChanges"),
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_PREVIEW_CHANGES_URL, label = "PageAdmin.auth.previewChanges.label", description = "PageAdmin.auth.previewChanges.description")
        })
public class PageFocusPreviewChanges<O extends ObjectType> extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABBED_PANEL = "tabbedPanel";
    private static final String ID_CONTINUE_EDITING = "continueEditing";
    private static final String ID_SAVE = "save";
    private static final String ID_SIMPLE_PANEL = "simplePanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageFocusPreviewChanges.class);

    private List<PreviewData> previewData;
    /*
     * Live model contexts may contain non-serializable model execution state
     * (e.g. LensContext -> MagicAssignment/Holder). Keep them only as transient
     * input until serializable preview data is created.
     */
    private transient Map<PrismObject<O>, ModelContext<O>> modelContextMap;

    private PageBase previousPage;

    public PageFocusPreviewChanges() {
        throw new RestartResponseException(getApplication().getHomePage());
    }

    public PageFocusPreviewChanges(Map<PrismObject<O>, ModelContext<O>> modelContextMap, PageBase previousPage) {
        this.modelContextMap = modelContextMap;
        this.previousPage = previousPage;
    }

    /**
     * Extracts the data needed by the preview page from live model contexts.
     *
     * The returned data is safe to keep in Wicket page state; the original model contexts are not.
     */
    private List<PreviewData> createPreviewData(Map<PrismObject<O>, ModelContext<O>> modelContextMap) {
        List<PreviewData> previewData = new ArrayList<>();
        modelContextMap.forEach((object, modelContext) ->
                previewData.add(
                        PreviewChangesTabPanel.createPreviewData(
                                getTabPanelTitleModel(object).getObject(), modelContext, this)));
        return previewData;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        // Wicket page state must contain only the serializable preview representation.
        if (previewData == null) {
            previewData = createPreviewData(modelContextMap);
            modelContextMap = null;
        }
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        mainForm.setMultiPart(true);
        add(mainForm);

        List<ITab> tabs = createTabs();
        TabbedPanel<ITab> tabbedPanel = WebComponentUtil.createTabPanel(ID_TABBED_PANEL, this, tabs, null);
        tabbedPanel.add(new VisibleBehaviour(() -> tabs.size() > 1));
        tabbedPanel.setOutputMarkupId(true);
        mainForm.add(tabbedPanel);

        Component simplePanel;
        if (tabs.size() == 1) {
            simplePanel = tabs.get(0).getPanel(ID_SIMPLE_PANEL);
        } else {
            simplePanel = new Label(ID_SIMPLE_PANEL);
        }
        simplePanel.add(new VisibleBehaviour(() -> tabs.size() == 1));
        mainForm.add(simplePanel);

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxButton cancel = new AjaxButton(ID_CONTINUE_EDITING, createStringResource("PagePreviewChanges.button.continueEditing")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);

        AjaxButton save = new AjaxButton(ID_SAVE, createStringResource("PagePreviewChanges.button.save")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                savePerformed(target);
            }
        };
        //save.add(new EnableBehaviour(() -> violationsEmpty()));           // does not work as expected (MID-4252)

        save.add(new VisibleBehaviour(() -> violationsEmpty() && isWithProductionConfiguration()));            // so hiding the button altogether
        mainForm.add(save);
    }

    private boolean isWithProductionConfiguration() {
        for (PreviewData previewChange : previewData) {
            if (previewChange.withProductionConfiguration()) {
                return true;
            }
        }
        return false;
    }

    private boolean violationsEmpty() {
        for (PreviewData previewChange : previewData) {
            if (!previewChange.violationsEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
        previewData.forEach(previewChange -> {

            tabs.add(new PanelTab(Model.of(previewChange.title())) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new PreviewChangesTabPanel(panelId, previewChange);
                }
            });
        });
        return tabs;
    }

    private IModel<String> getTabPanelTitleModel(PrismObject<? extends ObjectType> object) {
        return Model.of(WebComponentUtil.getEffectiveName(object, AbstractRoleType.F_DISPLAY_NAME));
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        if (previousPage != null) {
            setResponsePage(previousPage);
            return;
        }
        redirectBack();
    }

    private void savePerformed(AjaxRequestTarget target) {
        if (previousPage != null) {
            setResponsePage(previousPage);

            if (previousPage instanceof PageFocusDetails) {
                ((PageFocusDetails) previousPage).setSaveOnConfigure(true);
            } else {
                error("Couldn't save changes - unexpected referring page: " + previousPage);
            }
        } else {
            Breadcrumb bc = redirectBack();
            error("Couldn't save changes - no instance for referring page; breadcrumb is " + bc);
        }
    }
}

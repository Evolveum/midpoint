/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.CheckboxMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsPanel extends BasePanel<ExecuteChangeOptionsDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangeOptionsPanel.class);

    private static final String ID_TRACING = "tracing";
    private static final String ID_TRACING_CONTAINER = "tracingContainer";
    private static final String ID_SAVE_IN_BACKGROUND_CONTAINER = "ExecuteChangeOptionsPanel.label.saveInBackgroundLabel";

    private static final String FORCE_LABEL = "ExecuteChangeOptionsPanel.label.force";
    private static final String RECONCILE_LABEL = "ExecuteChangeOptionsPanel.label.reconcile";
    private static final String EXECUTE_AFTER_ALL_APPROVALS_LABEL = "ExecuteChangeOptionsPanel.label.executeAfterAllApprovals";
    private static final String KEEP_DISPLAYING_RESULTS_LABEL = "ExecuteChangeOptionsPanel.label.keepDisplayingResults";

    private static final String ID_OPTIONS = "options";
    private static final String ID_RESET_CHOICES = "resetChoices";

    public ExecuteChangeOptionsPanel(String id, LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel) {
        super(id, executeOptionsModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private CheckboxMenuItem createCheckboxMenuItem(String label, String propertyExpression) {
        return new CheckboxMenuItem(createStringResource(label), new PropertyModel<>(getModel(), propertyExpression));
    }

    private void initLayout() {
        setOutputMarkupId(true);

        createOptionsDropdownButton(createDropdownMenuItems());
        createTracingOptionsPanel();
    }

    private List<InlineMenuItem> createDropdownMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createCheckboxMenuItem(FORCE_LABEL, ExecuteChangeOptionsDto.F_FORCE));
        items.add(createCheckboxMenuItem(RECONCILE_LABEL, ExecuteChangeOptionsDto.F_RECONCILE));
        items.add(createCheckboxMenuItem(EXECUTE_AFTER_ALL_APPROVALS_LABEL, ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS));
        items.add(createCheckboxMenuItem(KEEP_DISPLAYING_RESULTS_LABEL, ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS));
        items.add(createCheckboxMenuItem(ID_SAVE_IN_BACKGROUND_CONTAINER, ExecuteChangeOptionsDto.F_SAVE_IN_BACKGROUND));
        return items;
    }

    private void createOptionsDropdownButton(List<InlineMenuItem> items) {
        DropdownButtonDto model = new DropdownButtonDto(null, GuiStyleConstants.CLASS_OPTIONS_BUTTON_ICON,
                getPageBase().createStringResource("ExecuteChangeOptionsPanel.options").getString(), items);
        DropdownButtonPanel dropdownButtonPanel = new DropdownButtonPanel(ID_OPTIONS, model) {

            @Override
            protected void populateMenuItem(String componentId, ListItem<InlineMenuItem> menuItem) {
                InlineMenuItem item = menuItem.getModelObject();
                if (!(item instanceof CheckboxMenuItem)) {
                    super.populateMenuItem(componentId, menuItem);
                    return;
                }

                CheckboxMenuItem checkboxMenuItem = (CheckboxMenuItem) item;
                CheckBoxPanel panel = new CheckBoxPanel(componentId, checkboxMenuItem.getCheckBoxModel(), checkboxMenuItem.getLabel(), null) {

                    @Override
                    public void onUpdate(AjaxRequestTarget target) {
                        target.add(this.getPanelComponent());
                    }
                };
                panel.add(new EnableBehaviour(() -> isOptionEnabled(model, checkboxMenuItem)));
                panel.setRenderBodyOnly(true);
                menuItem.add(panel);
            }

            @Override
            protected String getSpecialButtonClass() {
                return "btn-sm btn-default btn-margin-right";
            }

            @Override
            protected String getSpecialDropdownMenuClass() {
                return "execute-options";
            }
        };
        add(dropdownButtonPanel);
        dropdownButtonPanel.setRenderBodyOnly(true);
    }

    private DropdownButtonPanel getOptionsButtonPanel() {
        return (DropdownButtonPanel) get(ID_OPTIONS);
     }

    private boolean isOptionEnabled(DropdownButtonDto dropdownButtonDto, CheckboxMenuItem checkboxMenuItem) {
        if (!(checkboxMenuItem.getLabel().getObject().equals(createStringResource(KEEP_DISPLAYING_RESULTS_LABEL).getString())) &&
                !(checkboxMenuItem.getLabel().getObject().equals(createStringResource(ID_SAVE_IN_BACKGROUND_CONTAINER).getString()))) {
            return true;
        }
        List<InlineMenuItem> items = dropdownButtonDto.getMenuItems();
        if (checkboxMenuItem.getLabel().getObject().equals(createStringResource(KEEP_DISPLAYING_RESULTS_LABEL).getString())) {
            for (InlineMenuItem item : items) {
                if (item.getLabel().getObject().equals(createStringResource(ID_SAVE_IN_BACKGROUND_CONTAINER).getString())) {
                    return !Boolean.TRUE.equals(((CheckboxMenuItem)item).getCheckBoxModel().getObject());
                }
            }
        }
        if (checkboxMenuItem.getLabel().getObject().equals(createStringResource(ID_SAVE_IN_BACKGROUND_CONTAINER).getString())) {
            for (InlineMenuItem item : items) {
                if (item.getLabel().getObject().equals(createStringResource(KEEP_DISPLAYING_RESULTS_LABEL).getString())) {
                    return !Boolean.TRUE.equals(((CheckboxMenuItem)item).getCheckBoxModel().getObject());
                }
            }
        }
        return true;
    }

   private void createTracingOptionsPanel() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Tracing")) {
            @Override
            public InlineMenuItemAction initAction() {
                return null;
            }
        });
       DropdownButtonDto model = new DropdownButtonDto(null, GuiStyleConstants.CLASS_TRACING_BUTTON_ICON, "Tracing", items);
       DropdownButtonPanel dropdownButtonPanel = new DropdownButtonPanel(ID_TRACING, model) {

           @Override
           protected void populateMenuItem(String componentId, ListItem<InlineMenuItem> menuItem) {
               menuItem.add(createTracingRadioChoicesFragment(componentId));
           }

           @Override
           protected String getSpecialButtonClass() {
               return "btn-sm btn-default btn-margin-right";
           }

           @Override
           protected String getSpecialDropdownMenuClass() {
               return "execute-options radio";
           }
       };
       add(dropdownButtonPanel);
       dropdownButtonPanel.setOutputMarkupId(true);
       dropdownButtonPanel.add(new VisibleBehaviour(this::isTracingEnabled));
   }

   private Fragment createTracingRadioChoicesFragment(String componentId) {
        Fragment fragment = new Fragment(componentId, ID_TRACING_CONTAINER, ExecuteChangeOptionsPanel.this);

       RadioChoice<TracingProfileType> tracingProfile = new RadioChoice<>(ID_TRACING, PropertyModel.of(ExecuteChangeOptionsPanel.this.getModel(), ExecuteChangeOptionsDto.F_TRACING),
               PropertyModel.of(ExecuteChangeOptionsPanel.this.getModel(), ExecuteChangeOptionsDto.F_TRACING_CHOICES), createTracinnChoiceRenderer());
        fragment.add(tracingProfile);

       AjaxLink<Void> resetChoices = new AjaxLink<>(ID_RESET_CHOICES) {

           @Override
           public void onClick(AjaxRequestTarget target) {
                ExecuteChangeOptionsPanel.this.getModelObject().setTracing(null);
                target.add(ExecuteChangeOptionsPanel.this);
           }
       };
       fragment.add(resetChoices);
       return fragment;
   }

   public boolean isTracingEnabled() {
       boolean canRecordTrace;
       try {
           canRecordTrace = getPageBase().isAuthorized(ModelAuthorizationAction.RECORD_TRACE.getUrl());
       } catch (Throwable t) {
           LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check trace recording authorization", t);
           canRecordTrace = false;
       }

       return canRecordTrace && WebModelServiceUtils.isEnableExperimentalFeature(getPageBase());
   }

    protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
    }

    private IChoiceRenderer<TracingProfileType> createTracinnChoiceRenderer() {
        return new IChoiceRenderer<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(TracingProfileType profile) {
                if (profile == null) {
                    return "(none)";
                } else if (profile.getDisplayName() != null) {
                    return profile.getDisplayName();
                } else if (profile.getName() != null) {
                    return profile.getName();
                } else {
                    return "(unnamed profile)";
                }
            }

            @Override
            public String getIdValue(TracingProfileType object, int index) {
                return String.valueOf(index);
            }

            @Override
            public TracingProfileType getObject(String id, IModel<? extends List<? extends TracingProfileType>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        };
    }
}

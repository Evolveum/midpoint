/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ResolveItemPanel extends ActionConfigurationPanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RESPONSES_PANEL = "responsesPanel";
    private static final String ID_RESPONSE_PANEL = "responsePanel";

    AccessCertificationResponseType selectedResponse = null;

    public ResolveItemPanel(String id, IModel<ContainerPanelConfigurationType> configurationModel) {
        super(id, configurationModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        List<AccessCertificationResponseType> responses = getResponses();
        ListView<AccessCertificationResponseType> responsesPanel = new ListView<>(ID_RESPONSES_PANEL, responses) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AccessCertificationResponseType> item) {
                CertificationItemResponseHelper responseHelper = new CertificationItemResponseHelper(item.getModelObject());
                SelectableInfoBoxPanel<AccessCertificationResponseType> widget =
                        new SelectableInfoBoxPanel<>(ID_RESPONSE_PANEL, item.getModel()) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void itemSelectedPerformed(AccessCertificationResponseType response, AjaxRequestTarget target) {
                                selectedResponse = response;
                                target.add(ResolveItemPanel.this);
                            }

                            @Override
                            protected IModel<String> getIconClassModel() {
                                return () -> {
                                    String iconCssClass = GuiDisplayTypeUtil.getIconCssClass(responseHelper.getResponseDisplayType());
                                    String iconBgColor = responseHelper.getBackgroundCssClass();
                                    return iconCssClass + " " + iconBgColor;
                                };
                            }

                            @Override
                            protected IModel<String> getLabelModel() {
                                return () -> GuiDisplayTypeUtil.getTranslatedLabel(responseHelper.getResponseDisplayType());
                            }

                            @Override
                            protected IModel<String> getDescriptionModel() {
                                return () -> GuiDisplayTypeUtil.getHelp(responseHelper.getResponseDisplayType());
                            }

                            protected IModel<String> getAdditionalLinkStyle() {
                                return getItemPanelAdditionalStyle(item.getModelObject());
                            }
                        };
                widget.add(new VisibleBehaviour(() -> isResponseVisible(item.getModelObject())));
                item.add(widget);
            }
        };
        responsesPanel.setOutputMarkupId(true);
        responsesPanel.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(responses)));
        add(responsesPanel);
    }

    @Override
    protected boolean isValidated(AjaxRequestTarget target) {
        if (selectedResponse == null) {
            warn(getString("ResolveItemPanel.noResponseSelected"));
            target.add(ResolveItemPanel.this);
            return false;
        }
        return true;
    }

    @Override
    protected Collection<ItemDelta<?, ?>> computedDeltas() {
        PrismContainerValueWrapper<Containerable> iw = model.getObject();
        String response = OutcomeUtils.toUri(selectedResponse);
        try {
            iw.findProperty(ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME))
                    .getValue().setRealValue(response);
        } catch (SchemaException e) {
            error("Couldn't set outcome: " + e.getMessage());
        }
        return super.computedDeltas();
    }

//    @Override
//    protected void confirmPerformedWithDeltas(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
//        if (selectedResponse == null) {
//                    warn(getString("PageCertDecisions.message.noItemSelected"));
//                    target.add(ResolveItemPanel.this);
//                    return;
//                }
//                savePerformed(target, selectedResponse, getComment());
//                getPageBase().hideMainPopup(target);
//    }

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("ResolveItemPanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return () -> "fa fa-edit";
    }

    private IModel<String> getItemPanelAdditionalStyle(AccessCertificationResponseType response) {
        return isSelected(response) ? Model.of("active") : Model.of("");
    }

    private boolean isSelected(AccessCertificationResponseType response) {
        return response != null && response.equals(selectedResponse);
    }

    protected List<AccessCertificationResponseType> getResponses() {
        return Arrays.stream(AccessCertificationResponseType.values())
                .filter(response -> response != AccessCertificationResponseType.DELEGATE)
                .toList();
    }

//    private String getComment() {
//        TextArea<String> comment = (TextArea<String>) get(ID_COMMENT);
//        return comment.getModelObject();
//    }

    private boolean isResponseVisible(AccessCertificationResponseType response) {
        return getResponses()
                .stream()
                .anyMatch(r -> r.equals(response));
    }

}

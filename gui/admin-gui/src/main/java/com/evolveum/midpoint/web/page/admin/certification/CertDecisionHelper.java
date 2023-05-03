/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;

import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrWorkItemDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertWorkItemDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.SearchingUtils;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandler;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandlerRegistry;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Some common functionality used from PageCertCampaign and PageCertDecisions.
 * TODO finish the refactoring
 *
 * @author mederly
 */
public class CertDecisionHelper implements Serializable {

    public enum WhichObject {
        OBJECT, TARGET
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createTypeColumn(final WhichObject which, final PageBase page) {
        IColumn column;
        column = new IconColumn<CertCaseOrWorkItemDto>(page.createStringResource("")) {
            @Override
            protected DisplayType getIconDisplayType(IModel<CertCaseOrWorkItemDto> rowModel) {
                ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(which, rowModel);
                String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                return GuiDisplayTypeUtil.createDisplayType(icon);
            }

            private ObjectTypeGuiDescriptor getObjectTypeDescriptor(WhichObject which, IModel<CertCaseOrWorkItemDto> rowModel) {
                QName targetType = rowModel.getObject().getObjectType(which);
                return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(targetType));
            }

            @Override
            public void populateItem(Item<ICellPopulator<CertCaseOrWorkItemDto>> item, String componentId, IModel<CertCaseOrWorkItemDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(which, rowModel);
                if (guiDescriptor != null) {
                    item.add(AttributeModifier.replace("title", page.createStringResource(guiDescriptor.getLocalizationKey())));
                    item.add(new TooltipBehavior());
                }
            }
        };
        return column;
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createObjectNameColumn(final PageBase page, final String headerKey) {
        IColumn column;
        column = new AjaxLinkColumn<CertCaseOrWorkItemDto>(page.createStringResource(headerKey),
                SearchingUtils.OBJECT_NAME, CertCaseOrWorkItemDto.F_OBJECT_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertCaseOrWorkItemDto> rowModel) {
                CertCaseOrWorkItemDto dto = rowModel.getObject();
                dispatchToObjectDetailsPage(dto.getCertCase().getObjectRef(), page, false);
            }
        };
        return column;
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createTargetNameColumn(final PageBase page, final String headerKey) {
        IColumn column;
        column = new AjaxLinkColumn<CertCaseOrWorkItemDto>(page.createStringResource(headerKey),
                SearchingUtils.TARGET_NAME, CertCaseOrWorkItemDto.F_TARGET_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertCaseOrWorkItemDto> rowModel) {
                CertCaseOrWorkItemDto dto = rowModel.getObject();
                dispatchToObjectDetailsPage(dto.getCertCase().getTargetRef(), page, false);
            }
        };
        return column;
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createReviewerNameColumn(final PageBase page, final String headerKey) {
        IColumn column;
        column = new AbstractColumn<T, String>(page.createStringResource(headerKey)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                    final IModel<T> rowModel) {
                CertCaseOrWorkItemDto dto = rowModel.getObject();
                RepeatingView reviewersPanel = new RepeatingView(componentId);
                if (dto instanceof CertWorkItemDto) {
                    List<ObjectReferenceType> reviewersList = ((CertWorkItemDto) dto).getReviewerRefList();
                    if (CollectionUtils.isNotEmpty(reviewersList)) {
                        for (ObjectReferenceType reviewer : reviewersList) {
                            reviewersPanel.add(new AjaxLinkPanel(reviewersPanel.newChildId(),
                                    Model.of(WebComponentUtil.getDisplayNameOrName(reviewer))) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    dispatchToObjectDetailsPage(reviewer, page, false);
                                }
                            });
                        }
                    }
                }
                cellItem.add(reviewersPanel);
            }

        };
        return column;
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createConflictingNameColumn(final PageBase page, final String headerKey) {
        return new PropertyColumn<>(page.createStringResource(headerKey), CertCaseOrWorkItemDto.F_CONFLICTING_TARGETS);
    }

    public <T extends CertCaseOrWorkItemDto> IColumn<T, String> createDetailedInfoColumn(final PageBase page) {
        IColumn column;
        column = new IconColumn<CertCaseOrWorkItemDto>(page.createStringResource("")) {

            @Override
            protected DisplayType getIconDisplayType(final IModel<CertCaseOrWorkItemDto> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType("fa fa-fw fa-info-circle text-info");
            }

            @Override
            public void populateItem(Item<ICellPopulator<CertCaseOrWorkItemDto>> item, String componentId, IModel<CertCaseOrWorkItemDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                CertCaseOrWorkItemDto aCase = rowModel.getObject();
                String handlerUri;
                if (aCase instanceof CertWorkItemDto) {
                    handlerUri = aCase.getHandlerUri();
                } else {
                    handlerUri = ((PageCertCampaign) page).getCampaignHandlerUri();
                }
                CertGuiHandler handler = page.getCertGuiHandlerRegistry().getHandler(handlerUri);
                if (handler != null) {
                    String title = handler.getCaseInfoButtonTitle(rowModel, page);
                    item.add(AttributeModifier.replace("title", title));
                    item.add(new TooltipBehavior());
                }
            }
        };
        return column;
    }
}

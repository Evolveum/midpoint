/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrDecisionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDecisionDto;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandler;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandlerRegistry;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;

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

    IColumn createTypeColumn(final WhichObject which, final PageBase page) {
        IColumn column;
        column = new IconColumn<CertCaseOrDecisionDto>(page.createStringResource("")) {
            @Override
            protected IModel<String> createIconModel(IModel<CertCaseOrDecisionDto> rowModel) {
                ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(which, rowModel);
                String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                return new Model<>(icon);
            }

            private ObjectTypeGuiDescriptor getObjectTypeDescriptor(WhichObject which, IModel<CertCaseOrDecisionDto> rowModel) {
                QName targetType = rowModel.getObject().getObjectType(which);
                return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(targetType));
            }

            @Override
            public void populateItem(Item<ICellPopulator<CertCaseOrDecisionDto>> item, String componentId, IModel<CertCaseOrDecisionDto> rowModel) {
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

    IColumn createObjectNameColumn(final PageBase page, final String headerKey) {
        IColumn column;
        column = new LinkColumn<CertCaseOrDecisionDto>(page.createStringResource(headerKey),
                AccessCertificationCaseType.F_OBJECT_REF.getLocalPart(), CertCaseOrDecisionDto.F_OBJECT_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertCaseOrDecisionDto> rowModel) {
                CertCaseOrDecisionDto dto = rowModel.getObject();
                dispatchToObjectDetailsPage(dto.getCertCase().getObjectRef(), page, false);
            }
        };
        return column;
    }

    IColumn createTargetNameColumn(final PageBase page, final String headerKey) {
        IColumn column;
        column = new LinkColumn<CertCaseOrDecisionDto>(page.createStringResource(headerKey),
				AccessCertificationCaseType.F_TARGET_REF.getLocalPart(), CertCaseOrDecisionDto.F_TARGET_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertCaseOrDecisionDto> rowModel) {
                CertCaseOrDecisionDto dto = rowModel.getObject();
                dispatchToObjectDetailsPage(dto.getCertCase().getTargetRef(), page, false);
            }
        };
        return column;
    }

    IColumn createConflictingNameColumn(final PageBase page, final String headerKey) {
        return new PropertyColumn<CertCaseOrDecisionDto, String>(page.createStringResource(headerKey),
                CertCaseOrDecisionDto.F_CONFLICTING_TARGETS);
    }

    public IColumn createDetailedInfoColumn(final PageBase page) {
        IColumn column;
        column = new IconColumn<CertCaseOrDecisionDto>(page.createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(final IModel<CertCaseOrDecisionDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return "fa fa-fw fa-info-circle text-info";
                    }
                };
            }

            @Override
            public void populateItem(Item<ICellPopulator<CertCaseOrDecisionDto>> item, String componentId, IModel<CertCaseOrDecisionDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                CertCaseOrDecisionDto aCase = rowModel.getObject();
                String handlerUri;
                if (aCase instanceof CertDecisionDto) {
                    handlerUri = aCase.getHandlerUri();
                } else {
                    handlerUri = ((PageCertCampaign) page).getCampaignHandlerUri();
                }
                CertGuiHandler handler = CertGuiHandlerRegistry.instance().getHandler(handlerUri);
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

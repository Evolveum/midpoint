/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.model.RelationModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

@Component
public class RelationPanelFactory extends AbstractGuiComponentFactory<QName> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        TextPanel relationPanel = new TextPanel<String>(panelCtx.getComponentId(), new RelationModel(panelCtx.getRealValueModel()));
        relationPanel.getBaseFormComponent().add(new RelationValidator());
        return relationPanel;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        ItemPath relationRefPath = ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_RELATIONS, RelationsDefinitionType.F_RELATION, RelationDefinitionType.F_REF);
        return relationRefPath.equivalent(wrapper.getPath().removeIds());
    }

    @Override
    public Integer getOrder() {
        return 9999;
    }


    class RelationValidator implements IValidator<String> {


        @Override
        public void validate(IValidatable<String> validatable) {
            String value = validatable.getValue();
            if (StringUtils.isBlank(value)) {
                return;
            }

            if (QNameUtil.isUri(value)) {
                return;
            }

            ValidationError error = new ValidationError();
            error.addKey("RelationPanel.relation.identifier.must.be.qname");
            error.setMessage("Relation identifier must be in the form of URI.");
            validatable.error(error);
        }
    }
}

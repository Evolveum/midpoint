/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class DisplayNamePanel<C extends Containerable> extends BasePanel<C>{

	private static final long serialVersionUID = 1L;

    private final static String ID_TYPE_IMAGE = "typeImage";
    private final static String ID_DISPLAY_NAME = "displayName";
    private final static String ID_IDENTIFIER = "identifier";
    private final static String ID_RELATION = "relation";
    private final static String ID_KIND_INTENT = "kindIntent";
    private final static String ID_DESCRIPTION = "description";

	public DisplayNamePanel(String id, IModel<C> model) {
		super(id, model);

		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.setOutputMarkupId(true);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
        add(typeImage);

        Label name = new Label(ID_DISPLAY_NAME, createHeaderModel());
        name.setOutputMarkupId(true);
        add(name);
        
        Label identifier = new Label(ID_IDENTIFIER, createIdentifierModel());
        identifier.setOutputMarkupId(true);
        identifier.add(new VisibleBehaviour(() -> isIdentifierVisible()));
        add(identifier);
        
        Label relation = new Label(ID_RELATION, Model.of(getRelationLabel()));
        relation.setOutputMarkupId(true);
        relation.add(new VisibleBehaviour(() -> isRelationVisible()));
        add(relation);

        IModel<String> kindIntentLabelModel = getKindIntentLabelModel();
        Label kindIntent = new Label(ID_KIND_INTENT, kindIntentLabelModel);
		kindIntent.setOutputMarkupId(true);
		kindIntent.add(new VisibleBehaviour(() -> isKindIntentVisible(kindIntentLabelModel)));
        add(kindIntent);

        add(new Label(ID_DESCRIPTION, new PropertyModel<String>(getModel(), ObjectType.F_DESCRIPTION.getLocalPart())));
	}
	
	private String createImageModel() {
		if (getModelObject() == null){
			return "";
		}
		if (ConstructionType.class.isAssignableFrom(getModelObject().getClass())) {
			return WebComponentUtil.createDefaultColoredIcon(ResourceType.COMPLEX_TYPE);
		}

		return WebComponentUtil.createDefaultColoredIcon(getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName());

	}

	private IModel<String> createHeaderModel() {
		// TODO: align with DisplayNameModel
		if (getModelObject() == null){
			return Model.of("");
		}
		if (ObjectType.class.isAssignableFrom(getModelObject().getClass())) {
			return Model.of(WebComponentUtil.getEffectiveName((ObjectType) getModelObject(), AbstractRoleType.F_DISPLAY_NAME));
		}
		PrismProperty<String> name = getModelObject().asPrismContainerValue().findProperty(ObjectType.F_NAME);
		if (name == null || name.isEmpty()) {
			return Model.of("");
		}
		return Model.of(name.getRealValue());
	}
	
	private IModel<String> createIdentifierModel() {
		if (getModelObject() == null){
			return Model.of("");
		}
		if (AbstractRoleType.class.isAssignableFrom(getModelObject().getClass())) {
			return Model.of(WebComponentUtil.getEffectiveName((ObjectType) getModelObject(), AbstractRoleType.F_IDENTIFIER));
		}
		return Model.of("");
	}

	private boolean isIdentifierVisible() {
		if (getModelObject() == null){
			return false;
		}
		if (AbstractRoleType.class.isAssignableFrom(getModelObject().getClass())) {
			return getModelObject().asPrismContainerValue().findProperty(new ItemPath(AbstractRoleType.F_IDENTIFIER)) != null;
		}
		return false;
	}
	
	// TODO: maybe move relation methods to subclass if we want this panel to be really reusable
	
	private boolean isRelationVisible() {
		QName relation = getRelation();
		return relation != null && !QNameUtil.match(SchemaConstants.ORG_DEFAULT, relation);
	}

	private boolean isKindIntentVisible(IModel<String> kindIntentLabelModel) {
		return kindIntentLabelModel != null && StringUtils.isNotEmpty(kindIntentLabelModel.getObject());
	}

	private String getRelationLabel() {
		QName relation = getRelation();
		if (relation == null) {
			return "";
		}
		// TODO: localization?
		return relation.getLocalPart();
	}

	protected IModel<String> getKindIntentLabelModel() {
		// To be overriden in subclasses
		return Model.of("");
	}

	protected QName getRelation() {
		// To be overriden in subclasses
		return null;
	}
}

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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

/**
 *  @author shood
 *
 *  Item paths edited by this component are limited to single segment.
 *  TODO - this component should probably be renamed to ItemPathType editor
 * */
public class QNameEditorPanel extends BasePanel<ItemPathType>{

    private static final String ID_LOCAL_PART = "localPart";
    private static final String ID_NAMESPACE = "namespace";
    private static final String ID_LOCAL_PART_LABEL = "localPartLabel";
    private static final String ID_LOCAL_PART_REQUIRED = "localPartRequired";
    private static final String ID_NAMESPACE_LABEL = "namespaceLabel";
    private static final String ID_NAMESPACE_REQUIRED = "namespaceRequired";
    private static final String ID_T_LOCAL_PART = "localPartTooltip";
    private static final String ID_T_NAMESPACE = "namespaceTooltip";

	private IModel<ItemPathType> itemPathModel;
	private IModel<String> localpartModel;
    private IModel<String> namespaceModel;

    public QNameEditorPanel(String id, IModel<ItemPathType> model, String localPartTooltipKey,
                            String namespaceTooltipKey, boolean markLocalPartAsRequired, boolean markNamespaceAsRequired) {
        super(id, model);
		this.itemPathModel = model;

		localpartModel = new IModel<String>() {
			@Override
			public String getObject() {
				QName qName = itemPathToQName();
				return qName != null ? qName.getLocalPart() : null;
			}

			@Override
			public void setObject(String object) {
				if (object == null) {
					itemPathModel.setObject(null);
				} else {
					itemPathModel.setObject(new ItemPathType(new ItemPath(new QName(namespaceModel.getObject(), object))));
				}
			}

			@Override
			public void detach() {
			}
		};
		namespaceModel = new IModel<String>() {
			@Override
			public String getObject() {
				QName qName = itemPathToQName();
				return qName != null ? qName.getNamespaceURI() : null;
			}

			@Override
			public void setObject(String object) {
				if (StringUtils.isBlank(localpartModel.getObject())) {
					itemPathModel.setObject(null);
				} else {
					itemPathModel.setObject(new ItemPathType(new ItemPath(new QName(object, localpartModel.getObject()))));
				}
			}

			@Override
			public void detach() {
			}
		};

        initLayout(localPartTooltipKey, namespaceTooltipKey, markLocalPartAsRequired, markNamespaceAsRequired);
    }

	private QName itemPathToQName() {
		if (itemPathModel.getObject() == null) {
			return null;
		}
		ItemPath path = itemPathModel.getObject().getItemPath();
		if (path.size() == 0) {
			return null;
		} else if (path.size() == 1 && path.first() instanceof NameItemPathSegment) {
			return ((NameItemPathSegment) path.first()).getName();
		} else {
			throw new IllegalStateException("Malformed ItemPath: " + path);
		}
	}



	@Override
    public IModel<ItemPathType> getModel() {
        IModel<ItemPathType> model = super.getModel();
        ItemPathType modelObject = model.getObject();

		// TODO consider removing this
        if (modelObject == null){
            model.setObject(new ItemPathType());
        }

        return model;
    }

    private void initLayout(String localPartTooltipKey,
			String namespaceTooltipKey, boolean markLocalPartAsRequired, boolean markNamespaceAsRequired){

        Label localPartLabel = new Label(ID_LOCAL_PART_LABEL, getString("SchemaHandlingStep.association.label.associationName"));
        localPartLabel.setOutputMarkupId(true);
        localPartLabel.setOutputMarkupPlaceholderTag(true);
        localPartLabel.add(getSpecificLabelStyleAppender());
        add(localPartLabel);

		WebMarkupContainer localPartRequired = new WebMarkupContainer(ID_LOCAL_PART_REQUIRED);
		localPartRequired.add(new VisibleEnableBehaviour(){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return markLocalPartAsRequired;
			}
		});
		add(localPartRequired);

        Label namespaceLabel = new Label(ID_NAMESPACE_LABEL, getString("SchemaHandlingStep.association.label.associationNamespace"));
        namespaceLabel.setOutputMarkupId(true);
        namespaceLabel.setOutputMarkupPlaceholderTag(true);
        namespaceLabel.add(getSpecificLabelStyleAppender());
        add(namespaceLabel);

		WebMarkupContainer namespaceRequired = new WebMarkupContainer(ID_NAMESPACE_REQUIRED);
		namespaceRequired.add(new VisibleEnableBehaviour(){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return markNamespaceAsRequired;
			}
		});
		add(namespaceRequired);

		TextField localPart = new TextField<>(ID_LOCAL_PART, localpartModel);
        localPart.setOutputMarkupId(true);
        localPart.setOutputMarkupPlaceholderTag(true);
        localPart.setRequired(isLocalPartRequired());
		localPart.add(new UpdateBehavior());
        add(localPart);

        DropDownChoice namespace = new DropDownChoice<>(ID_NAMESPACE, namespaceModel, prepareNamespaceList());
        namespace.setOutputMarkupId(true);
        namespace.setOutputMarkupPlaceholderTag(true);
        namespace.setNullValid(false);
        namespace.setRequired(isNamespaceRequired());
		namespace.add(new UpdateBehavior());
        add(namespace);

        Label localPartTooltip = new Label(ID_T_LOCAL_PART);
        localPartTooltip.add(new AttributeAppender("data-original-title", getString(localPartTooltipKey)));
        localPartTooltip.add(new InfoTooltipBehavior());
        localPartTooltip.setOutputMarkupPlaceholderTag(true);
        localPartTooltip.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return localPartTooltipKey != null;
			}
		});
        add(localPartTooltip);

        Label namespaceTooltip = new Label(ID_T_NAMESPACE);
        namespaceTooltip.add(new AttributeAppender("data-original-title", getString(namespaceTooltipKey)));
		namespaceTooltip.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return namespaceTooltipKey != null;
			}
		});
        namespaceTooltip.add(new InfoTooltipBehavior());
        namespaceTooltip.setOutputMarkupPlaceholderTag(true);
        add(namespaceTooltip);
    }

    /**
     *  Override to provide custom list of namespaces
     *  for QName editor
     * */
    protected List<String> prepareNamespaceList(){
        return Arrays.asList(SchemaConstants.NS_ICF_SCHEMA, MidPointConstants.NS_RI);
    }

    public boolean isLocalPartRequired(){
        return false;
    }

	public boolean isNamespaceRequired() {
		return false;
	}

	private class UpdateBehavior extends EmptyOnChangeAjaxFormUpdatingBehavior {
		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			QNameEditorPanel.this.onUpdate(target);
		}
	}

	protected void onUpdate(AjaxRequestTarget target) {
	}

	protected AttributeAppender getSpecificLabelStyleAppender(){
    	return AttributeAppender.append("style", "");
	}
}

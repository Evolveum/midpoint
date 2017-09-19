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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author mederly
 */
public class ScenePanel extends BasePanel<SceneDto> {

	private static final String ID_BOX = "box";
	private static final String STRIPED_CLASS = "striped";
    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_PARTIAL_SCENES = "partialScenes";
    private static final String ID_PARTIAL_SCENE = "partialScene";

    private static final Trace LOGGER = TraceManager.getTrace(ScenePanel.class);
	public static final String ID_OPTION_BUTTONS = "optionButtons";
	public static final String ID_HEADER_PANEL = "headerPanel";
	public static final String ID_HEADER_DESCRIPTION = "description";
	public static final String ID_HEADER_WRAPPER_DISPLAY_NAME = "wrapperDisplayName";
	public static final String ID_HEADER_NAME_LABEL = "nameLabel";
	public static final String ID_HEADER_NAME_LINK = "nameLink";
	public static final String ID_HEADER_CHANGE_TYPE = "changeType";
	public static final String ID_HEADER_OBJECT_TYPE = "objectType";
	public static final String ID_BODY = "body";
	public static final String ID_OLD_VALUE_LABEL = "oldValueLabel";
	public static final String ID_NEW_VALUE_LABEL = "newValueLabel";
	public static final String ID_VALUE_LABEL = "valueLabel";

    public ScenePanel(String id, @NotNull IModel<SceneDto> model) {
        super(id, model);
        setOutputMarkupId(true);
        initLayout();
    }

    private AjaxEventBehavior createHeaderOnClickBehaviour(final IModel<SceneDto> model) {
        return new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        };
    }

    private void initLayout() {
		final IModel<SceneDto> model = getModel();

		WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
		box.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				SceneDto dto = model.getObject();

				if (dto.getBoxClassOverride() != null) {
					return dto.getBoxClassOverride();
				}

				if (dto.getChangeType() == null) {
					return null;
				}

				switch (dto.getChangeType()) {
					case ADD:
						return "box-success";
					case DELETE:
						return "box-danger";
					case MODIFY:
						return "box-info";
					default:
						return null;
				}
			}
		}));
		add(box);

        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
		box.add(headerPanel);

		headerPanel.add(new SceneButtonPanel(ID_OPTION_BUTTONS, model) {
			@Override
			public void minimizeOnClick(AjaxRequestTarget target) {
				headerOnClickPerformed(target, model);
			}
		});

		Label headerChangeType = new Label(ID_HEADER_CHANGE_TYPE, new ChangeTypeModel());
		//headerChangeType.setRenderBodyOnly(true);
		Label headerObjectType = new Label(ID_HEADER_OBJECT_TYPE, new ObjectTypeModel());
		//headerObjectType.setRenderBodyOnly(true);

		IModel<String> nameModel = new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return model.getObject().getName(ScenePanel.this);
			}
		};
		Label headerNameLabel = new Label(ID_HEADER_NAME_LABEL, nameModel);
		LinkPanel headerNameLink = new LinkPanel(ID_HEADER_NAME_LINK, nameModel) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				PrismContainerValue<?> value = getModelObject().getScene().getSourceValue();
				if (value != null && value.getParent() instanceof PrismObject) {
					PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) value.getParent();
					WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(object), getPageBase(), false);
				}
			}
		};
        Label headerDescription = new Label(ID_HEADER_DESCRIPTION, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return model.getObject().getDescription(ScenePanel.this);
			}
		});
        Label headerWrapperDisplayName = new Label(ID_HEADER_WRAPPER_DISPLAY_NAME,
				new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						String key = ((WrapperScene) getModelObject().getScene()).getDisplayNameKey();
						Object[] parameters = ((WrapperScene) getModelObject().getScene()).getDisplayNameParameters();
						return new StringResourceModel(key, this).setModel(null)
								.setDefaultValue(key)
								.setParameters(parameters).getObject();
					}
				});

		headerPanel.add(headerChangeType);
		headerPanel.add(headerObjectType);
		headerPanel.add(headerNameLabel);
		headerPanel.add(headerNameLink);
		headerPanel.add(headerDescription);
		headerPanel.add(headerWrapperDisplayName);

		headerChangeType.add(createHeaderOnClickBehaviour(model));
		headerObjectType.add(createHeaderOnClickBehaviour(model));
		headerNameLabel.add(createHeaderOnClickBehaviour(model));
		headerDescription.add(createHeaderOnClickBehaviour(model));
		headerWrapperDisplayName.add(createHeaderOnClickBehaviour(model));

		VisibleEnableBehaviour visibleIfNotWrapper = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !getModelObject().isWrapper();
			}
		};
		VisibleEnableBehaviour visibleIfWrapper = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().isWrapper();
			}
		};
		VisibleEnableBehaviour visibleIfExistingObject = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				if (getModelObject().isWrapper()) {
					return false;
				}
				return isExistingViewableObject();
			}
		};
		VisibleEnableBehaviour visibleIfNotWrapperAndNotExistingObject = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				if (getModelObject().isWrapper()) {
					return false;
				}
				return !isExistingViewableObject();
			}
		};
		headerChangeType.add(visibleIfNotWrapper);
		headerObjectType.add(visibleIfNotWrapper);
		headerNameLabel.add(visibleIfNotWrapperAndNotExistingObject);
		headerNameLink.add(visibleIfExistingObject);
		headerDescription.add(visibleIfNotWrapper);
		headerWrapperDisplayName.add(visibleIfWrapper);

		WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                SceneDto wrapper = model.getObject();
                return !wrapper.isMinimized();
            }
        });
        box.add(body);

		WebMarkupContainer itemsTable = new WebMarkupContainer(ID_ITEMS_TABLE);
		itemsTable.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !model.getObject().getItems().isEmpty();
			}
		});
		WebMarkupContainer oldValueLabel = new WebMarkupContainer(ID_OLD_VALUE_LABEL);
		oldValueLabel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().containsDeltaItems();
			}
		});
		itemsTable.add(oldValueLabel);
		WebMarkupContainer newValueLabel = new WebMarkupContainer(ID_NEW_VALUE_LABEL);
		newValueLabel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().containsDeltaItems();
			}
		});
		itemsTable.add(newValueLabel);
		WebMarkupContainer valueLabel = new WebMarkupContainer(ID_VALUE_LABEL);
		valueLabel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !model.getObject().containsDeltaItems();
			}
		});
		itemsTable.add(valueLabel);
		ListView<SceneItemDto> items = new ListView<SceneItemDto>(ID_ITEMS,
				new PropertyModel<List<SceneItemDto>>(model, SceneDto.F_ITEMS)) {

			@Override
			protected void populateItem(ListItem<SceneItemDto> item) {
				SceneItemPanel panel = new SceneItemPanel(ID_ITEM, item.getModel());
				panel.setRenderBodyOnly(true);
				item.add(panel);
			}
		};
        items.setReuseItems(true);
        itemsTable.add(items);
		body.add(itemsTable);

        ListView<SceneDto> partialScenes = new ListView<SceneDto>(ID_PARTIAL_SCENES,
				new PropertyModel<List<SceneDto>>(model, SceneDto.F_PARTIAL_SCENES)) {

            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                ScenePanel panel = new ScenePanel(ID_PARTIAL_SCENE, item.getModel());
				panel.setOutputMarkupPlaceholderTag(true);
				item.add(panel);
            }
        };
        partialScenes.setReuseItems(true);
        body.add(partialScenes);
    }

	protected boolean isExistingViewableObject() {
		final Scene scene = getModelObject().getScene();
		final PrismContainerValue<?> value = scene.getSourceValue();
		return value != null &&
				value.getParent() instanceof PrismObject &&
				WebComponentUtil.hasDetailsPage((PrismObject) value.getParent()) &&
				((PrismObject) value.getParent()).getOid() != null &&
				(scene.getSourceDelta() == null || !scene.getSourceDelta().isAdd());
	}

	public void headerOnClickPerformed(AjaxRequestTarget target, IModel<SceneDto> model) {
        SceneDto dto = model.getObject();
        dto.setMinimized(!dto.isMinimized());
        target.add(this);
    }

	private class ChangeTypeModel extends AbstractReadOnlyModel<String> {

		@Override
		public String getObject() {
			ChangeType changeType = getModel().getObject().getScene().getChangeType();
			if (changeType == null) {
				return "";
			}
			return WebComponentUtil.createLocalizedModelForEnum(changeType, ScenePanel.this).getObject();
		}
	}

	private class ObjectTypeModel extends AbstractReadOnlyModel<String> {

		@Override
		public String getObject() {
			Scene scene = getModel().getObject().getScene();
			PrismContainerDefinition<?> def = scene.getSourceDefinition();
			if (def == null) {
				return "";
			}
			if (def instanceof PrismObjectDefinition) {
				return PageBase.createStringResourceStatic(ScenePanel.this, SchemaConstants.OBJECT_TYPE_KEY_PREFIX +def.getTypeName().getLocalPart()).getObject();
			} else {
				return "";
			}
		}
	}
}

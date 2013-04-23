/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.ThreeStateCheckPanel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends SimplePanel<AssignmentEditorDto> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorPanel.class);

    private static final String DOT_CLASS = AssignmentEditorPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";

    private static final String ID_MAIN = "main";
    private static final String ID_HEADER = "headerPanel";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_BODY = "body";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_RELATION = "relation";
    private static final String ID_ENABLED = "enabled";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_SHOW_EMPTY = "showEmpty";
    private static final String ID_SHOW_EMPTY_LABEL = "showEmptyLabel";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_TARGET = "target";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";


    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);

        initPanelLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(AssignmentEditorPanel.class, "AssignmentEditorPanel.css")));
    }

    private void initPanelLayout() {
        final WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER);
        headerPanel.add(new AttributeAppender("class", createHeaderClassModel(getModel()), " "));
        headerPanel.setOutputMarkupId(true);
        add(headerPanel);

        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED)) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //do we want to update something?
            }
        };
        headerPanel.add(selected);

        Image typeImage = new Image(ID_TYPE_IMAGE,
                createImageTypeModel(new PropertyModel<AssignmentEditorDtoType>(getModel(), AssignmentEditorDto.F_TYPE)));
        headerPanel.add(typeImage);

        AjaxLink name = new AjaxLink(ID_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerPanel.add(name);

        Label nameLabel = new Label(ID_NAME_LABEL, new PropertyModel<String>(getModel(), AssignmentEditorDto.F_NAME));
        name.add(nameLabel);

        Label activation = new Label(ID_ACTIVATION, createActivationModel());
        headerPanel.add(activation);

        WebMarkupContainer main = new WebMarkupContainer(ID_MAIN);
        main.setOutputMarkupId(true);
        add(main);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                return !editorDto.isMinimized();
            }
        });
        main.add(body);

        initBodyLayout(body);
    }

    private IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = model.getObject();
                return dto.getStatus().name().toLowerCase();
            }
        };
    }

    private IModel<String> createActivationModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = getModel().getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return "-";
                }

                Boolean enabled = activation.isEnabled();
                String strEnabled;
                if (enabled != null) {
                    strEnabled = enabled ? getString("AssignmentEditorPanel.active")
                            : getString("AssignmentEditorPanel.inactive");
                } else {
                    strEnabled = getString("AssignmentEditorPanel.undefined");
                }

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()), MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return "-";
            }
        };
    }

    private IModel<Date> createDateModel(final IModel<XMLGregorianCalendar> model) {
        return new Model<Date>() {

            @Override
            public Date getObject() {
                XMLGregorianCalendar calendar = model.getObject();
                if (calendar == null) {
                    return null;
                }
                return MiscUtil.asDate(calendar);
            }

            @Override
            public void setObject(Date object) {
                if (object == null) {
                    model.setObject(null);
                } else {
                    model.setObject(MiscUtil.asXMLGregorianCalendar(object));
                }
            }
        };
    }

    private void initBodyLayout(WebMarkupContainer body) {
        TextArea description = new TextArea(ID_DESCRIPTION,
                new PropertyModel(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        body.add(description);

        TextField relation = new TextField(ID_RELATION, new PropertyModel(getModel(), AssignmentEditorDto.F_RELATION));
        relation.setEnabled(false);
        body.add(relation);

        ThreeStateCheckPanel enabled = new ThreeStateCheckPanel(ID_ENABLED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_ACTIVATION + ".enabled"));
        enabled.setStyle("margin: 1px 0 0 10px;");
        body.add(enabled);

        DateTextField validFrom = DateTextField.forDatePattern(ID_VALID_FROM,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")), "dd/MMM/yyyy");
        validFrom.add(new DatePicker());
        body.add(validFrom);

        DateTextField validTo = DateTextField.forDatePattern(ID_VALID_TO,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")), "dd/MMM/yyyy");
        validTo.add(new DatePicker());
        body.add(validTo);

        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        targetContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return !AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(targetContainer);

        Label target = new Label(ID_TARGET, createTargetModel());
        targetContainer.add(target);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        constructionContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(constructionContainer);

        AjaxLink showEmpty = new AjaxLink(ID_SHOW_EMPTY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyPerformed(target);
            }
        };
        constructionContainer.add(showEmpty);

        Label showEmptyLabel = new Label(ID_SHOW_EMPTY_LABEL, createShowEmptyLabel());
        showEmptyLabel.setOutputMarkupId(true);
        showEmpty.add(showEmptyLabel);

        initAttributesLayout(constructionContainer);
    }

    private void initAttributesLayout(WebMarkupContainer constructionContainer) {
        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        attributes.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        constructionContainer.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE,
                new LoadableModel<List<ACAttributeDto>>(false) {

                    @Override
                    protected List<ACAttributeDto> load() {
                        return loadAttributes();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                final IModel<ACAttributeDto> attrModel = listItem.getModel();
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, attrModel);
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
                listItem.setOutputMarkupId(true);

                listItem.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                        if (editorDto.isShowEmpty()) {
                            return true;
                        }

                        ACAttributeDto dto = attrModel.getObject();
                        return !dto.isEmpty();
                    }
                });
            }
        };
        attributes.add(attribute);

        //todo extension
    }

    private IModel<String> createShowEmptyLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();

                if (dto.isShowEmpty()) {
                    return getString("AssignmentEditorPanel.hideEmpty");
                } else {
                    return getString("AssignmentEditorPanel.showEmpty");
                }
            }
        };
    }

    private void showEmptyPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();
        dto.setShowEmpty(!dto.isShowEmpty());

        WebMarkupContainer parent = (WebMarkupContainer) get(createComponentPath(ID_MAIN, ID_BODY,
                ID_CONSTRUCTION_CONTAINER));

        target.add(parent.get(ID_ATTRIBUTES), parent.get(createComponentPath(ID_SHOW_EMPTY, ID_SHOW_EMPTY_LABEL)),
                getPageBase().getFeedbackPanel());
    }

    private List<ACAttributeDto> loadAttributes() {
        AssignmentEditorDto dto = getModel().getObject();

        OperationResult result = new OperationResult(OPERATION_LOAD_ATTRIBUTES);
        List<ACAttributeDto> attributes = new ArrayList<ACAttributeDto>();
        try {
            ConstructionType construction = WebMiscUtil.getValue(dto.getOldValue(),
                    AssignmentType.F_ACCOUNT_CONSTRUCTION, ConstructionType.class);
            PrismObject<ResourceType> resource = construction.getResource() != null
                    ? construction.getResource().asPrismObject() : null;
            if (resource == null) {
                resource = getReference(construction.getResourceRef(), result);
            }

            PrismContext prismContext = getPageBase().getPrismContext();
            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                    LayerType.PRESENTATION, prismContext);
            PrismContainerDefinition definition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, construction.getIntent())
            		.toResourceAttributeContainerDefinition();

            List<ResourceAttributeDefinitionType> attrConstructions = construction.getAttribute();

            Collection<ItemDefinition> definitions = definition.getDefinitions();
            for (ItemDefinition attrDef : definitions) {
                if (!(attrDef instanceof PrismPropertyDefinition)) {
                    //log skipping or something...
                    continue;
                }

                PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;
                if (propertyDef.isOperational() || propertyDef.isIgnored()) {
                    continue;
                }
                attributes.add(ACAttributeDto.createACAttributeDto(propertyDef,
                        findOrCreateValueConstruction(propertyDef, attrConstructions), prismContext));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Exception occurred during assignment attribute loading", ex);
            result.recordFatalError("Exception occurred during assignment attribute loading.", ex);
        } finally {
            result.recomputeStatus();
        }

        Collections.sort(attributes, new Comparator<ACAttributeDto>() {

            @Override
            public int compare(ACAttributeDto a1, ACAttributeDto a2) {
                return String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName());
            }
        });

        dto.setAttributes(attributes);

        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResultInSession(result);
        }

        return attributes;
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
            target = getPageBase().getModelService().getObject(ObjectType.class, ref.getOid(), null, task,
                    subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get account construction resource ref", ex);
            subResult.recordFatalError("Couldn't get account construction resource ref.", ex);
        }

        return target;
    }

    private ResourceAttributeDefinitionType findOrCreateValueConstruction(PrismPropertyDefinition attrDef,
                                                                          List<ResourceAttributeDefinitionType> attrConstructions) {
        for (ResourceAttributeDefinitionType construction : attrConstructions) {
            if (attrDef.getName().equals(construction.getRef())) {
                return construction;
            }
        }

        ResourceAttributeDefinitionType construction = new ResourceAttributeDefinitionType();
        construction.setRef(attrDef.getName());

        return construction;
    }

    private IModel<ResourceReference> createImageTypeModel(final IModel<AssignmentEditorDtoType> model) {
        return new AbstractReadOnlyModel<ResourceReference>() {

            @Override
            public ResourceReference getObject() {
                AssignmentEditorDtoType type = model.getObject();
                switch (type) {
                    case ROLE:
                        return new SharedResourceReference(ImgResources.class, ImgResources.USER_SUIT);
                    case ORG_UNIT:
                        return new SharedResourceReference(ImgResources.class, ImgResources.BUILDING);
                    case ACCOUNT_CONSTRUCTION:
                    default:
                        return new SharedResourceReference(ImgResources.class, ImgResources.DRIVE);
                }
            }
        };
    }

    private void nameClickPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = getModel().getObject();
        boolean minimized = dto.isMinimized();
        if (minimized) {
//            dto.startEditing();//todo ???
        }

        dto.setMinimized(!minimized);

        target.add(get(ID_MAIN));
        target.add(get(ID_HEADER));
    }

    private IModel<String> createTargetModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                AssignmentEditorDto dto = getModel().getObject();
                PrismContainerValue assignment = dto.getOldValue();

                PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
                if (targetRef == null) {
                    return getString("AssignmentEditorPanel.undefined");
                }

                PrismReferenceValue refValue = targetRef.getValue();
                if (refValue != null && refValue.getObject() != null) {
                    PrismObject object = refValue.getObject();
                    return WebMiscUtil.getName(object);
                }

                String oid = targetRef.getOid();
                OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT);
                try {
                    PageBase page = getPageBase();
                    ModelService model = page.getMidpointApplication().getModel();
                    Task task = page.createSimpleTask(OPERATION_LOAD_OBJECT);

                    Collection<SelectorOptions<GetOperationOptions>> options =
                            SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
                    PrismObject object = model.getObject(ObjectType.class, oid, options, task, result);

                    return WebMiscUtil.getName(object);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
                }

                return oid;
            }
        };
    }
}

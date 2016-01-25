/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.BootstrapLabel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class CheckTableHeader extends SimplePanel<ObjectWrapper> {

    private static final String ID_CHECK = "check";
    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_MENU = "menu";
    private static final String ID_LINK = "link";
    private static final String ID_STATUS = "status";
    private static final String ID_SHOW_MORE = "showMore";
    private static final String ID_TRIGGER = "trigger";
    private static final String ID_PROTECTED = "protected";

    public CheckTableHeader(String id, IModel<ObjectWrapper> model) {
        super(id, model);

        add(AttributeModifier.append("class", "check-table-header"));
    }

    @Override
    protected void initLayout() {
        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK,
                new PropertyModel<Boolean>(getModel(), ObjectWrapper.F_SELECTED)) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(check);

        Label icon = new Label(ID_ICON);
        icon.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createAccountIcon();
            }
        }));
        add(icon);

        Label trigger = new Label(ID_TRIGGER);
        trigger.add(AttributeModifier.replace("title", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createTriggerTooltip();
            }
        }));
        trigger.add(new TooltipBehavior());
        trigger.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasTriggers();
            }
        });
        add(trigger);

        Label protectedIcon = new Label(ID_PROTECTED);
        protectedIcon.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ObjectWrapper wrapper = getModelObject();
                return wrapper.isProtectedAccount();
            }
        });
        add(protectedIcon);

        BootstrapLabel status = new BootstrapLabel(ID_STATUS, createStringResource("CheckTableHeader.label.error"),
                new Model(BootstrapLabel.State.DANGER));
        status.add(createFetchErrorVisibleBehaviour());
        add(status);
        AjaxLink showMore = new AjaxLink(ID_SHOW_MORE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowMorePerformed(target);
            }
        };
        showMore.add(createFetchErrorVisibleBehaviour());
        add(showMore);

        AjaxLink link = new AjaxLink(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target);
            }
        };
        add(link);

        Label name = new Label(ID_NAME, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getDisplayName();
            }
        });
        link.add(name);

        Label description = new Label(ID_DESCRIPTION, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getDescription();
            }
        });
        add(description);

        final IModel<List<InlineMenuItem>> items = new Model((Serializable) createMenuItems());
        InlineMenu menu = new InlineMenu(ID_MENU, items, true);
        menu.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                List<InlineMenuItem> list = items.getObject();
                return list != null && !list.isEmpty();
            }
        });
        add(menu);
    }

    private String createAccountIcon() {
        ObjectWrapper wrapper = getModelObject();
        PrismObject object = wrapper.getObject();
        PrismProperty status = object.findProperty(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        if (status != null && status.getRealValue() != null) {
            ActivationStatusType value = (ActivationStatusType) status.getRealValue();
            if (ActivationStatusType.DISABLED.equals(value)) {
                return "fa fa-male text-muted";
            }
        }

        return "fa fa-male";
    }

    private String createTriggerTooltip() {
        ObjectWrapper wrapper = getModelObject();
        PrismObject obj = wrapper.getObject();
        PrismContainer container = obj.findContainer(ObjectType.F_TRIGGER);
        if (container == null || container.isEmpty()) {
            return null;
        }

        List<String> triggers = new ArrayList<>();
        for (PrismContainerValue val : (List<PrismContainerValue>) container.getValues()) {
            XMLGregorianCalendar time = (XMLGregorianCalendar) val.getPropertyRealValue(TriggerType.F_TIMESTAMP,
                    XMLGregorianCalendar.class);

            if (time == null) {
                triggers.add(getString("CheckTableHeader.triggerUnknownTime"));
            } else {
                triggers.add(getString("CheckTableHeader.triggerPlanned", WebMiscUtil.formatDate(time)));
            }
        }

        return StringUtils.join(triggers, '\n');
    }

    private boolean hasTriggers() {
        ObjectWrapper wrapper = getModelObject();
        PrismObject obj = wrapper.getObject();
        PrismContainer container = obj.findContainer(ObjectType.F_TRIGGER);
        return container != null && !container.isEmpty();
    }

    private VisibleEnableBehaviour createFetchErrorVisibleBehaviour() {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                OperationResult fetchResult = getModelObject().getFetchResult();
                if (fetchResult != null && !WebMiscUtil.isSuccessOrHandledError(fetchResult)) {
                    return true;
                }

                OperationResult result = getModelObject().getResult();
                if (result != null && !WebMiscUtil.isSuccessOrHandledError(result)) {
                    return true;
                }

                return false;
            }
        };
    }

    private String getDisplayName() {
        ObjectWrapper wrapper = getModel().getObject();
        String key = wrapper.getDisplayName();
        return translate(key);
    }

    private String getDescription() {
        ObjectWrapper wrapper = getModel().getObject();
        String key = wrapper.getDescription();
        return translate(key);
    }

    private String translate(String key) {
        if (key == null) {
            key = "";
        }
        return PageTemplate.createStringResourceStatic(getPage(), key).getString();
//        return new StringResourceModel(key, getPage(), null, key).getString();
    }

    protected List<InlineMenuItem> createMenuItems() {
        return new ArrayList<>();
    }

    protected void onClickPerformed(AjaxRequestTarget target) {
        ObjectWrapper wrapper = getModelObject();
        wrapper.setMinimalized(!wrapper.isMinimalized());

        target.add(findParent(PrismObjectPanel.class));
    }

    protected void onShowMorePerformed(AjaxRequestTarget target){
        showResult(getModelObject().getFetchResult());
        showResult(getModelObject().getResult());

        target.add(getPageBase().getFeedbackPanel());
    }

    private void showResult(OperationResult result) {
        PageBase page = getPageBase();
        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            page.showResult(result);
        }
    }
}

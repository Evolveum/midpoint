/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.BootstrapLabel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class CheckTableHeader<O extends ObjectType> extends BasePanel<ObjectWrapper<O>> {
	private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";
    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LINK = "link";
    private static final String ID_STATUS = "status";
    private static final String ID_SHOW_MORE = "showMore";
    private static final String ID_TRIGGER = "trigger";
    private static final String ID_EXPAND = "expand";

    public CheckTableHeader(String id, IModel<ObjectWrapper<O>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
    	
        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK,
                new PropertyModel<Boolean>(getModel(), ObjectWrapper.F_SELECTED)) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(check);

        Label icon = new Label(ID_ICON);
        icon.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                return "check-table-header-icon " + createAccountIcon();
            }
        }));
        add(icon);

        Label trigger = new Label(ID_TRIGGER);
        trigger.add(AttributeModifier.replace("title", new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                return createTriggerTooltip();
            }
        }));
        trigger.add(new TooltipBehavior());
        trigger.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isVisible() {
                return hasTriggers();
            }
        });
        add(trigger);

        BootstrapLabel status = new BootstrapLabel(ID_STATUS, createStringResource("CheckTableHeader.label.error"),
                new Model<>(BootstrapLabel.State.DANGER));
        status.add(createFetchErrorVisibleBehaviour());
        add(status);
        AjaxLink showMore = new AjaxLink(ID_SHOW_MORE) {
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                return getDisplayName();
            }
        });
        link.add(name);

        Label description = new Label(ID_DESCRIPTION, new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                return getDescription();
            }
        });
        add(description);
        
        ToggleIconButton expandButton = new ToggleIconButton(ID_EXPAND,
        		GuiStyleConstants.CLASS_ICON_EXPAND, GuiStyleConstants.CLASS_ICON_COLLAPSE) {
        	private static final long serialVersionUID = 1L;
        	
        	@Override
            public void onClick(AjaxRequestTarget target) {
        		onClickPerformed(target);
            }
        	
        	@Override
			public boolean isOn() {
				return !CheckTableHeader.this.getModelObject().isMinimalized();
			}
        };
        add(expandButton);

    }

    private String createAccountIcon() {
        ObjectWrapper<O> wrapper = getModelObject();
        PrismObject<O> object = wrapper.getObject();
        PrismProperty<ActivationStatusType> status = object.findProperty(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        if (status != null && status.getRealValue() != null) {
            ActivationStatusType value = status.getRealValue();
            if (ActivationStatusType.DISABLED.equals(value)) {
                return "fa fa-male text-muted";
            }
        }

        return "fa fa-male";
    }

    private String createTriggerTooltip() {
        ObjectWrapper<O> wrapper = getModelObject();
        PrismObject<O> obj = wrapper.getObject();
        PrismContainer<TriggerType> container = obj.findContainer(ObjectType.F_TRIGGER);
        if (container == null || container.isEmpty()) {
            return null;
        }

        List<String> triggers = new ArrayList<>();
        for (PrismContainerValue<TriggerType> val : container.getValues()) {
            XMLGregorianCalendar time = val.getPropertyRealValue(TriggerType.F_TIMESTAMP, XMLGregorianCalendar.class);

            if (time == null) {
                triggers.add(getString("CheckTableHeader.triggerUnknownTime"));
            } else {
                triggers.add(getString("CheckTableHeader.triggerPlanned", WebComponentUtil.formatDate(time)));
            }
        }

        return StringUtils.join(triggers, '\n');
    }

    private boolean hasTriggers() {
        ObjectWrapper<O> wrapper = getModelObject();
        PrismObject<O> obj = wrapper.getObject();
        PrismContainer<TriggerType> container = obj.findContainer(ObjectType.F_TRIGGER);
        return container != null && !container.isEmpty();
    }

    private VisibleEnableBehaviour createFetchErrorVisibleBehaviour() {
        return new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isVisible() {
                OperationResult fetchResult = getModelObject().getFetchResult();
                if (fetchResult != null && !WebComponentUtil.isSuccessOrHandledError(fetchResult)) {
                    return true;
                }
                //TODO: do we need to check overall status????
                //[PM] you're absolutely right - see MID-3951. The result contains results of fetching all shadows. So I'm commenting the code out.
//                OperationResult result = getModelObject().getResult();
//                result.computeStatusIfUnknown();
//                if (result != null && !WebComponentUtil.isSuccessOrHandledError(result)) {
//                    return true;
//                }
                return false;
            }
        };
    }

    private String getDisplayName() {
        ObjectWrapper<O> wrapper = getModel().getObject();
        String key = wrapper.getDisplayName();
        return translate(key);
    }

    private String getDescription() {
        ObjectWrapper<O> wrapper = getModel().getObject();
        String key = wrapper.getDescription();
        return translate(key);
    }

    private String translate(String key) {
        if (key == null) {
            key = "";
        }
        return PageBase.createStringResourceStatic(getPage(), key).getString();
//        return new StringResourceModel(key, getPage(), null, key).getString();
    }

    protected void onClickPerformed(AjaxRequestTarget target) {
        ObjectWrapper<O> wrapper = getModelObject();
        wrapper.setMinimalized(!wrapper.isMinimalized());
    }

    protected void onShowMorePerformed(AjaxRequestTarget target){
        showResult(getModelObject().getFetchResult());
        showResult(getModelObject().getResult());

        target.add(getPageBase().getFeedbackPanel());
    }

    private void showResult(OperationResult result) {
        PageBase page = getPageBase();
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            page.showResult(result);
        }
    }
}

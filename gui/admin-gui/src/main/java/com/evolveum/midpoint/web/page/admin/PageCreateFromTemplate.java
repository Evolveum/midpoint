/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/template", matchUrlForSecurity = "/admin/template")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageCreateFromTemplate extends PageAdmin {

    private static final String ID_TEMPLATE = "template";

    public PageCreateFromTemplate(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageCreateFromTemplate." + getType().getLocalPart() + ".title");
    }

    private void initLayout() {
        MultiCompositedButtonPanel buttonsPanel = new MultiCompositedButtonPanel(ID_TEMPLATE, new PropertyModel<>(loadButtonDescriptions(), MultiFunctinalButtonDto.F_ADDITIONAL_BUTTONS)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                List<ObjectReferenceType> archetypeRef = ObjectCollectionViewUtil.getArchetypeReferencesList(collectionViews);
                try {
                    WebComponentUtil.initNewObjectWithReference(getPageBase(),
                            getType(),
                            archetypeRef);
                } catch (SchemaException ex) {
                    getPageBase().getFeedbackMessages().error(PageCreateFromTemplate.this, ex.getUserFriendlyMessage());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }

        };
        add(buttonsPanel);
    }

    protected LoadableModel<MultiFunctinalButtonDto> loadButtonDescriptions() {
        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();

                Collection<CompiledObjectCollectionView> compiledObjectCollectionViews = getCompiledGuiProfile().findAllApplicableArchetypeViews(getType());

                if (CollectionUtils.isNotEmpty(compiledObjectCollectionViews)) {
                    compiledObjectCollectionViews.forEach(collection -> {
                        CompositedIconButtonDto buttonDesc = new CompositedIconButtonDto();
                        buttonDesc.setCompositedIcon(createCompositedIcon(collection));
                        buttonDesc.setOrCreateDefaultAdditionalButtonDisplayType(collection.getDisplay());
                        buttonDesc.setCollectionView(collection);
                        additionalButtons.add(buttonDesc);
                    });
                }

                if (isGenericNewButtonVisible()) {
                    CompositedIconButtonDto defaultButton = new CompositedIconButtonDto();
                    DisplayType defaultButtonDisplayType = new DisplayType();
                    defaultButtonDisplayType.setLabel(new PolyStringType("bla"));
                    defaultButton.setAdditionalButtonDisplayType(defaultButtonDisplayType);

                    CompositedIconBuilder defaultButtonIconBuilder = new CompositedIconBuilder();
                    defaultButtonIconBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(defaultButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                            .appendColorHtmlValue(WebComponentUtil.getIconColor(defaultButtonDisplayType));
//                            .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"), IconCssStyle.BOTTOM_RIGHT_STYLE);

                    defaultButton.setCompositedIcon(defaultButtonIconBuilder.build());
                    additionalButtons.add(defaultButton);
                }

                MultiFunctinalButtonDto multifunctionalButton = new MultiFunctinalButtonDto();
                multifunctionalButton.setAdditionalButtons(additionalButtons);
                return multifunctionalButton;
            }
        };

    }

    //TODO copied from MainObjectListPanel
    private CompositedIcon createCompositedIcon(CompiledObjectCollectionView collectionView) {
        DisplayType additionalButtonDisplayType = WebComponentUtil.getNewObjectDisplayTypeFromCollectionView(collectionView, PageCreateFromTemplate.this);
        CompositedIconBuilder builder = new CompositedIconBuilder();

        builder.setBasicIcon(WebComponentUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(WebComponentUtil.getIconColor(additionalButtonDisplayType));
//                    .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"), IconCssStyle.BOTTOM_RIGHT_STYLE);

        return builder.build();
    }

    protected boolean isGenericNewButtonVisible() {
        if (QNameUtil.match(ReportType.COMPLEX_TYPE, getType())) {
            return false;
        }
        return true;
    }

    private QName getType() {
        StringValue restType = getPageParameters().get("type");
        if (restType == null || restType.toString() == null) {
            throw redirectBackViaRestartResponseException();
        }
        return ObjectTypes.getTypeQNameFromRestType(restType.toString());
    }
}

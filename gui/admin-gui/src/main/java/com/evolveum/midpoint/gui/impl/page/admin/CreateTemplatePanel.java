/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class CreateTemplatePanel<O extends ObjectType> extends BasePanel<PrismObject<O>> {

    private static final String ID_TEMPLATE = "template";

    public CreateTemplatePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MultiCompositedButtonPanel buttonsPanel = new MultiCompositedButtonPanel(ID_TEMPLATE, new PropertyModel<>(loadButtonDescriptions(), MultiFunctinalButtonDto.F_ADDITIONAL_BUTTONS)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                onTemplateChosePerformed(collectionViews, target);
            }

        };
        add(buttonsPanel);
    }

    protected void onTemplateChosePerformed(CompiledObjectCollectionView collectionViews, AjaxRequestTarget target) {
        List<ObjectReferenceType> archetypeRef = ObjectCollectionViewUtil.getArchetypeReferencesList(collectionViews);
        try {
            WebComponentUtil.initNewObjectWithReference(getPageBase(),
                    getType(),
                    archetypeRef);
        } catch (SchemaException ex) {
            getPageBase().getFeedbackMessages().error(getPageBase(), ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    protected LoadableModel<MultiFunctinalButtonDto> loadButtonDescriptions() {
        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();

                Collection<CompiledObjectCollectionView> compiledObjectCollectionViews = findAllApplicableArchetypeViews();

                if (CollectionUtils.isNotEmpty(compiledObjectCollectionViews)) {
                    compiledObjectCollectionViews.forEach(collection -> {
                        CompositedIconButtonDto buttonDesc = new CompositedIconButtonDto();
                        buttonDesc.setCompositedIcon(createCompositedIcon(collection));
                        buttonDesc.setOrCreateDefaultAdditionalButtonDisplayType(collection.getDisplay());
                        buttonDesc.setCollectionView(collection);
                        additionalButtons.add(buttonDesc);
                    });
                }

                MultiFunctinalButtonDto multifunctionalButton = new MultiFunctinalButtonDto();
                multifunctionalButton.setAdditionalButtons(additionalButtons);
                return multifunctionalButton;
            }
        };
    }

    protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        return null;
    }

    //TODO copied from MainObjectListPanel
    private CompositedIcon createCompositedIcon(CompiledObjectCollectionView collectionView) {
        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getNewObjectDisplayTypeFromCollectionView(collectionView, getPageBase());
        CompositedIconBuilder builder = new CompositedIconBuilder();

        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(additionalButtonDisplayType));

        return builder.build();
    }

    private DisplayType getDefaultButtonDisplayType() {
        String iconCssStyle = WebComponentUtil.createDefaultBlackIcon(getType());

        String sb = createStringResource("MainObjectListPanel.newObject").getString()
                + " "
                + createStringResource("ObjectTypeLowercase." + getType().getLocalPart()).getString();
        DisplayType display = GuiDisplayTypeUtil.createDisplayType(iconCssStyle, "", sb);
        display.setLabel(WebComponentUtil.createPolyFromOrigString(
                getType().getLocalPart(), "ObjectType." + getType().getLocalPart()));
        return display;
    }

    protected boolean isGenericNewButtonVisible() {
        if (QNameUtil.match(ReportType.COMPLEX_TYPE, getType())) {
            return false;
        }
        return true;
    }

    protected QName getType() {
        return null;
    }

}

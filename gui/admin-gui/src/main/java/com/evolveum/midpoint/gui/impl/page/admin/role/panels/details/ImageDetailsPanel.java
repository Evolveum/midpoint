/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.panels.details;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortMiningSet;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.getRoleObject;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.CustomImageResource;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    private static final String ID_IMAGE = "image";

    List<String> similarGroupOid;
    MiningType targetValue;

    public ImageDetailsPanel(String id, IModel<String> messageModel,
            List<String> similarGroupOid, MiningType targetValue) {
        super(id, messageModel);
        this.similarGroupOid = similarGroupOid;
        this.targetValue = targetValue;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        String string = DOT_CLASS + "getMiningTypeObject";
        OperationResult result = new OperationResult(string);
        List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();
        Set<String> rolesOid = new HashSet<>();
        for (String groupOid : similarGroupOid) {
            PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);

            miningTypeList.add(miningObject);
            rolesOid.addAll(miningObject.asObjectable().getRoles());
        }
        miningTypeList.add(targetValue.asPrismObject());
        rolesOid.addAll(targetValue.getRoles());

        List<PrismObject<RoleType>> rolePrismObjectList = new ArrayList<>();
        for (String oid : rolesOid) {
            PrismObject<RoleType> roleObject = getRoleObject(getPageBase(), oid, result);
            if (roleObject != null) {
                rolePrismObjectList.add(roleObject);
            }
        }

        List<PrismObject<MiningType>> jaccSortMiningSet = jaccSortMiningSet(miningTypeList);

        Map<PrismObject<RoleType>, Long> roleCountMap = rolePrismObjectList.stream()
                .collect(Collectors.toMap(Function.identity(),
                        role -> jaccSortMiningSet.stream()
                                .filter(miningType -> miningType.asObjectable().getRoles().contains(role.getOid()))
                                .count()));

        List<PrismObject<RoleType>> sortedRolePrismObjectList = roleCountMap.entrySet().stream()
                .sorted(Map.Entry.<PrismObject<RoleType>, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        CustomImageResource imageResource = new CustomImageResource(sortedRolePrismObjectList, jaccSortMiningSet);

        Image image = new Image(ID_IMAGE, imageResource);

        image.add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                target.appendJavaScript(getScaleScript());

            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));

            }
        });

        add(image);

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1700;
    }

    @Override
    public int getHeight() {
        return 1100;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("Details.panel");
    }

    private String getScaleScript() {
        return "let imageContainer = document.querySelector('#imageContainer');" +
                "let image = imageContainer.querySelector('img');" +
                "let scale = 1;" +
                "if (imageContainer && image) {" +
                "  imageContainer.onwheel = function(e) {" +
                "    e.preventDefault();" +
                "    let rectBefore = image.getBoundingClientRect();" +
                "    let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;" +
                "    let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;" +
                "    image.style.transformOrigin = 'left top';" +
                "    if (e.deltaY < 0) {" +
                "      console.log('Zooming in');" +
                "      scale += 0.03;" +
                "      let prevScale = scale - 0.1;" +
                "      let scaleFactor = scale / prevScale;" +
                "      let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);" +
                "      let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);" +
                "      image.style.transformOrigin = x + '%' + ' ' + y + '%';" +
                "      image.style.transition = 'transform 0.3s';" +
                "      image.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = image.getBoundingClientRect();" +
                "      imageContainer.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);" +
                "      imageContainer.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);" +
                "    } else if (e.deltaY > 0) {" +
                "      console.log('Zooming out');" +
                "      scale -= 0.03;" +
                "      scale = Math.max(0.1, scale);" +
                "      image.style.transition = 'transform 0.3s';" +
                "      image.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = image.getBoundingClientRect();" +
                "      imageContainer.scrollLeft += (rectAfter.left - rectBefore.left);" +
                "      imageContainer.scrollTop += (rectAfter.top - rectBefore.top);" +
                "    }" +
                "  };" +
                "} else {" +
                "  console.error('Image or container not found');" +
                "}";
    }

}

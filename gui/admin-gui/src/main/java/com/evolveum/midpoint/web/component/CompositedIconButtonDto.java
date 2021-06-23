/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.markup.html.WebPage;

import java.io.Serializable;

public class CompositedIconButtonDto implements Serializable {

    public static final String F_COMPOSITED_ICON = "compositedIcon";

    private DisplayType additionalButtonDisplayType;
    private CompositedIcon compositedIcon;
    private AssignmentObjectRelation assignmentObjectRelation;
    private CompiledObjectCollectionView collectionView;

    private Class<? extends WebPage> page;

    public CompositedIconButtonDto() {

    }

    public DisplayType getAdditionalButtonDisplayType() {
        return additionalButtonDisplayType;
    }

    public void setAdditionalButtonDisplayType(DisplayType additionalButtonDisplayType) {
        this.additionalButtonDisplayType = additionalButtonDisplayType;
    }

    public void setOrCreateDefaultAdditionalButtonDisplayType(DisplayType additionalButtonDisplayType) {
        if (additionalButtonDisplayType == null) {
            additionalButtonDisplayType = new DisplayType();
        }
        if (additionalButtonDisplayType.getIcon() == null) {
            additionalButtonDisplayType.setIcon(new IconType());
        }
        if (additionalButtonDisplayType.getIcon().getCssClass() == null) {
            additionalButtonDisplayType.getIcon().setCssClass("");
        }

        this.additionalButtonDisplayType = additionalButtonDisplayType;
    }

    public CompositedIcon getCompositedIcon() {
        return compositedIcon;
    }

    public void setCompositedIcon(CompositedIcon compositedIcon) {
        this.compositedIcon = compositedIcon;
    }

    public AssignmentObjectRelation getAssignmentObjectRelation() {
        return assignmentObjectRelation;
    }

    public void setAssignmentObjectRelation(AssignmentObjectRelation assignmentObjectRelation) {
        this.assignmentObjectRelation = assignmentObjectRelation;
    }

    public CompiledObjectCollectionView getCollectionView() {
        return collectionView;
    }

    public void setCollectionView(CompiledObjectCollectionView collectionView) {
        this.collectionView = collectionView;
    }

    public void setPage(Class<? extends WebPage> page) {
        this.page = page;
    }

    public Class<? extends WebPage> getPage() {
        return page;
    }
}

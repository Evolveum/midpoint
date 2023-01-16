/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tag")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TAGS_ALL_URL,
                        label = "PageTag.auth.tag.label",
                        description = "PageTag.auth.tag.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TAG_URL,
                        label = "PageTag.auth.tag.label",
                        description = "PageTag.auth.tag.description")
        })
public class PageTag extends PageAssignmentHolderDetails<TagType, AssignmentHolderDetailsModel<TagType>> {

    private static final long serialVersionUID = 1L;

    public PageTag() {
        super();
    }

    public PageTag(PageParameters parameters) {
        super(parameters);
    }

    public PageTag(final PrismObject<TagType> obj) {
        super(obj);
    }

    @Override
    public Class<TagType> getType() {
        return TagType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<TagType> summaryModel) {
        return new ObjectSummaryPanel<>(id, TagType.class, summaryModel, getSummaryPanelSpecification()) {

            @Override
            protected String getDefaultIconCssClass() {
                return null;
            }

            @Override
            protected String getIconBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected String getBoxAdditionalCssClass() {
                return null;
            }
        };
    }
}

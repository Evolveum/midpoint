/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/mark")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MARKS_ALL_URL,
                        label = "PageTag.auth.mark.label",
                        description = "PageTag.auth.mark.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MARK_URL,
                        label = "PageTag.auth.mark.label",
                        description = "PageTag.auth.mark.description")
        })
public class PageMark extends PageAssignmentHolderDetails<MarkType, AssignmentHolderDetailsModel<MarkType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageMark.class);

    public PageMark() {
        super();
    }

    public PageMark(PageParameters parameters) {
        super(parameters);
    }

    public PageMark(final PrismObject<MarkType> obj) {
        super(obj);
    }

    @Override
    public Class<MarkType> getType() {
        return MarkType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<MarkType> summaryModel) {
        return new ObjectSummaryPanel<>(id, MarkType.class, summaryModel, getSummaryPanelSpecification()) {

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

    private List<Class<? extends Containerable>> getAllDetailsTypes() {
        return Arrays.asList(
                MarkType.class,
                GlobalPolicyRuleType.class
        );
    }

    @Override
    protected AssignmentHolderDetailsModel<MarkType> createObjectDetailsModels(PrismObject<MarkType> object) {
        return new AssignmentHolderDetailsModel<>(createPrismObjectModel(object), this) {

            @Override
            protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
                CompiledGuiProfile profile = getModelServiceLocator().getCompiledGuiProfile();
                try {
                    GuiObjectDetailsPageType defaultPageConfig = null;
                    for (Class<? extends Containerable> clazz : getAllDetailsTypes()) {
                        QName type = GuiImplUtil.getContainerableTypeName(clazz);
                        if (defaultPageConfig == null) {
                            defaultPageConfig = profile.findObjectDetailsConfiguration(type);
                        } else {
                            GuiObjectDetailsPageType anotherConfig = profile.findObjectDetailsConfiguration(type);
                            defaultPageConfig = getModelServiceLocator().getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, anotherConfig);
                        }
                    }

                    return applyArchetypePolicy(defaultPageConfig);
                } catch (Exception ex) {
                    LOGGER.error("Couldn't create default gui object details page and apply archetype policy", ex);
                }

                return null;
            }
        };
    }
}

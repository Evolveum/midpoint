package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {

    public static IModel<String> createActivationTitleModel(IModel<AssignmentEditorDto> model, String defaultTitle, BasePanel basePanel) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentEditorDto dto = model.getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return defaultTitle;
                }

                ActivationStatusType status = activation.getAdministrativeStatus();
                String strEnabled = basePanel.createStringResource(status, "lower", "ActivationStatusType.null")
                        .getString();

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()),
                            MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return defaultTitle;
            }
        };
    }
}

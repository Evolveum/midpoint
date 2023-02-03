package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

public class ObjectReferenceColumnPanel extends BasePanel<ObjectReferenceType> {

    private static final String ID_IMAGE = "image";
    private static final String ID_NAME = "name";

    public ObjectReferenceColumnPanel(String id, IModel<ObjectReferenceType> object) {
        super(id, object);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CompositedIconPanel iconPanel = new CompositedIconPanel(ID_IMAGE, createCompositedIconModel());
        add(iconPanel);
        LinkPanel label = new LinkPanel(ID_NAME, () -> WebComponentUtil.getDisplayNameOrName(getResolvedTarget())) {
            @Override
            public void onClick() {
                WebComponentUtil.dispatchToObjectDetailsPage(getModelObject(), ObjectReferenceColumnPanel.this, false);
            }
        };
        add(label);

    }

    private <R extends AbstractRoleType> PrismObject<R> getResolvedTarget() {
        ObjectReferenceType rowValue = getModelObject();
        if (rowValue == null) {
            return null;
        }
        if (rowValue.getObject() != null) {
            return rowValue.getObject();
        }

        if (rowValue.getOid() != null) {
            PrismObject<R> resolvedTarget = WebModelServiceUtils.loadObject(rowValue, getPageBase());
            if (resolvedTarget != null) {
                return resolvedTarget;
            }
        }
        return null;
    }

    private IModel<CompositedIcon> createCompositedIconModel() {
        return () -> {
            ObjectReferenceType ref = getModelObject();

            PrismObject<? extends ObjectType> object = ref.getObject();
            if (object != null) {
                return WebComponentUtil.createCompositeIconForObject(object.asObjectable(),
                        new OperationResult("create_assignment_composited_icon"), getPageBase());
            }
            String displayType = WebComponentUtil.createDefaultBlackIcon(ref.getType());
            CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
            iconBuilder.setBasicIcon(displayType, IconCssStyle.IN_ROW_STYLE);
            return iconBuilder.build();
        };
    }
}

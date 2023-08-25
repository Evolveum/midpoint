package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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
        add(AttributeAppender.append("class", "d-flex gap-2"));

        CompositedIconPanel iconPanel = new CompositedIconPanel(ID_IMAGE, createCompositedIconModel());
        add(iconPanel);

        AjaxButton name = new AjaxButton(ID_NAME, () -> WebComponentUtil.getDisplayNameOrName(getResolvedTarget())) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DetailsPageUtil.dispatchToObjectDetailsPage(ObjectReferenceColumnPanel.this.getModelObject(), ObjectReferenceColumnPanel.this, false);
            }
        };
        add(name);
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
            if (ref == null) {
                return null;
            }

            PrismObject<? extends ObjectType> object = ref.getObject();
            if (object != null) {
                return WebComponentUtil.createCompositeIconForObject(object.asObjectable(),
                        new OperationResult("create_assignment_composited_icon"), getPageBase());
            }
            String displayType = IconAndStylesUtil.createDefaultBlackIcon(ref.getType());
            CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
            iconBuilder.setBasicIcon(displayType, IconCssStyle.IN_ROW_STYLE);
            return iconBuilder.build();
        };
    }
}

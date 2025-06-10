package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collection;

public class ObjectReferenceColumnPanel extends BasePanel<ObjectReferenceType> {

    private static final String ID_IMAGE = "image";
    private static final String ID_NAME = "name";

    IModel<PrismObject<? extends ObjectType>> target;

    public ObjectReferenceColumnPanel(String id, IModel<ObjectReferenceType> object) {
        super(id, object);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initTargetModel();
        initLayout();
    }

    private void initTargetModel() {
        target = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<? extends ObjectType> load() {
                ObjectReferenceType rowValue = getModelObject();
                if (rowValue == null) {
                    return null;
                }

                if (rowValue.getObject() != null) {
                    return rowValue.getObject();
                }

                if (rowValue.getOid() != null) {
                    return WebModelServiceUtils.loadObject(rowValue, getOptions(), getPageBase());
                }

                return null;
            }
        };
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return null;
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex gap-2"));

        CompositedIconPanel iconPanel = new CompositedIconPanel(ID_IMAGE, createCompositedIconModel());
        add(iconPanel);

        AjaxButton name = new AjaxButton(ID_NAME, () -> WebComponentUtil.getDisplayNameOrName(target.getObject())) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DetailsPageUtil.dispatchToObjectDetailsPage(ObjectReferenceColumnPanel.this.getModelObject(), ObjectReferenceColumnPanel.this, false);
            }
        };
        name.setVisible(target.getObject() != null);
        add(name);
    }

    private IModel<CompositedIcon> createCompositedIconModel() {
        return () -> {
            ObjectReferenceType ref = getModelObject();
            if (ref == null) {
                return null;
            }

            PrismObject<? extends ObjectType> object = ref.getObject();

            if (object == null) {
                object = target.getObject();
            }

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

    @Override
    protected void onDetach() {
        super.onDetach();
        target.detach();
    }
}

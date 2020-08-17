package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
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
        ImagePanel imagePanel = new ImagePanel(ID_IMAGE, new ReadOnlyModel<>(() -> getIconDisplayType()));
        add(imagePanel);
        add(new Label(ID_NAME, WebComponentUtil.getDisplayNameOrName(getModelObject())));

    }

    private DisplayType getIconDisplayType() {
        ObjectReferenceType ref = getModelObject();
        if (ref == null || ref.getType() == null) {
            return WebComponentUtil.createDisplayType("");
        }

        ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
        String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

//        if (guiDescriptor != null) {
//            item.add(AttributeModifier.replace("title", createStringResource(guiDescriptor.getLocalizationKey())));
//            item.add(new TooltipBehavior());
//        }
        return WebComponentUtil.createDisplayType(icon);
    }

    private ObjectTypeGuiDescriptor getObjectTypeDescriptor(QName type) {
        return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
    }
}

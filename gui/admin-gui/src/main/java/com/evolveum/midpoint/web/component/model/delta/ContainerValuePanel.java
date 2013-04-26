package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */
public class ContainerValuePanel extends SimplePanel<ContainerValueDto> {

    private static final Trace LOGGER = TraceManager.getTrace(ModificationsPanel.class);

    private static final String ID_ITEM = "item";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_VALUE = "value";

    public ContainerValuePanel(String id, IModel<ContainerValueDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        add(new ListView<ContainerItemDto>(ID_ITEM, new PropertyModel(getModel(), ContainerValueDto.F_ITEM_LIST)) {
            @Override
            protected void populateItem(ListItem<ContainerItemDto> item) {
                item.add(new Label(ID_ATTRIBUTE, new PropertyModel(item.getModel(), ContainerItemDto.F_ATTRIBUTE)));
                if (item.getModelObject().getValue() instanceof ContainerValueDto) {
                    item.add(new ContainerValuePanel(ID_VALUE, new PropertyModel(item.getModel(), ContainerItemDto.F_VALUE)));
                } else {        // should be String
                    item.add(new Label(ID_VALUE, new PropertyModel(item.getModel(), ContainerItemDto.F_VALUE)));
                }
            }
        });
    }


}

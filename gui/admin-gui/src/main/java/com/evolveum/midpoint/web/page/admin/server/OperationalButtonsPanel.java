package com.evolveum.midpoint.web.page.admin.server;

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class OperationalButtonsPanel extends BasePanel<Void> {

    private static final String ID_BUTTONS = "buttons";

    public OperationalButtonsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        RepeatingView repeatingView = new RepeatingView(ID_BUTTONS);
        add(repeatingView);

        addButtons(repeatingView);
    }

    protected void addButtons(RepeatingView repeatingView) {

    }

    public boolean buttonsExist(){
        RepeatingView repeatingView = (RepeatingView) get(ID_BUTTONS);
        boolean buttonsExist = repeatingView != null && repeatingView.iterator().hasNext();
        if (buttonsExist) {
            Iterator<Component> buttonsIt = repeatingView.iterator();
            while (buttonsIt.hasNext()) {
                Component comp = buttonsIt.next();
                comp.configure();
                if (comp.isVisible()){
                    return true;
                }
            }
        }
        return false;
    }
}

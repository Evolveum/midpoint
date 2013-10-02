package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import org.apache.wicket.markup.html.form.Form;

import java.util.List;

/**
 * @author lazyman
 */
public class PageBootstrap extends PageBase {

    private static final Trace LOGGER = TraceManager.getTrace(PageBootstrap.class);

    public PageBootstrap() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form("form");
        add(form);


    }

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        return null;
    }
}

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessage;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessageFilter;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.atmosphere.Subscribe;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
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

        InlineMenu menu = new InlineMenu("menu", new Model((Serializable) createMenu()));
        form.add(menu);
    }

    private List<InlineMenuItem> createMenu() {
        List<InlineMenuItem> list = new ArrayList<InlineMenuItem>();
        list.add(new InlineMenuItem(createStringResource("menu1"), new InlineMenuItemAction()));
        list.add(new InlineMenuItem(createStringResource("menu2"), new InlineMenuItemAction()));
        list.add(new InlineMenuItem());
        list.add(new InlineMenuItem(createStringResource("menu3"), new Model(false), null, true, new InlineMenuItemAction()));
        list.add(new InlineMenuItem(createStringResource("menu1"), new Model(false), null, false, new InlineMenuItemAction()));

        return list;
    }

    @Subscribe(filter = NotifyMessageFilter.class)
    public void receiveNotifyMessage(AjaxRequestTarget target, NotifyMessage message) {
        LOGGER.info("receiveNotifyMessage");
        StringBuilder sb = new StringBuilder();
        sb.append("$.pnotify({\n");
        sb.append("\ttitle: '").append(createStringResource(message.getTitle()).getString()).append("',\n");
        sb.append("\ttext: '").append(createStringResource(message.getMessage()).getString()).append("',\n");
        sb.append("\ttype: '").append(message.getType().name().toLowerCase()).append("',\n");
        sb.append("\topacity: .8,\n");
        sb.append("\ticon: '").append(message.getIcon()).append("'\n");
        sb.append("});");

        target.appendJavaScript(sb.toString());
    }
}

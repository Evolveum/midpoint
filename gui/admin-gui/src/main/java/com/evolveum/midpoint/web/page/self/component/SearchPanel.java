package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.self.dto.LinkDto;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

/**
 * Created by Kate on 23.09.2015.
 */
public class SearchPanel extends SimplePanel<T>{

    private static final String ID_SEARCH_INPUT = "searchInput";

    public SearchPanel(String id) {
        this(id, null);
    }

    public SearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        final TextField searchInput = new TextField(ID_SEARCH_INPUT);
        add(searchInput);
    }


}

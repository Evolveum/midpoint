package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import org.apache.wicket.model.IModel;

/**
 * Created by Kate Honchar.
 */
public class StageDefinitionPanel extends SimplePanel<StageDefinitionDto> {
    public StageDefinitionPanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
    }

}

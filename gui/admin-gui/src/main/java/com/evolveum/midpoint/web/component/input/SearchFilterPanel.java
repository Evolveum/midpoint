/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.dto.SearchFilterTypeDto;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class SearchFilterPanel<T extends SearchFilterType> extends SimplePanel<T>{

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterPanel.class);

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_FILTER_CLAUSE = "filterClause";
    private static final String ID_BUTTON_UPDATE = "update";

    protected IModel<SearchFilterTypeDto> model;

    public SearchFilterPanel(String id, IModel<T> model){
        super(id, model);
    }

    private void loadModel(){
        if(model == null){
            model = new LoadableModel<SearchFilterTypeDto>() {

                @Override
                protected SearchFilterTypeDto load() {
                    return new SearchFilterTypeDto(getModel().getObject(), getPageBase().getPrismContext());
                }
            };
        }
    }

    @Override
    protected void initLayout(){
        loadModel();

        TextArea description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<String>(model, SearchFilterTypeDto.F_FILTER_OBJECT + ".description"));
        add(description);

        TextArea filterClause = new TextArea<>(ID_FILTER_CLAUSE,
                new PropertyModel<String>(model, SearchFilterTypeDto.F_FILTER_CLAUSE));
        add(filterClause);

        AjaxSubmitLink update = new AjaxSubmitLink(ID_BUTTON_UPDATE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateClausePerformed(target);
            }
        };
        add(update);
    }

    private void updateClausePerformed(AjaxRequestTarget target){
        try{
            model.getObject().updateFilterClause(getPageBase().getPrismContext());

            success(getString("SearchFilterPanel.message.expressionSuccess"));
        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Could not create MapXNode from provided XML filterClause.", e);
            error(getString("SearchFilterPanel.message.cantSerialize"));
        }

        performFilterClauseHook(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    /**
     *  Override this in component with SearchFilterPanel to provide additional functionality when filterClause is updated
     * */
    public void performFilterClauseHook(AjaxRequestTarget target){}
}

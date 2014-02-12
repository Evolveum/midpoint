/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.web.util;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

/**
 *  @author shood
 * */
public class SearchFormEnterBehavior extends Behavior {
    private static final long serialVersionUID = 1L;

    @Override
    public void renderHead(Component component, IHeaderResponse response){
        super.renderHead(component, response);
        String[] args = getSourceAndTarget();

        response.render(OnDomReadyHeaderItem.forScript("$( \"#"+args[0]+"\" ).on(\"keypress\",function(event) {" +
                "if(event.which==13){ $(\"#"+args[1]+"\").click();return false;}" +
                "});"));
    }

    public String[] getSourceAndTarget(){
        return new String[]{"",""};
    }


}

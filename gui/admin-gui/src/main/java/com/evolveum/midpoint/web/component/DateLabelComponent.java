/*
 * Copyright (c) 2010-2017 Evolveum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.model.IModel;

import java.util.Date;

/**
 * Created by honchar
 * Component for displaying date value as a label
 * By default (if no converter is set) the date is formatted
 * according to the client's locale, timezone (not implemented yet),
 * with applying long style for date and long style for time.
 */
public class DateLabelComponent extends DateLabel {

    public static final String SHORT_SHORT_STYLE = "SS";    //short style for date, short style for time
    public static final String MEDIUM_MEDIUM_STYLE = "MM";    //medium style for date, medium style for time
    public static final String LONG_LONG_STYLE = "LL";    //long style for date, long style for time
    public static final String FULL_FULL_STYLE = "FF";    //full style for date, full style for time
    public static final String SHORT_MEDIUM_STYLE = "SM";    //short style for date, medium style for time
    public static final String SHORT_LONG_STYLE = "SL";    //short style for date, long style for time
    public static final String SHORT_FULL_STYLE = "SF";    //short style for date, full style for time
    public static final String SHORT_NOTIME_STYLE = "S-";    //short style for date, no time
    public static final String MEDIUM_SHORT_STYLE = "MS";    //medium style for date, short style for time
    public static final String MEDIUM_LONG_STYLE = "ML";    //medium style for date, long style for time
    public static final String MEDIUM_FULL_STYLE = "MF";    //medium style for date, full style for time
    public static final String MEDIUM_NOTIME_STYLE = "M-";    //medium style for date, no time
    public static final String LONG_SHORT_STYLE = "LS";    //long style for date, short style for time
    public static final String LONG_MEDIUM_STYLE = "LM";    //long style for date, medium style for time
    public static final String LONG_FULL_STYLE = "LF";    //long style for date, full style for time
    public static final String LONG_NOTIME_STYLE = "L-";    //long style for date, no time
    public static final String FULL_SHORT_STYLE = "FS";    //full style for date, short style for time
    public static final String FULL_MEDIUM_STYLE = "FM";    //full style for date, medium style for time
    public static final String FULL_LONG_STYLE = "FL";    //full style for date, long style for time
    public static final String FULL_NOTIME_STYLE = "F-";    //full style for date, no time
    public static final String NODATE_SHORT_STYLE = "-S";    //no date, short style for time
    public static final String NODATE_MEDIUM_STYLE = "-M";    //no date, medium style for time
    public static final String NODATE_LONG_STYLE = "-L";    //no date, long style for time
    public static final String NODATE_FULL_STYLE = "-F";    //no date, full style for time

    public DateLabelComponent(String id, IModel<Date> model){
        this(id, model, (DateConverter) null);
    }

    public DateLabelComponent(String id, IModel<Date> model, DateConverter converter){
        this(id, model, converter, null, "", "");
    }

    public DateLabelComponent(String id, IModel<Date> model, String style){
        this(id, model, null, style, "", "");
    }

    public DateLabelComponent(String id, IModel<Date> model, DateConverter converter, String style,
                              String beforeDateText, String afterDateText){
        super(id, model, converter == null ?
                new PatternDateConverter(WebComponentUtil.getLocalizedDatePattern(style == null ? LONG_LONG_STYLE : style), true ) : converter);

        setBefore(beforeDateText);
        setAfter(afterDateText);
    }

    public void setBeforeTextOnDateNull(String nullDateText){
        if (getModel().getObject() == null){
            setBefore(nullDateText);
        }
    }
}


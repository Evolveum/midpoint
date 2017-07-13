/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.marshaller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * TODO: This should do more parsing in the future. But good for now.
 *
 * @author semancik
 */
public class TrivialXPathParser {

    private Map<String,String> namespaceMap;
    private String pureXPathString;

    private TrivialXPathParser() {
        namespaceMap = new HashMap<String, String>();
        pureXPathString = "";
    }

    public static TrivialXPathParser parse(String xpath) {

        TrivialXPathParser parser = new TrivialXPathParser();

        // This is using regexp to "parse" the declarations. It is not ideal,
        // it does not check the syntax, does not show reasonable errors, etc.
        // But it was all done in like 20minutes. Good value/price ;-)

        String regexp = "(^|;)[\\s\\p{Z}]*declare[\\s\\p{Z}]+(default[\\s\\p{Z}]+)?namespace[\\s\\p{Z}]+((\\w+)[\\s\\p{Z}]*=[\\s\\p{Z}]*)?(['\"])([^'\"]*)\\5[\\s\\p{Z}]*(?=;)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(xpath);

//        System.out.println("regexp: "+regexp);
//        System.out.println("xpath: "+xpath);

        int lastEnd=0;
        while (matcher.find()) {
            String prefix = matcher.group(4);
            String url = matcher.group(6);
            if (matcher.group(2)!=null) {
                // default namespace declaration
                prefix = "";
            }
//            System.out.println("match: "+prefix+" : "+url);
            parser.namespaceMap.put(prefix, url);
            lastEnd = matcher.end();
        }

        parser.pureXPathString = xpath;
//        System.out.println("End: "+lastEnd);
        
        if (lastEnd>0) {
            // Skip colon (as it is look-ahead assertion in the pattern) and trim
            parser.pureXPathString = xpath.substring(lastEnd+1).trim();
        }
//        System.out.println("Pure Xpath: "+parser.pureXPathString);

        // Trim whitechars
        // trim() won't do here. it is not trimming non-breakable spaces.

        parser.pureXPathString = parser.pureXPathString.replaceFirst("^[\\p{Z}\\s]+", "").replaceFirst("[\\p{Z}\\s]+$", "");

        return parser;
    }

    public Map<String,String> getNamespaceMap() {
        return namespaceMap;
    }

    public String getPureXPathString() {
        return pureXPathString;
    }

}

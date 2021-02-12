/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.util;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Schrodinger {

    public static final String SCHRODINGER_ELEMENT = "schrodinger";

    public static final String DATA_S_RESOURCE_KEY = "data-s-resource-key";
    public static final String DATA_S_ID = "data-s-id";
    public static final String DATA_S_QNAME = "data-s-qname";

    /**
     * <p>Produces a XPath query pointing to any element which contains a specified <code>data-s-resource-key</code> value. The specified element is queried based on the value of the
     * element attribute <code>data-s-resource-key</code></p>
     *
     * @param key
     * <p>Value of the element attribute <code>data-s-resource-key</code></p>
     *
     * @return the XPath query value for the searched element
     */


    public static By byDataResourceKey(String key) {
        return byDataResourceKey(null, key);
    }

    /**
     * <p>Produces a XPath query pointing to a <code>schrodinger</code> element. The specified element is queried based on the value of the
     * element attribute <code>data-s-resource-key</code></p>
     *
     * @param key
     * <p>Value of the element attribute <code>data-s-resource-key</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySchrodingerDataResourceKey(String key) {
        return byDataResourceKey(SCHRODINGER_ELEMENT, key);
    }

    /**
     * <p>Produces a XPath query pointing to a specified element. The specified element is queried based on the value of the
     * element attribute <code>data-s-resource-key</code></p>
     *
     * @param elementName
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param key
     * <p>Value of the element attribute <code>data-s-resource-key</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDataResourceKey(String elementName, String key) {
        if (elementName == null) {
            elementName = "*";
        }

        return byElementAttributeValue(elementName, DATA_S_RESOURCE_KEY, key);
    }

    /**
     * <p>Produces a XPath query pointing to any element which contains a specified <code>data-s-id</code> value. The specified element is queried based on the value of the
     * element attribute <code>data-s-id</code></p>
     *
     * @param id
     * <p>Value of the element attribute <code>data-s-id</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDataId(String id) {
        return byDataId(null, id);
    }

    /**
     * <p>Produces a XPath query pointing to a <code>schrodinger</code> element. The specified element is queried based on the value of the
     * element attribute <code>data-s-id</code></p>
     *
     * @param id
     * <p>Value of the element attribute <code>data-s-id</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySchrodingerDataId(String id) {
        return byDataId(SCHRODINGER_ELEMENT, id);
    }

    /**
     * <p>Produces a XPath query pointing to a specified element. The specified element is queried based on the value of the
     * element attribute <code>data-s-id</code></p>
     *
     * @param elementName
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param id
     * <p>Value of the element attribute <code>data-s-id</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDataId(String elementName, String id) {
        return byElementAttributeValue(elementName, DATA_S_ID, id);
    }

    /**
     * <p>Produces a XPath query pointing to any element which contains a specified <code>data-s-qname</code> value. The specified element is queried based on the value of the
     * element attribute <code>data-s-qname</code></p>
     *
     * @param qname
     * <p>Value of the element attribute <code>data-s-qname</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDataQName(String qname) {
        return byDataQName(null, qname);
    }

    /**
     * <p>Produces a XPath query pointing to a <code>schrodinger</code> element. The specified element is queried based on the value of the
     * element attribute <code>data-s-qname</code></p>
     *
     * @param qname
     * <p>Value of the <code>schrodinger</code> element attribute <code>data-s-qname</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySchrodingerDataQName(String qname) {
        return byDataQName(SCHRODINGER_ELEMENT, qname);
    }

    /**
     * <p>Produces a XPath query pointing to an element. The specified element is queried based on the value of the
     * element attribute <code>data-s-qname</code></p>
     *
     * @param elementName
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param qname
     * <p>Value of the element attribute <code>data-s-qname</code></p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDataQName(String elementName, String qname) {
        return byElementAttributeValue(elementName, DATA_S_QNAME, qname);
    }

    /**
     * <p>Produces a XPath query pointing to an element. The specified element is queried based on one of the
     * element attributes and the exact attribute value used for the query</p>
     *
     * @param element
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the element which is the subject of the query</p>
     *
     * @param value
     * <p>The value of the attribute which is used in this query</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byElementAttributeValue(String element, String attr, String value) {
        if (element == null) {
            element = "*";
        }
        return By.xpath("//" + element + "[@" + attr + "='" + value + "']");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The specified element is queried based on one of the
     * element attributes, additionally a function which should be used in the XPath query is specified</p>
     *
     * @param element
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param function
     * <p>Function which should be used in the construction of the XPath query e.g. <code>contains</code></p>
     *
     * @param attr
     * <p>Attribute of the element which is the subject of the query</p>
     *
     * @param value
     * <p>The value of the attribute which is used in this query</p>
     *
     * @return the XPath query value for the searched element
     */


    public static By byElementAttributeValue(String element, String function, String attr, String value) {
        if (element == null) {
            element = "*";
        }

        return By.xpath("//" + element + "[" + function + "(@" + attr + ",'" + value + "')]");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The specified element is queried based on the enclosed value
     * of any of it's descendant elements</p>
     *
     * @param element
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param enclosedText
     * <p>The  string value or its part which is encompassed between the element brackets</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byElementValue(String element, String attr, String attrValue, String enclosedText) {

        if (element == null) {
            element = "*";
        }

        return byElementValue(element, attr, attrValue, null, enclosedText);
    }

    /**
     * <p>Produces a XPath query pointing to an element. The specified element is queried based on the enclosed value
     * of one of it's specified descendant elements</p>
     *
     * @param element
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param enclosingElement
     * <p>The element which contains the queried string value</p>
     *
     * @param enclosedText
     * <p>The  string value or its part which is encompassed between the element brackets</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byElementValue(String element, String attr, String attrValue, String enclosingElement, String enclosedText) {

        if (element == null) {
            element = "*";
        }
        if (enclosingElement == null) {
            enclosingElement = ".";
        }

        return By.xpath("//" + element + "[@" + attr + "=\"" + attrValue + "\"][contains(" + enclosingElement + ",\"" + enclosedText + "\")]/..");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The XPath is constructed in a way tha we look for the specified element
     * with the string value as the value encompassed between the element brackets</p>
     *
     * @param elementName
     * <p>The name of the element which is subject of the search. e.g. <code>li</code></p>
     *
     * @param value
     * <p>The exact string value which is encompassed between the element brackets</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byElementValue(String elementName, String value) {
        if (elementName == null) {
            elementName = "*";
        }

        return By.xpath(".//" + elementName + "[text()='" + value + "']");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the ancestor of the searched element which is
     * identified via a specified attribute</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param ancestorAttr
     * <p>Attribute of the ancestor (or self) of the element e.g. <code>data-s-id</code></p>
     *
     * @param ancestorAttrValue
     * <p>Value of the ancestor (or self) attribute</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySelfOrAncestorElementAttributeValue(String element, String attr, String attrValue, String ancestorAttr, String ancestorAttrValue) {
        if (element == null) {
            element = "*";
        }

        return By.xpath("//" + element + "[@" + attr + "=\"" + attrValue + "\" and ancestor-or-self::*[@" + ancestorAttr + "=\"" + ancestorAttrValue + "\"]]");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the descendant of the searched element or the descendant of the
     * ancestor of the searched element which is identified via a specified attribute</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>. If attribute contains an empty value the XPath is constructed
     * in a way that we expect an element without attributes. If the attribute is null that this is translated into
     * "any attribute". </p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param ancestorAttr
     * <p>Attribute of the ancestor (or self) of the searched element e.g. <code>data-s-id</code></p>. If attribute contains an empty value the XPath is constructed
     * in a way that we expect an element without attributes. If the attribute is null that this is translated into
     * "any attribute". </p>
     *
     * @param ancestorAttrValue
     * <p>Value of the ancestor (or self) attribute</p>
     *
     * @param ancestorsDescendantAttr
     * <p>Value of the descendants attribute of the ancestor (or self)If attribute contains an empty value the XPath is constructed
     * in a way that we expect an element without attributes. If the attribute is null that this is translated into "any attribute".</p>
     *
     * @param ancestorsDescendantAttrValue
     * <p>Value of the descendants attribute</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySelfOrDescendantOfAncestorElementAttributeValue(String element, String attr, String attrValue, String ancestorAttr, String ancestorAttrValue, String ancestorsDescendantAttr, String ancestorsDescendantAttrValue) {
        if (element == null) {
            element = "*";
        }
        StringBuilder xpathBuilder = new StringBuilder("//").append(element).append("[");

        if(attr!=null){
            if(!attr.isEmpty()){
                xpathBuilder.append("@").append(attr).append("=\"").append(attrValue).append("\"");
            }else{

                xpathBuilder.append("not(@*)");
            }

        }else{
            xpathBuilder.append("@").append("*");

        }
        xpathBuilder.append(" and ancestor-or-self::*[");
        if(ancestorAttr!=null){
            if(!ancestorAttr.isEmpty()){
                xpathBuilder.append("@").append(ancestorAttr).append("=\"").append(ancestorAttrValue).append("\"");
            }else{

                xpathBuilder.append("not(@*)");
            }

        }else{
            xpathBuilder.append("@").append("*");

        }
        xpathBuilder.append(" and descendant-or-self::*[");

        if(ancestorsDescendantAttr!=null){
            if(!ancestorsDescendantAttr.isEmpty()){
                xpathBuilder.append("@").append(ancestorsDescendantAttr).append("=\"").append(ancestorsDescendantAttrValue).append("\"");
            }else{

                xpathBuilder.append("not(@*)");
            }

        }else{
            xpathBuilder.append("@").append("*");
        }

        return By.xpath(xpathBuilder.append("]]]").toString());
    }

    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the descendant of the searched element which is
     * identified via a specified attribute</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param descendantAttr
     * <p>Attribute of the descendant of the searched element e.g. <code>data-s-id</code></p>
     *
     * @param descendantAttrValue
     * <p>Value of the descendants attribute</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By bySelfOrDescendantElementAttributeValue(String element, String attr, String attrValue, String descendantAttr, String descendantAttrValue) {
        if (element == null) {
            element = "*";
        }

        return By.xpath("//" + element + "[@" + attr + "=\"" + attrValue + "\" and descendant-or-self::*[@" + descendantAttr + "=\"" + descendantAttrValue + "\"]]");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the descendant of the searched element which is
     * identified via a specified attribute</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param descendantAttr
     * <p>Attribute of the descendant of the searched element e.g. <code>data-s-id</code></p>
     *
     * @param descendantAttrValue
     * <p>Value of the descendants attribute</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byDescendantElementAttributeValue(String element, String descendantAttr, String descendantAttrValue) {
        if (element == null) {
            element = "*";
        }

        return By.xpath("//" + element + "[descendant-or-self::*[@" + descendantAttr + "=\"" + descendantAttrValue + "\"]]");
    }

    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the following sibling of the searched element and the value
     * which is enclosed between it's HTML tags or the element tags of the siblings descendants</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param siblingAttr
     * <p>Attribute of the following sibling of the searched element e.g. <code>data-s-id</code></p>
     *
     * @param siblingAttrValue
     * <p>Value of the following siblings attribute</p>
     *
     * @param siblingEnclosedText
     * <p>String which is enclosed between the HTML element tags of the preceding sibling element or its child element</p>
     *
     * @return the XPath query value for the searched element
     */


    public static By byFollowingSiblingEnclosedValue(String element, String attr, String attrValue, String siblingAttr, String siblingAttrValue, String siblingEnclosedText) {
        if (element == null) {
            element = "*";
        }

        return By.xpath("//" + element + "[@" + attr + "=\"" + attrValue + "\" and following-sibling::*[@" + siblingAttr + "=\"" + siblingAttrValue + "\" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]");
    }


    /**
     * <p>Produces a XPath query pointing to an element. The search is based on the preceding sibling of the searched element and the value
     * which is enclosed between it's HTML tags or the element tags of the siblings descendants</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>. If attribute contains an empty value the XPath is constructed
     * in a way that we expect an element without attributes. If the attribute is null that this is translated into
     * "any attribute".</p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param siblingAttr
     * <p>Attribute of the preceding sibling of the searched element e.g. <code>data-s-id</code></p>. If attribute contains an empty value the XPath is constructed
     * in a way that we expect an element without attributes. If the attribute is null that this is translated into
     * "any attribute".</p>
     *
     * @param siblingAttrValue
     * <p>Value of the preceding siblings attribute</p>
     *
     * @param siblingEnclosedText
     * <p>String which is enclosed between the HTML element tags of the preceding sibling element or its child element</p>
     *
     * @return the XPath query value for the searched element
     */


    public static By byPrecedingSiblingEnclosedValue(String element, String attr, String attrValue, String siblingAttr, String siblingAttrValue, String siblingEnclosedText) {
        if (element == null) {
            element = "*";
        }

        StringBuilder xpathBuilder = new StringBuilder("//").append(element).append("[");

        if (attr != null) {
            if (!attr.isEmpty()){
                xpathBuilder.append("@").append(attr).append("=\"").append(attrValue).append("\" and preceding-sibling::*[");

            }else{
                xpathBuilder.append("not(@*)").append(" and preceding-sibling::*[");
            }
        } else {
            xpathBuilder.append("@").append("*").append(" and preceding-sibling::*[");
        }

        if (siblingAttr != null) {
            if (!siblingAttr.isEmpty()){
            xpathBuilder.append("@").append(siblingAttr).append("=\"").append(siblingAttrValue).append("\" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]");
        }else{
                xpathBuilder.append("not(@*)").append(" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]");
        }
        } else {
            xpathBuilder.append("@").append("*").append(" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]");
        }

        return By.xpath(xpathBuilder.toString());
    }


    /**
     * <p>Produces a XPath query pointing to an element. This is based on the elements ancestors preceding sibling or
     * any of the siblings children. Search is based on the ancestors preceding sibling (or its children) enclosed text</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param ancestorAttr
     * <p>Attribute of the ancestor of the searched element e.g. <code>data-s-id</code></p>
     *
     * @param ancestorAttrValue
     * <p>Value of the ancestor attribute</p>
     *
     * @param siblingEnclosedText
     * <p>String which is enclosed between the HTML element tags of the ancestors preceding sibling element or its child element</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue(String element, String attr, String attrValue, String ancestorAttr, String ancestorAttrValue, String siblingEnclosedText) {
        if (element == null) {
            element = "*";
        }

        StringBuilder xpathBuilder = new StringBuilder("//").append(element).append("[ancestor::*[");

        if (ancestorAttr != null) {
            xpathBuilder.append("@").append(ancestorAttr).append("=\"").append(ancestorAttrValue)
                    .append("\" and preceding-sibling::*[descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]")
                    .append(" or preceding-sibling::*[").append("@").append(ancestorAttr).append("=\"").append(ancestorAttrValue)
                    .append("\" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]");
        } else {
            xpathBuilder.append("preceding-sibling::*[descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]]");
        }

        if (attr != null) {
            xpathBuilder.append("[@").append(attr).append("=\"").append(attrValue).append("\"]");
        }

        return By.xpath(xpathBuilder.toString());
    }

    /**
     * <p>Produces a XPath query pointing to an element. This is based on the elements ancestors following sibling or
     * any of the siblings children. Search is based on the ancestors following sibling (or its children) enclosed text</p>
     *
     * @param element
     * <p>An element which is the subject of the search. e.g. <code>li</code></p>
     *
     * @param attr
     * <p>Attribute of the searched element e.g. <code>type</code></p>
     *
     * @param attrValue
     * <p>Value of the previously specified attribute</p>
     *
     * @param ancestorAttr
     * <p>Attribute of the ancestor of the searched element e.g. <code>data-s-id</code></p>
     *
     * @param ancestorAttrValue
     * <p>Value of the ancestor attribute</p>
     *
     * @param siblingEnclosedText
     * <p>String which is enclosed between the HTML element tags of the ancestors sibling element or its child element</p>
     *
     * @return the XPath query value for the searched element
     */

    public static By byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue(String element, String attr, String attrValue, String ancestorAttr, String ancestorAttrValue, String siblingEnclosedText) {
        if (element == null) {
            element = "*";
        }

        StringBuilder xpathBuilder = new StringBuilder("//").append(element).append("[ancestor::*[");

        if (ancestorAttr != null) {
            xpathBuilder.append("@").append(ancestorAttr).append("=\"").append(ancestorAttrValue)
                    .append("\" and following-sibling::*[descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]")
                    .append(" or following-sibling::*[").append("@").append(ancestorAttr).append("=\"").append(ancestorAttrValue)
                    .append("\" and descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]]");
        } else {
            xpathBuilder.append("following-sibling::*[descendant-or-self::*[contains(.,\"" + siblingEnclosedText + "\")]]]]");
        }

        if (attr != null) {
            xpathBuilder.append("[@").append(attr).append("=\"").append(attrValue).append("\"]");
        }

        return By.xpath(xpathBuilder.toString());
    }

/**
 * @param qname
 * <p>The <code>QName</code> value of property</p>
 *
 * @return the <code>QName</code> as a string value
 */
    public static String qnameToString(QName qname) {
        if (qname == null) {
            return null;
        }

        return StringUtils.join(new Object[]{qname.getNamespaceURI(), qname.getLocalPart()}, "#");
    }


}

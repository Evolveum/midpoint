
/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.foo;

import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventHandlerChainType", propOrder = {
    "handler"
})
public class EventHandlerChainType
    extends EventHandlerType
    implements Serializable, Cloneable, Equals, HashCode
{
    public final static QName COMPLEX_TYPE = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "EventHandlerChainType");

    private final static long serialVersionUID = 201105211233L;
    @XmlElementRef(name = "handler", namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends EventHandlerType>> handler;

    /**
     * Creates a new {@code EventHandlerChainType} instance.
     *
     */
    public EventHandlerChainType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code EventHandlerChainType} instance by deeply copying a given {@code EventHandlerChainType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public EventHandlerChainType(final EventHandlerChainType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'EventHandlerChainType' from 'null'.");
        }
        // 'Handler' collection.
        if (o.handler!= null) {
            copyHandler(o.getHandler(), this.getHandler());
        }
    }

    /**
     * Gets the value of the handler property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the handler property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHandler().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link SimpleWorkflowNotifierType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link SimpleResourceObjectNotifierType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventHandlerChainType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link DummyNotifierType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link UserPasswordNotifierType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link AccountPasswordNotifierType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventCategoryFilterType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventHandlerType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventStatusFilterType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventExpressionFilterType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventHandlerForkType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link EventOperationFilterType }{@code >}
     * {@link javax.xml.bind.JAXBElement }{@code <}{@link SimpleUserNotifierType }{@code >}
     *
     *
     */
    public List<JAXBElement<? extends EventHandlerType>> getHandler() {
        if (handler == null) {
            handler = new ArrayList<>();
        }
        return this.handler;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     *
     */
    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = super.hashCode(locator, strategy);
        {
            List<JAXBElement<? extends EventHandlerType>> theHandler;
            theHandler = (((this.handler!= null)&&(!this.handler.isEmpty()))?this.getHandler():null);
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "handler", theHandler), currentHashCode, theHandler);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof EventHandlerChainType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final EventHandlerChainType that = ((EventHandlerChainType) object);
        {
            List<JAXBElement<? extends EventHandlerType>> lhsHandler;
            lhsHandler = (((this.handler!= null)&&(!this.handler.isEmpty()))?this.getHandler():null);
            List<JAXBElement<? extends EventHandlerType>> rhsHandler;
            rhsHandler = (((that.handler!= null)&&(!that.handler.isEmpty()))?that.getHandler():null);
            if (!strategy.equals(LocatorUtils.property(thisLocator, "handler", lhsHandler), LocatorUtils.property(thatLocator, "handler", rhsHandler), lhsHandler, rhsHandler)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Copies all values of property {@code Handler} deeply.
     *
     * @param source
     *     The source to copy from.
     * @param target
     *     The target to copy {@code source} to.
     * @throws NullPointerException
     *     if {@code target} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static void copyHandler(final List<JAXBElement<? extends EventHandlerType>> source, final List<JAXBElement<? extends EventHandlerType>> target) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if ((source!= null)&&(!source.isEmpty())) {
            for (final Iterator<?> it = source.iterator(); it.hasNext(); ) {
                final Object next = it.next();
                if (next instanceof JAXBElement) {
                    // Referenced elements without classes.
                    if (((JAXBElement) next).getValue() instanceof EventOperationFilterType) {
                        // CElementInfo: javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationFilterType>
                        target.add(copyOfEventOperationFilterTypeElement(((JAXBElement) next)));
                        continue;
                    }
                    if (((JAXBElement) next).getValue() instanceof EventStatusFilterType) {
                        // CElementInfo: javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusFilterType>
                        target.add(copyOfEventStatusFilterTypeElement(((JAXBElement) next)));
                        continue;
                    }
                    if (((JAXBElement) next).getValue() instanceof EventCategoryFilterType) {
                        // CElementInfo: javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryFilterType>
                        target.add(copyOfEventCategoryFilterTypeElement(((JAXBElement) next)));
                        continue;
                    }
                    if (((JAXBElement) next).getValue() instanceof EventHandlerChainType) {
                        // CElementInfo: javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerChainType>
                        target.add(copyOfEventHandlerChainTypeElement(((JAXBElement) next)));
                        continue;
                    }
                    if (((JAXBElement) next).getValue() instanceof EventHandlerType) {
                        // CElementInfo: javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType>
                        target.add(copyOfEventHandlerTypeElement(((JAXBElement) next)));
                        continue;
                    }
                }
                // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                throw new AssertionError((("Unexpected instance '"+ next)+"' for property 'Handler' of class 'com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerChainType'."));
            }
        }
    }

    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationFilterType>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<EventOperationFilterType> copyOfEventOperationFilterTypeElement(final JAXBElement<EventOperationFilterType> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<EventOperationFilterType> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CClassInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationFilterType
            copy.setValue(((((EventOperationFilterType) copy.getValue()) == null)?null:((EventOperationFilterType) copy.getValue()).clone()));
            return copy;
        }
        return null;
    }


    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusFilterType>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<EventStatusFilterType> copyOfEventStatusFilterTypeElement(final JAXBElement<EventStatusFilterType> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<EventStatusFilterType> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CClassInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusFilterType
            copy.setValue(((((EventStatusFilterType) copy.getValue()) == null)?null:((EventStatusFilterType) copy.getValue()).clone()));
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryFilterType>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<EventCategoryFilterType> copyOfEventCategoryFilterTypeElement(final JAXBElement<EventCategoryFilterType> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<EventCategoryFilterType> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CClassInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryFilterType
            copy.setValue(((((EventCategoryFilterType) copy.getValue()) == null)?null:((EventCategoryFilterType) copy.getValue()).clone()));
            return copy;
        }
        return null;
    }


    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerChainType>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<EventHandlerChainType> copyOfEventHandlerChainTypeElement(final JAXBElement<EventHandlerChainType> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<EventHandlerChainType> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CClassInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerChainType
            copy.setValue(((((EventHandlerChainType) copy.getValue()) == null)?null:((EventHandlerChainType) copy.getValue()).clone()));
            return copy;
        }
        return null;
    }


    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<EventHandlerType> copyOfEventHandlerTypeElement(final JAXBElement<EventHandlerType> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<EventHandlerType> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CClassInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType
            copy.setValue(((((EventHandlerType) copy.getValue()) == null)?null:((EventHandlerType) copy.getValue()).clone()));
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public EventHandlerChainType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final EventHandlerChainType clone = ((EventHandlerChainType) super.clone());
            // 'Handler' collection.
            if (this.handler!= null) {
                clone.handler = null;
                copyHandler(this.getHandler(), clone.getHandler());
            }
            return clone;
        }
    }

    @Override
    public String toString() {
        return "EventHandlerChainType{" +
                "handler=" + handler +
                '}';
    }
}

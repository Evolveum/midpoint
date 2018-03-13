
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * <p>Java class for EventOperationFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EventOperationFilterType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://midpoint.evolveum.com/xml/ns/public/common/common-3}EventHandlerType">
 *       &lt;sequence>
 *         &lt;element name="operation" type="{http://midpoint.evolveum.com/xml/ns/public/common/common-3}EventOperationType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventOperationFilterType", propOrder = {
    "operation"
})
public class EventOperationFilterType
    extends EventHandlerType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    protected List<String> operation;

    /**
     * Creates a new {@code EventOperationFilterType} instance.
     *
     */
    public EventOperationFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code EventOperationFilterType} instance by deeply copying a given {@code EventOperationFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public EventOperationFilterType(final EventOperationFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'EventOperationFilterType' from 'null'.");
        }
        // 'Operation' collection.
        if (o.operation!= null) {
            copyOperation(o.getOperation(), this.getOperation());
        }
    }

    /**
     * Gets the value of the operation property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the operation property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOperation().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EventOperationType }
     *
     *
     */
    public List<String> getOperation() {
        if (operation == null) {
            operation = new ArrayList<>();
        }
        return this.operation;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     *
     */

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = super.hashCode(locator, strategy);
        {
            List<String> theOperation;
            theOperation = (((this.operation!= null)&&(!this.operation.isEmpty()))?this.getOperation():null);
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "operation", theOperation), currentHashCode, theOperation);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof EventOperationFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final EventOperationFilterType that = ((EventOperationFilterType) object);
        {
            List<String> lhsOperation;
            lhsOperation = (((this.operation!= null)&&(!this.operation.isEmpty()))?this.getOperation():null);
            List<String> rhsOperation;
            rhsOperation = (((that.operation!= null)&&(!that.operation.isEmpty()))?that.getOperation():null);
            if (!strategy.equals(LocatorUtils.property(thisLocator, "operation", lhsOperation), LocatorUtils.property(thatLocator, "operation", rhsOperation), lhsOperation, rhsOperation)) {
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
     * Copies all values of property {@code Operation} deeply.
     *
     * @param source
     *     The source to copy from.
     * @param target
     *     The target to copy {@code source} to.
     * @throws NullPointerException
     *     if {@code target} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static void copyOperation(final List<String> source, final List<String> target) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if ((source!= null)&&(!source.isEmpty())) {
            for (final Iterator<?> it = source.iterator(); it.hasNext(); ) {
                final Object next = it.next();
                if (next instanceof String) {
                    // CEnumLeafInfo: com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType
                    target.add(((String) next));
                    continue;
                }
                // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                throw new AssertionError((("Unexpected instance '"+ next)+"' for property 'Operation' of class 'com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationFilterType'."));
            }
        }
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public EventOperationFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final EventOperationFilterType clone = ((EventOperationFilterType) super.clone());
            // 'Operation' collection.
            if (this.operation!= null) {
                clone.operation = null;
                copyOperation(this.getOperation(), clone.getOperation());
            }
            return clone;
        }
    }

    @Override
    public String toString() {
        return "EventOperationFilterType{" +
                "operation=" + operation +
                '}';
    }
}

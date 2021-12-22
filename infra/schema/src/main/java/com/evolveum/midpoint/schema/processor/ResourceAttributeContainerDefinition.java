/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Definition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO review docs
 *
 * Resource Object Definition (Object Class).
 *
 * Object Class refers to a type of object on the Resource. Unix account, Active
 * Directory group, inetOrgPerson LDAP objectclass or a schema of USERS database
 * table are all Object Classes from the midPoint point of view. Object class
 * defines a set of attribute names, types for each attributes and few
 * additional properties.
 *
 * This class represents schema definition for resource object (object class).
 * See {@link Definition} for more details.
 *
 * Resource Object Definition is immutable. TODO: This will probably need to be changed to a mutable object.
 *
 * @author Radovan Semancik
 */
public interface ResourceAttributeContainerDefinition extends PrismContainerDefinition<ShadowAttributesType> {

    @Override
    ResourceObjectDefinition getComplexTypeDefinition();

    /**
     * TODO review docs
     *
     * Returns the definition of primary identifier attributes of a resource object.
     *
     * May return empty set if there are no identifier attributes. Must not
     * return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of identifier attributes
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers();

    /**
     * TODO review docs
     *
     * Returns the definition of secondary identifier attributes of a resource
     * object.
     *
     * May return empty set if there are no secondary identifier attributes.
     * Must not return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of secondary identifier attributes
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers();

    Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers();

    /**
     * TODO review docs
     *
     * Returns the definition of description attribute of a resource object.
     *
     * Returns null if there is no description attribute.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of secondary identifier attributes
     * @throws IllegalStateException
     *             if there is more than one description attribute. But this
     *             should never happen.
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    ResourceAttributeDefinition<?> getDescriptionAttribute();

    /**
     * TODO review docs
     *
     * Specifies which resource attribute should be used as a "technical" name
     * for the account. This name will appear in log files and other troubleshooting
     * tools. The name should be a form of unique identifier that can be used to
     * locate the resource object for diagnostics. It should not contain white chars and
     * special chars if that can be avoided and it should be reasonable short.

     * It is different from a display name attribute. Display name is intended for a
     * common user or non-technical administrator (such as role administrator). The
     * naming attribute is intended for technical IDM administrators and developers.
     *
     * @return resource attribute definition that should be used as a "technical" name
     *                     for the account.
     */
    ResourceAttributeDefinition<?> getNamingAttribute();

    /**
     * TODO review docs
     *
     * Returns the native object class string for the resource object.
     *
     * Native object class is the name of the Resource Object Definition (Object
     * Class) as it is seen by the resource itself. The name of the Resource
     * Object Definition may be constrained by XSD or other syntax and therefore
     * may be "mangled" to conform to such syntax. The <i>native object
     * class</i> value will contain un-mangled name (if available).
     *
     * Returns null if there is no native object class.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return native object class
     * @throws IllegalStateException
     *             if there is more than one description attribute.
     */
    String getNativeObjectClass();

    /**
     * TODO review docs
     *
     * Indicates whether definition is should be used as default account type.
     *
     * If true value is returned then the definition should be used as a default
     * account type definition. This is a way how a resource connector may
     * suggest applicable object classes (resource object definitions) for
     * accounts.
     *
     * If no information about account type is present, false should be
     * returned. This method must return true only if isAccountType() returns
     * true.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of at-most-one value should be done at the time of
     * schema parsing. The exception may not even be thrown at all if the
     * implementation is not able to determine duplicity.
     *
     * @return true if the definition should be used as account type.
     * @throws IllegalStateException
     *             if more than one default account is suggested in the schema.
     */
    boolean isDefaultAccountDefinition();

    /**
     * TODO review docs
     *
     * Returns the definition of display name attribute.
     *
     * Display name attribute specifies which resource attribute should be used
     * as title when displaying objects of a specific resource object class. It
     * must point to an attribute of String type. If not present, primary
     * identifier should be used instead (but this method does not handle this
     * default behavior).
     *
     * Returns null if there is no display name attribute.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return native object class
     * @throws IllegalStateException
     *             if there is more than one display name attribute or the
     *             definition of the referenced attribute does not exist.
     */
    ResourceAttributeDefinition<?> getDisplayNameAttribute();

    @NotNull
    ResourceAttributeContainer instantiate();

    @NotNull
    ResourceAttributeContainer instantiate(QName name);

    @NotNull
    ResourceAttributeContainerDefinition clone();

    <T> ResourceAttributeDefinition<T> findAttributeDefinition(QName elementQName, boolean caseInsensitive);

    ResourceAttributeDefinition<?> findAttributeDefinition(ItemPath elementPath);

    ResourceAttributeDefinition<?> findAttributeDefinition(String localName);

    List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions();

    // Only attribute definitions should be here.
    @Override
    @NotNull
    List<? extends ResourceAttributeDefinition<?>> getDefinitions();

    @NotNull <T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition();
}

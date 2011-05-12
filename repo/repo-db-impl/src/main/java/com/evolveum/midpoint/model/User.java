/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * End user entity.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Entity
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = User.DDL_TABLE_USER,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")},
uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@NamedQueries(value = {
    @NamedQuery(name = User.QUERY_USER_FIND_ALL, query = "SELECT m FROM User m"),
    @NamedQuery(name = User.QUERY_USER_FIND_BY_NAME, query = "SELECT m FROM User m WHERE m.name = ?0")
})
public class User extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_USER = "Users";
    // queries
    public final static String QUERY_USER_FIND_BY_NAME = "User.findByUname";
    public final static String QUERY_USER_FIND_ALL = "User.findAll";
    /**
     *
     */
    private static final long serialVersionUID = -6219139356897428716L;
    private String givenName;
    private String familyName;
    private String fullName;
    private List<String> additionalNames;
    private String honorificPrefix;
    private String honorificSuffix;
    private List<String> eMailAddress;
    private List<String> telephoneNumber;
    private String employeeNumber;
    private List<String> employeeType;
    private List<String> organizationalUnit;
    private String locality;
    private Set<Account> accounts = new HashSet<Account>(0);
    /**
     * The prime value for hash code calculating.
     */
    private static final int PRIME = 31;

    /**
     * The username.
     */
//    private PersonInfo _info = new PersonInfo();
    // private String dataXml;
//    private Map<String, String> _attributes = new HashMap<String, String>(0);
    /**
     * Construct a default user.
     */
    public User() {
    }

    /**
     * Construct a user of the specified domain and usename.
     *
     * @param domain
     *            the domain
     * @param username
     *            the username
     */
    public User(final Domain domain, final String username) {
        //this.domain = domain;
        this.name = username;
    }

    /**
     * Get the username.
     *
     * @return the username
     */
    @Column(table = User.DDL_TABLE_USER, name = "name", unique = true, nullable = false, length = 128)
    public String getName() {
        return name;
    }

    @Column(table = User.DDL_TABLE_USER, name = "familyName", length = 128)
    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @Column(table = User.DDL_TABLE_USER, name = "fullName", length = 128)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Column(table = User.DDL_TABLE_USER, name = "givenName", length = 128)
    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

//    @Column(table = User.DDL_TABLE_USER, name = "email", length = 128)
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", cascade = CascadeType.REMOVE)
    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @org.hibernate.annotations.IndexColumn(name = "index_position", base = 1)
    public List<String> getAdditionalNames() {
        return additionalNames;
    }

    public void setAdditionalNames(List<String> additionalNames) {
        this.additionalNames = additionalNames;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @org.hibernate.annotations.IndexColumn(name = "index_position", base = 1)
    public List<String> getEMailAddress() {
        return eMailAddress;
    }

    public void setEMailAddress(List<String> eMailAddress) {
        this.eMailAddress = eMailAddress;
    }

    @Column(table = User.DDL_TABLE_USER, name = "employeeNumber", length = 128)
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @org.hibernate.annotations.IndexColumn(name = "index_position", base = 1)
    public List<String> getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(List<String> employeeType) {
        this.employeeType = employeeType;
    }

    @Column(table = User.DDL_TABLE_USER, name = "honorificPrefix", length = 128)
    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    @Column(table = User.DDL_TABLE_USER, name = "honorificSuffix", length = 128)
    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

    @Column(table = User.DDL_TABLE_USER, name = "locality", length = 128)
    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @org.hibernate.annotations.IndexColumn(name = "index_position", base = 1)
    public List<String> getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(List<String> organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @org.hibernate.annotations.IndexColumn(name = "index_position", base = 1)
    public List<String> getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(List<String> telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if ((this.givenName == null) ? (other.givenName != null) : !this.givenName.equals(other.givenName)) {
            return false;
        }
        if ((this.familyName == null) ? (other.familyName != null) : !this.familyName.equals(other.familyName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.givenName != null ? this.givenName.hashCode() : 0);
        hash = 89 * hash + (this.familyName != null ? this.familyName.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        StringBuffer builder = new StringBuffer();
        builder.append("User [");
        builder.append("accounts.size=");
        builder.append(accounts.size());
        builder.append(", additionalNames=");
        builder.append(additionalNames);
        builder.append(", eMailAddress=");
        builder.append(eMailAddress);
        builder.append(", employeeNumber=");
        builder.append(employeeNumber);
        builder.append(", employeeType=");
        builder.append(employeeType);
        builder.append(", familyName=");
        builder.append(familyName);
        builder.append(", fullName=");
        builder.append(fullName);
        builder.append(", givenName=");
        builder.append(givenName);
        builder.append(", honorificPrefix=");
        builder.append(honorificPrefix);
        builder.append(", honorificSuffix=");
        builder.append(honorificSuffix);
        builder.append(", locality=");
        builder.append(locality);
        builder.append(", organizationalUnit=");
        builder.append(organizationalUnit);
        builder.append(", telephoneNumber=");
        builder.append(telephoneNumber);
        builder.append("]");
        return builder.toString();
    }
}

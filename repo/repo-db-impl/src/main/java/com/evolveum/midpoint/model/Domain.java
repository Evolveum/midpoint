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

import java.net.URL;

import java.util.regex.Pattern;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * Virtual domain model.
 * <p>
 * The following are the types. The parameter "contextPath" depends on your
 * webapp, usually we installed in the ROOT directory, the contextPath is "".
 * </p>
 * <ol>
 * <li>type = 1:
 * <table border="1">
 * <tr>
 * <td>server base url:</td>
 * <td>
 * <code>
 * http(s)://[&lt;serverHost&gt;.]&lt;your-domain&gt;[:&lt;port&gt;]contextPath/
 * </code>
 * </td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>
 * http(s)://&lt;resourcename&gt;.&lt;your-domain&gt;[:&lt;port&gt;]contextPath/[&lt;memberPath&gt;/]
 * </code>
 * </td>
 * </tr>
 * </table>
 * Examples:
 * <ul>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td><code>http(s)://resourcename.example.com/</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td><code>www</code></td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://www.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td><code>http(s)://resourcename.example.com/</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td>null or empty</td>
 * <tr>
 * <td>memberPath:</td>
 * <td><code>member</code></td>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://resourcename.example.com/member/</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td><code>www</code></td>
 * <tr>
 * <td>memberPath:</td>
 * <td><code>member</code></td>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://www.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://resourcename.example.com/member/</code></td>
 * </tr>
 * </table>
 * </li>
 * </ul>
 * </li>
 * <li>type = 2:
 * <table border="1">
 * <tr>
 * <td>server base url:</td>
 * <td>
 * <code>
 * http(s)://[&lt;serverHost&gt;.]&lt;your-domain&gt;[:&lt;port&gt;]contextPath/
 * </code>
 * </td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>
 * http(s)://&lt;your-domain&gt;[:&lt;port&gt;]contextPath/[&lt;memberPath&gt;/]&lt;resourcename&gt;
 * </code>
 * </td>
 * </tr>
 * </table>
 * Examples:
 * <ul>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>openid.example.com</code></td>
 * </tr>
 * <tr>
 * <td>sercerHost:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://openid.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://openid.example.com/resourcename</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>openid.example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td><code>www</code></td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://www.openid.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://openid.example.com/resourcename</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>openid.example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td>null or empty</td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td><code>member</code></td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://openid.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://openid.example.com/member/resourcename</code></td>
 * </tr>
 * </table>
 * </li>
 * <li>
 * <table border="1">
 * <tr>
 * <td>your-domain:</td>
 * <td><code>openid.example.com</code></td>
 * </tr>
 * <tr>
 * <td>serverHost:</td>
 * <td><code>www</code></td>
 * </tr>
 * <tr>
 * <td>memberPath:</td>
 * <td><code>member</code></td>
 * </tr>
 * <tr>
 * <td>server base url:</td>
 * <td><code>http(s)://www.openid.example.com/</code></td>
 * </tr>
 * <tr>
 * <td>identifier:</td>
 * <td>
 * <code>http(s)://openid.example.com/member/resourcename</code></td>
 * </tr>
 * </table>
 * </li>
 * </ul>
 * </li>
 * </ol>
 * 
 * @author $author$
 */
//@Entity
//@Table(name = "Domains", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = Domain.DDL_TABLE_DOMAIN,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")},
uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@NamedQueries({
    @NamedQuery(name = "Domain.findAll", query = "SELECT p FROM Domain p")
})
public class Domain extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_DOMAIN = "Domains";
    /**
     *
     */
    private static final long serialVersionUID = 7341781651134648946L;
    /**
     * The prime value for {@code #hashCode()}.
     */
    private static final int PRIME = 31;
    /**
     * The regular expression of the resourcename.
     */
    private String regex;
    /**
     * The reserved regular expression of the resourcename.
     */
    private String reservedRegex;
    /**
     * The unallowedable regular expression of the resourcename.
     */
    private String unallowableRegex;
    /**
     * The compiled pattern.
     */
    private Pattern pattern;
    /**
     * The compiled reserved pattern.
     */
    private Pattern reservedPattern;
    /**
     * The compiled unallowable pattern.
     */
    private Pattern unallowablePattern;
    /**
     * resourcename configuration.
     */
//    private NameConfiguration resourcenameConfiguration =
//            new NameConfiguration();
    /**
     * Extended configuration.
     */
    //private Map<String, String> configuration;
    /**
     * Domain runtime inforation.
     */
    private DomainRuntime runtime = new DomainRuntime();

    public Domain() {
        String re = "[a-z]{1,16}";
        this.setRegex(re);
        re = "root|toor|wheel|staff|admin|administrator";
        this.setReservedRegex(re);
        re = "w+|home|server|approve.*|approving|register|login|logout|email.*" + "|password.*|persona.*|site.*|attribute.*|hl|member|news|jos" + "|mail|smtp|pop3|pop|.*fuck.*";
        this.setUnallowableRegex(re);
    }

    /**
     * @return the name
     */
    @Column(name = "name", unique = true, nullable = false, length = 128)
    public String getName() {
        return name;
    }

//    /**
//     * @return the resourcenameConfiguration
//     */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "nameconf_uuid", nullable = false)
//    public NameConfiguration getNameConfiguration() {
//        return resourcenameConfiguration;
//    }
//
//    /**
//     * @param resourcenameConfiguration
//     *            the resourcenameConfiguration to set
//     */
//    public void setNameConfiguration(
//            final NameConfiguration resourcenameConfiguration) {
//        this.resourcenameConfiguration = resourcenameConfiguration;
//        resourcenameConfiguration.getDomain().add(this);
//    }
    /**
     * @return the runtime
     */
    @Transient
    public DomainRuntime getRuntime() {
        return runtime;
    }

    /**
     * @param runtime
     *            the runtime to set
     */
    public void setRuntime(final DomainRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the identifierPrefix
     */
    @Transient
    public String getIdentifierPrefix() {
        StringBuilder sb = new StringBuilder();
        URL baseUrl = this.getRuntime().getServerBaseUrl();


        sb.append(baseUrl.getProtocol()).append("://");
        sb.append(getName());
        sb.append(baseUrl.getPath());

        return sb.toString();
    }

    /**
     * Get the regular expression of the resourcename.
     *
     * @return the regular expression of the resourcename
     */
    @Column(name = "regex", nullable = false, length = 500)
    public String getRegex() {
        return regex;
    }

    /**
     * Set the regular expression of the resourcename.
     *
     * @param regex
     *            the regular expression of the resourcename
     */
    public void setRegex(final String regex) {
        this.regex = regex;
        this.pattern = regex != null ? Pattern.compile(regex) : null;
    }

    /**
     * Get the regular expression of the reserved resourcename.
     *
     * @return the regular expression of the reserved resourcename
     */
    @Column(name = "reserved_regex", nullable = false, length = 500)
    public String getReservedRegex() {
        return reservedRegex;
    }

    /**
     * Set the regular expression of the reserved resourcename.
     *
     * @param reservedRegex
     *            the regular expression of the reserved resourcename
     */
    public void setReservedRegex(final String reservedRegex) {
        this.reservedRegex = reservedRegex;
        this.reservedPattern = reservedRegex != null ? Pattern.compile(
                reservedRegex, Pattern.CASE_INSENSITIVE) : null;
    }

    /**
     * Get the regular expression of the unallowable resourcename.
     *
     * @return the regular expression of the unabllowable resourcename
     */
    @Column(name = "unallowable_regex", nullable = false, length = 500)
    public String getUnallowableRegex() {
        return unallowableRegex;
    }

    /**
     * Set the regular expression of the unallowable resourcename.
     *
     * @param unallowableRegex
     *            the regular expression of the unallowable resourcename
     */
    public void setUnallowableRegex(final String unallowableRegex) {
        this.unallowableRegex = unallowableRegex;
        this.unallowablePattern = unallowableRegex != null ? Pattern.compile(
                unallowableRegex, Pattern.CASE_INSENSITIVE) : null;
    }

    /**
     * Get the compiled pattern of the resourcename.
     *
     * @return the compiled pattern
     */
    @Transient
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Get the compiled pattern of the reserved resourcename.
     *
     * @return the compiled pattern of the reserved resourcename
     */
    @Transient
    public Pattern getReservedPattern() {
        return reservedPattern;
    }

    /**
     * Get the compiled pattern of the unallowable resourcename.
     *
     * @return the compiled pattern of the unallowable resourcename
     */
    @Transient
    public Pattern getUnallowablePattern() {
        return unallowablePattern;
    }

    /**
     * Determines if the resourcename is permissible as the resourcename in this domain
     * resourcename configuration.
     *
     * @param resourcename
     *            the resourcename to check
     * @return true if the resourcename is permissible as the resourcename in this
     *         domain resourcename configuration.
     */
    public boolean isResourcename(final String resourcename) {
        return this.getPattern() != null ? this.getPattern().matcher(resourcename).matches() : false;
    }

    /**
     * Determines if the resourcename is reserved.
     *
     * @param resourcename
     *            the resourcename
     * @return true if reserved, otherwise false.
     */
    public boolean isReserved(final String resourcename) {
        return this.getReservedPattern() != null ? this.getReservedPattern().matcher(resourcename).matches() : false;
    }

    /**
     * Determines if the resourcename is unallowable.
     *
     * @param resourcename
     *            the resourcename
     * @return true if unallowable, otherwise false.
     */
    public boolean isUnallowable(final String resourcename) {
        return this.getUnallowablePattern() != null ? this.getUnallowablePattern().matcher(resourcename).matches() : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = PRIME * result + ((getName() == null) ? 0 : getName().hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Domain)) {
            return false;
        }
        Domain other = (Domain) obj;
        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }
}

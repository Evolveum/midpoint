
package com.evolveum.prism.xml.ns._public.query_3;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.evolveum.prism.xml.ns._public.query_3 package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Present_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "present");
    private final static QName _Not_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "not");
    private final static QName _Or_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "or");
    private final static QName _Substring_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "substring");
    private final static QName _Ref_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "ref");
    private final static QName _True_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "true");
    private final static QName _MinDepth_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "minDepth");
    private final static QName _Org_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "org");
    private final static QName _Query_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "query");
    private final static QName _GreaterOrEqual_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "greaterOrEqual");
    private final static QName _Filter_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "filter");
    private final static QName _Matching_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "matching");
    private final static QName _And_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "and");
    private final static QName _LessOrEqual_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "lessOrEqual");
    private final static QName _Value_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "value");
    private final static QName _Type_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "type");
    private final static QName _MaxDepth_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "maxDepth");
    private final static QName _Equal_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "equal");
    private final static QName _OrgRef_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "orgRef");
    private final static QName _Path_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/query-3", "path");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.evolveum.prism.xml.ns._public.query_3
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link NAryLogicalOperatorFilterType }
     *
     */
    public NAryLogicalOperatorFilterType createNAryLogicalOperatorFilterType() {
        return new NAryLogicalOperatorFilterType();
    }

    /**
     * Create an instance of {@link FilterClauseType }
     *
     */
    public FilterClauseType createFilterType() {
        return new FilterClauseType();
    }

    /**
     * Create an instance of {@link QueryType }
     *
     */
    public QueryType createQueryType() {
        return new QueryType();
    }

    /**
     * Create an instance of {@link PropertyNoValueFilterType }
     *
     */
    public PropertyNoValueFilterType createPropertyNoValueFilterType() {
        return new PropertyNoValueFilterType();
    }

    /**
     * Create an instance of {@link PropertyComplexValueFilterType }
     *
     */
    public PropertyComplexValueFilterType createPropertyComplexValueFilterType() {
        return new PropertyComplexValueFilterType();
    }

    /**
     * Create an instance of {@link PropertySimpleValueFilterType }
     *
     */
    public PropertySimpleValueFilterType createPropertySimpleValueFilterType() {
        return new PropertySimpleValueFilterType();
    }

    /**
     * Create an instance of {@link UriFilterType }
     *
     */
    public UriFilterType createUriFilterType() {
        return new UriFilterType();
    }

    /**
     * Create an instance of {@link UnaryLogicalOperatorFilterType }
     *
     */
    public UnaryLogicalOperatorFilterType createUnaryLogicalOperatorFilterType() {
        return new UnaryLogicalOperatorFilterType();
    }

    /**
     * Create an instance of {@link ValueType }
     *
     */
    public ValueType createValueType() {
        return new ValueType();
    }

    /**
     * Create an instance of {@link PagingType }
     *
     */
    public PagingType createPagingType() {
        return new PagingType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertyNoValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "present", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertyNoValueFilterType> createPresent(PropertyNoValueFilterType value) {
        return new JAXBElement<>(_Present_QNAME, PropertyNoValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnaryLogicalOperatorFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "not", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<UnaryLogicalOperatorFilterType> createNot(UnaryLogicalOperatorFilterType value) {
        return new JAXBElement<>(_Not_QNAME, UnaryLogicalOperatorFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NAryLogicalOperatorFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "or", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<NAryLogicalOperatorFilterType> createOr(NAryLogicalOperatorFilterType value) {
        return new JAXBElement<>(_Or_QNAME, NAryLogicalOperatorFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "substring", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createSubstring(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_Substring_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "ref", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createRef(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_Ref_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertyNoValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "true", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertyNoValueFilterType> createTrue(PropertyNoValueFilterType value) {
        return new JAXBElement<>(_True_QNAME, PropertyNoValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "minDepth", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createMinDepth(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_MinDepth_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertyComplexValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "org", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertyComplexValueFilterType> createOrg(PropertyComplexValueFilterType value) {
        return new JAXBElement<>(_Org_QNAME, PropertyComplexValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "query")
    public JAXBElement<QueryType> createQuery(QueryType value) {
        return new JAXBElement<>(_Query_QNAME, QueryType.class, null, value);
    }

    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "filter")
    public JAXBElement<SearchFilterType> createFilter(SearchFilterType value) {
        return new JAXBElement<>(_Filter_QNAME, SearchFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "greaterOrEqual", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createGreaterOrEqual(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_GreaterOrEqual_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FilterClauseType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "filterClause")
    public JAXBElement<FilterClauseType> createFilterClause(FilterClauseType value) {
        return new JAXBElement<>(_Filter_QNAME, FilterClauseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "matching")
    public JAXBElement<String> createMatching(String value) {
        return new JAXBElement<>(_Matching_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NAryLogicalOperatorFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "and", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<NAryLogicalOperatorFilterType> createAnd(NAryLogicalOperatorFilterType value) {
        return new JAXBElement<>(_And_QNAME, NAryLogicalOperatorFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "lessOrEqual", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createLessOrEqual(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_LessOrEqual_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ValueType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "value")
    public JAXBElement<ValueType> createValue(ValueType value) {
        return new JAXBElement<>(_Value_QNAME, ValueType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UriFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "type", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<UriFilterType> createType(UriFilterType value) {
        return new JAXBElement<>(_Type_QNAME, UriFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "maxDepth", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createMaxDepth(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_MaxDepth_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertyComplexValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "equal", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertyComplexValueFilterType> createEqual(PropertyComplexValueFilterType value) {
        return new JAXBElement<>(_Equal_QNAME, PropertyComplexValueFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PropertySimpleValueFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "orgRef", substitutionHeadNamespace = "http://prism.evolveum.com/xml/ns/public/query-3", substitutionHeadName = "filterClause")
    public JAXBElement<PropertySimpleValueFilterType> createOrgRef(PropertySimpleValueFilterType value) {
        return new JAXBElement<>(_OrgRef_QNAME, PropertySimpleValueFilterType.class, null, value);
    }

//    /**
//     * Create an instance of {@link JAXBElement }{@code <}{@link XPathType }{@code >}}
//     *
//     */
//    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/query-3", name = "path")
//    public JAXBElement<XPathType> createPath(XPathType value) {
//        return new JAXBElement<XPathType>(_Path_QNAME, XPathType.class, null, value);
//    }

}

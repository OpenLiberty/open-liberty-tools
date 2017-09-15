/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.jaxws.web.webservicecontext.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the wasdev.sample.jaxws.web.webservicecontext.client package. 
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

    private final static QName _Query_QNAME = new QName("http://webservicecontext.web.jaxws.sample.wasdev/", "query");
    private final static QName _QueryResponse_QNAME = new QName("http://webservicecontext.web.jaxws.sample.wasdev/", "queryResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: wasdev.sample.jaxws.web.webservicecontext.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link QueryResponse }
     * 
     */
    public QueryResponse createQueryResponse() {
        return new QueryResponse();
    }

    /**
     * Create an instance of {@link Query }
     * 
     */
    public Query createQuery() {
        return new Query();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Query }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservicecontext.web.jaxws.sample.wasdev/", name = "query")
    public JAXBElement<Query> createQuery(Query value) {
        return new JAXBElement<Query>(_Query_QNAME, Query.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservicecontext.web.jaxws.sample.wasdev/", name = "queryResponse")
    public JAXBElement<QueryResponse> createQueryResponse(QueryResponse value) {
        return new JAXBElement<QueryResponse>(_QueryResponse_QNAME, QueryResponse.class, null, value);
    }

}

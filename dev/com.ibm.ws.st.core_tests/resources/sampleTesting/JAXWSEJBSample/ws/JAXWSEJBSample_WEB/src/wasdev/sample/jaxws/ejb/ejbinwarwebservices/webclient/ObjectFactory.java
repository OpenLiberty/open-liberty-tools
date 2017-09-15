/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SayHelloResponse_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "sayHelloResponse");
    private final static QName _SayHello_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "sayHello");
    private final static QName _InvokeOther_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "invokeOther");
    private final static QName _InvokeOtherResponse_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "invokeOtherResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient
     * 
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link SayHello }
     * 
     */
    public SayHello createSayHello() {
        return new SayHello();
    }

    /**
     * Create an instance of {@link SayHelloResponse }
     * 
     */
    public SayHelloResponse createSayHelloResponse() {
        return new SayHelloResponse();
    }

    /**
     * Create an instance of {@link InvokeOther }
     * 
     */
    public InvokeOther createInvokeOther() {
        return new InvokeOther();
    }

    /**
     * Create an instance of {@link InvokeOtherResponse }
     * 
     */
    public InvokeOtherResponse createInvokeOtherResponse() {
        return new InvokeOtherResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}
     * 
     */
    @XmlElementDecl(namespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello }{@code >}
     * 
     */
    @XmlElementDecl(namespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", name = "sayHello")
    public JAXBElement<SayHello> createSayHello(SayHello value) {
        return new JAXBElement<SayHello>(_SayHello_QNAME, SayHello.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeOther }{@code >}
     * 
     */
    @XmlElementDecl(namespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", name = "invokeOther")
    public JAXBElement<InvokeOther> createInvokeOther(InvokeOther value) {
        return new JAXBElement<InvokeOther>(_InvokeOther_QNAME, InvokeOther.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeOtherResponse }{@code >}
     * 
     */
    @XmlElementDecl(namespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", name = "invokeOtherResponse")
    public JAXBElement<InvokeOtherResponse> createInvokeOtherResponse(InvokeOtherResponse value) {
        return new JAXBElement<InvokeOtherResponse>(_InvokeOtherResponse_QNAME, InvokeOtherResponse.class, null, value);
    }

}

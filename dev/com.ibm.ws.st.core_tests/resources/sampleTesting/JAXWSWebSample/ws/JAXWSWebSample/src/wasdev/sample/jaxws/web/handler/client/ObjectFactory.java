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

package wasdev.sample.jaxws.web.handler.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the wasdev.sample.jaxws.web.handler.client package. 
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

    private final static QName _Track_QNAME = new QName("http://web.jaxws.sample.wasdev/", "track");
    private final static QName _TrackResponse_QNAME = new QName("http://web.jaxws.sample.wasdev/", "trackResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: wasdev.sample.jaxws.web.handler.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Track }
     * 
     */
    public Track createTrack() {
        return new Track();
    }

    /**
     * Create an instance of {@link TrackResponse }
     * 
     */
    public TrackResponse createTrackResponse() {
        return new TrackResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Track }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://web.jaxws.sample.wasdev/", name = "track")
    public JAXBElement<Track> createTrack(Track value) {
        return new JAXBElement<Track>(_Track_QNAME, Track.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TrackResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://web.jaxws.sample.wasdev/", name = "trackResponse")
    public JAXBElement<TrackResponse> createTrackResponse(TrackResponse value) {
        return new JAXBElement<TrackResponse>(_TrackResponse_QNAME, TrackResponse.class, null, value);
    }

}

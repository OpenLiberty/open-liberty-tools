/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wasdev.sample.jaxws.ejb.webservicesxml;

import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;

/**
 * @author forrestxm
 * 
 */
public class ToUpperCase implements SOAPHandler<SOAPMessageContext> {

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        try {
            final SOAPMessage message = context.getMessage();
            final SOAPBody body = message.getSOAPBody();
            final String localName = body.getFirstChild().getLocalName();

            if ("countdownfrommeResponse".equals(localName)) {
                final Node responseNode = body.getFirstChild();
                final Node returnNode = responseNode.getFirstChild();
                final Node returnValue = returnNode.getFirstChild();

                returnValue.setNodeValue(returnValue.getNodeValue()
                                .toUpperCase());

            }
            return true;
        } catch (SOAPException e) {
            System.out.println(e);
            return false;
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close(MessageContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<QName> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

}

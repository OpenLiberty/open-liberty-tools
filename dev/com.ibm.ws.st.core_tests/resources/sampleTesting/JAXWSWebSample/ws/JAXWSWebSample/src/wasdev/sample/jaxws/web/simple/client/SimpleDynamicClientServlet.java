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

package wasdev.sample.jaxws.web.simple.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;

import wasdev.sample.jaxws.web.BaseClientServlet;

@WebServlet("/SimpleDynamicClient")
public class SimpleDynamicClientServlet extends BaseClientServlet {

    private static final long serialVersionUID = -6484178039946518128L;

    private static final String REQUEST_MESSAGE = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echo xmlns:ns2=\"http://simpleEchoProvider.web.jaxws.sample.wasdev/\"><arg0>${placeHolder}</arg0></ns2:echo></soap:Body></soap:Envelope>";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String echoParameter = request.getParameter("echoParameter");
        if (echoParameter == null) {
            return;
        }

        //Build request SOAP message        
        String requestMessageValue = REQUEST_MESSAGE.replace("${placeHolder}", echoParameter);
        request.setAttribute("requestMessage", requestMessageValue);
        Source requestMessageSource = new StreamSource(new StringReader(requestMessageValue));
        SOAPMessage requestMessage = null;
        try {
            MessageFactory factory = MessageFactory.newInstance();
            requestMessage = factory.createMessage();
            requestMessage.getSOAPPart().setContent(requestMessageSource);
            requestMessage.saveChanges();
        } catch (SOAPException e) {
            throw new ServletException(e);
        }

        //Create Dispatch based client
        Service service = Service.create(new QName("http://abc", "abc"));
        service.addPort(new QName("http://abc", "anyPort"), SOAPBinding.SOAP11HTTP_BINDING, getRequestBaseURL(request)
                + "/SimpleEchoProviderService");
        Dispatch<SOAPMessage> dispatch = service.createDispatch(new QName("http://abc", "anyPort"), SOAPMessage.class,
                Mode.MESSAGE);

        //Invoke with Dispatch 
        if (requestMessage != null) {
            SOAPMessage responseMessage = dispatch.invoke(requestMessage);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                responseMessage.writeTo(ps);
                request.setAttribute("responseMessage", baos.toString());
            } catch (SOAPException e) {
                throw new ServletException(e);
            }
        }

        request.getRequestDispatcher("simple_webserviceprovider_sample.jsp").forward(request, response);
    }
}

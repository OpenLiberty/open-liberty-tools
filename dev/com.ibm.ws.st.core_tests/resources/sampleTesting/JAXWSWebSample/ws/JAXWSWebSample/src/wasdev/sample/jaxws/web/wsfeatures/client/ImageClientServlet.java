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

package wasdev.sample.jaxws.web.wsfeatures.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.MTOM;

import wasdev.sample.jaxws.web.BaseClientServlet;
import wasdev.sample.jaxws.web.XMLUtils;

@WebServlet("/ImageClientServlet")
public class ImageClientServlet extends BaseClientServlet {

    private static final long serialVersionUID = -7603240804080798249L;

    private static final byte[] MOCK_IMAGE_BYTES = { 0, 1, 2, 3 };

    @MTOM
    @WebServiceRef(value = ImageServiceImplService.class)
    private ImageServiceImpl mtomEnabledImageService;

    @WebServiceRef(value = ImageServiceImplService.class)
    private ImageServiceImpl mtomDisabledImageService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String uploadId = request.getParameter("uploadId");
        if (uploadId != null) {
            if (request.getParameter("uploadMTOM") != null) {
                setEndpointAddress((BindingProvider) mtomEnabledImageService, request, "ImageServiceImplService");
                request.setAttribute("requestMessage",
                        XMLUtils.escapeXML(new String(mtomEnabledImageService.uploadImage(uploadId, MOCK_IMAGE_BYTES))));
            } else if (request.getParameter("uploadNonMTOM") != null) {
                setEndpointAddress((BindingProvider) mtomDisabledImageService, request, "ImageServiceImplService");
                request.setAttribute("requestMessage", XMLUtils.escapeXML(new String(mtomDisabledImageService
                        .uploadImage(uploadId, MOCK_IMAGE_BYTES))));
            }
        }
        request.getRequestDispatcher("wsfeatures_sample.jsp").forward(request, response);
    }
}

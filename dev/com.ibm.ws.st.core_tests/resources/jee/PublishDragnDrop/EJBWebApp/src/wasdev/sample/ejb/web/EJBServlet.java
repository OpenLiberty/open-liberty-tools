/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.ejb.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shared.SampleHello;

/**
 * A servlet which injects a stateless EJB
 */
@WebServlet({"/EJBServlet"})
public class EJBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String JNDI_LOCAL_STANDALONE = "java:global/EJBSampleApp/SampleStatelessBean!shared.SampleHello";
    
    @EJB(lookup = JNDI_LOCAL_STANDALONE)
    SampleHello statelessBean;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        // Call hello method on a stateless session bean
        String message = statelessBean.hello();

        writer.println(message);
    }
}

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


package wasdev.sample.ejb.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import wasdev.sample.ejb.SampleStatelessBean;

/**
 * A servlet which injects a stateless EJB
 */
@WebServlet({"/", "/sampleServlet"})
public class EJBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    SampleStatelessBean statelessBean;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        // Call hello method on a stateless session bean
        String message = statelessBean.hello();

        writer.println(message);
    }
}

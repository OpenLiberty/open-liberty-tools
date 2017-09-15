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

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

public class TestClientLogicalHandler implements LogicalHandler<LogicalMessageContext> {

    @Resource(name = "arg0")
    private String testArg0;

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ": init param \"arg0\" = " + testArg0);
        System.out.println(this.getClass().getName() + ": postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ": PreDestroy is invoked");
    }

    @Override
    public boolean handleMessage(LogicalMessageContext context) {
        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOut) {
            System.out.println(getClass().getName() + "(outbound)");
        } else {
            System.out.println(getClass().getName() + "(inbound)");
        }
        return true;
    }

    @Override
    public boolean handleFault(LogicalMessageContext context) {
        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOut) {
            System.out.println(getClass().getName() + "(fault/outbound)");
        } else {
            System.out.println(getClass().getName() + "(fault/inbound)");
        }
        return true;
    }

    @Override
    public void close(MessageContext context) {
        System.out.println(this.getClass().getName() + " is closed");
    }
}

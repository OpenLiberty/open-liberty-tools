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
package wasdev.sample.jaxws.ejb.ejbinwarwebservices;

import javax.jws.WebService;

@WebService(serviceName = "SayHelloPOJOService",
            portName = "SayHelloPOJOPort")
public class SayHelloPojoBean implements SayHelloLocal {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloLocal#invokeOther()
     */
    @Override
    public String invokeOther() {
        return sayHello("Anonym");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloLocal#sayHello(java.lang.String)
     */
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + getClass().getSimpleName();
    }

}

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


package wasdev.sample.jaxws.web.catalog;

import javax.jws.WebService;

@WebService(serviceName = "Calculator", portName = "CalculatorPort", endpointInterface = "wasdev.sample.jaxws.web.catalog.Calculator",
            targetNamespace = "http://catalog.web.jaxws.sample.wasdev", wsdlLocation = "WEB-INF/wsdl/calculator.wsdl")
public class CalculatorService {

    /**
     * @param value1
     * @param value2
     * @return returns int
     */
    public int add(int value1, int value2) {
        return value1 + value2;
    }
}

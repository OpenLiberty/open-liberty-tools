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

package wasdev.sample.cdi;

import javax.enterprise.context.RequestScoped;

/**
 * <p>
 * Annotating this class with <code>javax.enterprise.context.RequestScoped</code> 
 * specifies that the container should manage this bean's life cycle along with the request. 
 * See the Javadoc for <code>javax.enterprise.context.RequestScoped</code> for more information. 
 * </p>
 */
@RequestScoped
public class InjectedBean {
	
    /**
     * @return a customized message for this bean
     */
    public String getValue() {
        return "Congratulations! You successfully used CDI to inject a bean at the request scope- Updated!";
    }
    
}

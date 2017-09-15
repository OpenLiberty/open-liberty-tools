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
import javax.inject.Inject;
import javax.inject.Named;

/**
 * <p>
 * Annotating this class with the special qualifier <code>javax.inject.Named</code> 
 * allows you to look up instances with Unified EL expressions, for example, in 
 * a JSP like <code>sample.jsp</code>.
 * </p>
 * <p>
 * Annotating this class with <code>javax.enterprise.context.RequestScoped</code> 
 * specifies that the container should manage this bean's life cycle along with the request. 
 * See the Javadoc for <code>javax.enterprise.context.RequestScoped</code> for more information. 
 * </p>
 */
@RequestScoped
@Named
public class NamedBean {

    /**
     * The container will inject an instance of this bean at the appropriate scope.
     */
    private @Inject
    InjectedBean bean;

    /**
     * @return a message indicating whether or not the container correctly injected the bean
     */
    public String getMessage() {
    	if(this.bean==null) {
    		return "Sorry, the bean was not injected.";
    	} else {
    		return this.bean.getValue();
    	}
    }

}

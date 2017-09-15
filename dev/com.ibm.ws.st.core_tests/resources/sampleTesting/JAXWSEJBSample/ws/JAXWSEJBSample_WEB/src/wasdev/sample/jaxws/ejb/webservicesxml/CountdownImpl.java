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

import javax.jws.WebService;
import javax.jws.HandlerChain;
import java.lang.StringBuffer;

@WebService
@HandlerChain(file = "handlers.xml")
public class CountdownImpl implements Countdown {

    private static String[] numbers = { "Ten", "Nine", "Eight", "Seven", "Six",
                                       "Five", "Four", "Three", "Two", "One" };

    @Override
    public String countdownfromme(int fromme) {
        if (fromme <= 0 || fromme > 10) {
            return "Oops! I don't know how to count down the number out of 1~10, " +
                   "please retry a number between 1 and 10 :)";
        } else {
            StringBuffer countstring = new StringBuffer();
            for (int i = fromme; i > 0; i--) {
                if (!numbers[10 - i].equalsIgnoreCase("one")) {
                    countstring.append(numbers[10 - i] + ", ");
                } else {
                    countstring.append(numbers[10 - i] + "! Done ");
                }
            }
            return countstring.toString();
        }
    }

}

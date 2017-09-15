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


package wasdev.sample.jms.mdb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

@MessageDriven
public class SampleMDB implements MessageListener {

	@Resource
	MessageDrivenContext ejbcontext;

	@SuppressWarnings("unused")
	@Resource
	private void setMessageDrivenContext(EJBContext ejbcontext) {

	}

	@PostConstruct
	public void postConstruct() {

	}

	@Override
	public void onMessage(Message message) {
		try {
			System.out.println("Updated Message Received in MDB !!!" + message);
			// send message to MDBREPLYQ
			System.out.println("Sending message to MDBREPLYQ");
			SendMDBResponse(message);
			System.out.println("Message sent to MDBREPLYQ");
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	public static void SendMDBResponse(Message msg) throws Exception {
		System.out
		.println("**************************************************************************");
		System.out.println("Test testQueueSendMessageMDB Started from MDB");

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi/MDBREPLYQ");
		System.out.println("QCF and Queue lookup completed");
		QueueConnection con = cf1.createQueueConnection();
		con.start();
		System.out.println("QueueConnection Created");
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		System.out.println("QueueSession Created");

		QueueSender send = sessionSender.createSender(queue);
		System.out.println("QueueSender Created");

		send.send(msg);
		System.out.println("Message sent to Queue MDBREPLYQ");

		if (con != null)
			con.close();
		System.out.println("Test testQueueSendMessageMDB Completed");

	}
}

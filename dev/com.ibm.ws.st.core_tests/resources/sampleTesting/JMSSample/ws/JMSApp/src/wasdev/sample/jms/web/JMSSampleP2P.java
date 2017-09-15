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


package wasdev.sample.jms.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class JMSSampleP2P
 */
@WebServlet("/JMSSampleP2P")
public class JMSSampleP2P extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public JMSSampleP2P() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String strAction = request.getParameter("ACTION");
		PrintWriter out = response.getWriter();
		try {


			if (strAction == null) {
				out.println("Please specify the Action");
				out.println("Example : http://<host>:<port>/JMSApp/JMSSampleP2P?ACTION=sendAndReceive");
			} else if (strAction.equalsIgnoreCase("sendAndReceive")) {
				// call the Send and Receive Message
				sendAndReceive(request, response);
			} else if (strAction.equalsIgnoreCase("sendMessage")) {
				// Send Message only
				sendMessage(request, response);
			} else if (strAction.equalsIgnoreCase("receiveAllMessages")) {
				// Receive All messages from queue
				receiveAllMessages(request, response);
			} else if (strAction
					.equalsIgnoreCase("receiveAllMessagesSelectors")) {
				// receive all the messages using message selector
				receiveAllMessagesSelectors(request, response);
			} else if (strAction.equalsIgnoreCase("mdbRequestResponse")) {
				// Send message to be processed by MDB and wait from MDB
				// response
				mdbRequestResponse(request, response);
			} else {
				out.println("Incorrect Action Specified, the valid actions are");
				out.println("ACTION=sendAndReceive");
				out.println("ACTION=sendMessage");
				out.println("ACTION=receiveAllMessages");
				out.println("ACTION=receiveAllMessagesSelectors");
				out.println("ACTION=mdbRequestResponse");
			}

		} catch (Exception e) {
			out.println("Something unexpected happened, check the logs or restart the server");
			e.printStackTrace();
		}

	}

	/**
	 * Scenario: Point to Point</br> Connects to ME using connection factory
	 * jndi_JMS_BASE_QCF </br> Sends one message to Queue defined in
	 * jndi_INPUT_Q </br> Receives the message and prints it on console </br>
	 * 
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws Exception
	 *             if an error occurs.
	 */

	public void sendAndReceive(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("SendAndReceive Started");

		/*
		 * Lookup Queue Connection Factory from JNDI
		 */
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		// Lookup Queue resource from JNDI
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi_INPUT_Q");
		QueueConnection con = cf1.createQueueConnection();

		// start the connection to receive message
		con.start();

		// create a queue session to send a message
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);
		out.println("Message sent successfully");
		// send a sample message
		send.send(sessionSender.createTextMessage("Liberty Sample Message"));

		// create a queue receiver object
		QueueReceiver rec = sessionSender.createReceiver(queue);

		// receive message from Queue
		TextMessage msg = (TextMessage) rec.receive();

		out.println("Received Message Successfully :" + msg);

		if (con != null)
			con.close();
		out.println("SendAndReceive Completed");
	}// end of SendAndReceive

	/**
	 * Scenario: Point to Point </br> Connects to ME using connection factory
	 * jndi_JMS_BASE_QCF defined in server.xml</br> Sends one message to Queue
	 * defined in jndi_INPUT_Q within server.xml</br>
	 * 
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws Exception
	 *             if an error occurs.
	 */

	public void sendMessage(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("SendMessage Started");

		// create a queue connection factory
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		// create a queue by performing jndi lookup
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi_INPUT_Q");

		// create a queue connection
		QueueConnection con = cf1.createQueueConnection();
		con.start();
		// create a queue sender
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);

		TextMessage msg = sessionSender.createTextMessage();
		msg.setStringProperty("COLOR", "BLUE");
		msg.setText("Liberty Sample Message");

		send.send(msg);
		out.println("Message sent successfuly");

		if (con != null)
			con.close();
		out.println("SendMessage Completed");

	}// end of SendMessage

	/**
	 * Scenario: Point to Point </br> Connects to ME using connection factory
	 * jndi_JMS_BASE_QCF defined in server.xml </br> Sends one message to the
	 * queue specified in jndi_INPUT_Q(i.e Queue Queue1) defined in server.xml
	 * </br> Receives all the messages from the above Queue and prints them on
	 * console </br>
	 * 
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws Exception
	 *             if an error occurs.
	 */

	public void receiveAllMessages(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("ReceiveAllMessages Started");
		// create queue connection factory
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		// create a queue by looking up from the JNDI repository
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi_INPUT_Q");

		// create a queue connection
		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession session = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver receive = session.createReceiver(queue);

		TextMessage msg = null;

		do {
			msg = (TextMessage) receive.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);

		if (con != null)
			con.close();

		out.println("ReceiveAllMessages Completed");

	} // end of ReceiveAllMessages

	/**
	 * Scenario: Point to Point</br> Connects to ME using connection factory
	 * jndi_JMS_BASE_QCF </br> Sends one message to Queue jndi_INPUT_Q(i.e Queue
	 * Queue1) </br> Receives the messages with selector COLOR='BLUE' and prints
	 * them on console </br>
	 * 
	 * @param request
	 *            HTTP request
	 * @param response
	 *            HTTP response
	 * @throws Exception
	 *             if an error occurs.
	 */

	public void receiveAllMessagesSelectors(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("ReceiveAllMessagesSelectors Started");

		// create a queue connection factory
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		// create a queue by looking up from the JNDI repository
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi_INPUT_Q");

		// create a queue connection
		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession session = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver receive = session.createReceiver(queue, "COLOR='BLUE'");
		TextMessage msg = null;

		do {
			msg = (TextMessage) receive.receive(2000);
			if(msg!=null)
			out.println("Received  messages " + msg);
		} while (msg != null);

		if (con != null)
			con.close();

		out.println("ReceiveAllMessagesSelectors Completed");

	} // end of ReceiveAllMessagesSelectors

	/**
	 * Scenario: Point to Point, Works in Conjunction with MDB</br> Send a
	 * Message to a queue MDBQ </br> MDB receives the Message </br> MDB will
	 * send that received message to MDBREPLYQ </br> This sample will then
	 * receive the message from MDBREPLYQ queue</br>
	 * 
	 * NOTE: Ensure the MDB is running before running testMDB test case.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void mdbRequestResponse(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("MDBRequestResponse Started");

		// Send Message to MDBQ
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
		.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		Queue queue = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi/MDBQ");
		QueueConnection con = cf1.createQueueConnection();
		con.start();
		QueueSession session = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = session.createSender(queue);

		TextMessage msg = session.createTextMessage();
		msg.setStringProperty("COLOR", "BLUE");
		msg.setText("MDB Test - Message to MDB");
		send.send(msg);

		out.println("Message sent successfully");

		// Waiting for the MDB to process the message and send the reply message
		// Receive the message from MDBREPLYQ to validate the test scenario
		Queue queue2 = (Queue) new InitialContext()
		.lookup("java:comp/env/jndi/MDBREPLYQ");
		QueueReceiver receiver = session.createReceiver(queue2);
		TextMessage msg1 = null;

		boolean messageReceived = false;
		do {
			// waiting 5sec so that MDB can send message to MDBREPLYQ
			msg = (TextMessage) receiver.receive(5000);
			if (msg != null) {
				// message is there in MDBREPLYQ
				messageReceived = true;
				out.println("Received messages from MDBREPLYQ" + msg);
			}
		} while (msg != null);

		if (!messageReceived) {
			throw new Exception("MDB did not receive the Message");
		} else {
			out.println("MDB has successfully received the message");
		}

		if (con != null)
			con.close();
		out.println("MDBRequestResponse Completed");

	}// end of MDBRequestResponse

}

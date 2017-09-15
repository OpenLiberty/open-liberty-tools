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

import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class JMSSamplePubSub
 */
@WebServlet("/JMSSamplePubSub")
public class JMSSamplePubSub extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public JMSSamplePubSub() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		String strAction = request.getParameter("ACTION");
		PrintWriter out = response.getWriter();
		try{


			if(strAction == null){
				out.println("Please specify the Action");
				out.println("Example : http://<host>:<port>/JMSApp/JMSSamplePubSub?ACTION=nonDurableSubscriber");
			}else if(strAction.equalsIgnoreCase("nonDurableSubscriber")){
				// Create a non durable subscriber and publish and receive the message from topic
				nonDurableSubscriber(request, response);
			}else if(strAction.equalsIgnoreCase("durableSubscriber")){
				// Create a Durable subscriber and publish and receive the message from topic
				durableSubscriber(request, response);
			}else if(strAction.equalsIgnoreCase("publishMessages")){
				// Publish 5 messages to the topic
				publishMessages(request, response);
			}else if(strAction.equalsIgnoreCase("unsubscribeDurableSubscriber")){
				// Unsubscribe the registered durable subscriber
				unsubscribeDurableSubscriber(request, response);
			}else{
				out.println("Incorrect Action Specified, the valid actions are");
				out.println("ACTION=nonDurableSubscriber");
				out.println("ACTION=durableSubscriber");
				out.println("ACTION=publishMessages");
				out.println("ACTION=unsubscribeDurableSubscriber");
			}

		}catch(Exception e){
			out.println("Something unexpected happened, check the logs or restart the server");
			e.printStackTrace();
		}
	}

	/**
	 * scenario: Performs Non-Durable pub/sub flow</br>
	 * Connects to ME using connection factory jmsTCF </br>
	 * Creates a NON-Durable subscriber for topic jmsTopic </br>
	 * Publishes a single message to the topic jmsTopic </br>
	 * Subscriber receives the message from topic jmsTopic and the message is printed on console </br>
	 *
	 * @param request HTTP request
	 * @param response HTTP response
	 * @throws Exception if an error occurs.
	 */
	public void nonDurableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("NonDurableSubscriber Started");

		// create a topic connection factory
		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jmsTCF");
		// create topic connection
		TopicConnection con = cf1.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		// Lookup topic from JNDI
		Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jmsTopic");

		// create a NON-Durable subscriber
		TopicSubscriber sub = session.createSubscriber(topic);

		// create a topic publisher
		TopicPublisher publisher = session.createPublisher(topic);

		// Publish a message to the topic
		publisher.publish(session.createTextMessage("Liberty PubSub Message"));

		TextMessage msg = (TextMessage) sub.receive(2000);
		if (null == msg) {
			throw new Exception("No message received");
		}else {
			out.println("Received message for non-durable subscriber " + msg);
		}
		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		out.println("NonDurableSubscriber Completed");

	} // NonDurableSubscriber

	/**
	 * Test scenario: Performs Durable pub/sub flow</br>
	 * Connects to ME using connection factory jmsTCF </br>
	 * Creates durable subscriber(named DURATEST) for topic jmsTopic </br>
	 * Publishes a single message to the topic jmsTopic </br>
	 * Subscriber receives the message from topic jmsTopic </br>
	 *
	 * @param request HTTP request
	 * @param response HTTP response
	 * @throws Exception if an error occurs.
	 */
	public void durableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("DurableSubscriber Started");
		// create a topic connection factory
		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jmsTCF");
		// lookup topic from JNDI
		Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jmsTopic");

		// create topic connection
		TopicConnection con = cf1.createTopicConnection();
		con.start();
		TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		// create a Durable Subscriber
		TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");

		// create a publisher
		TopicPublisher publisher = session.createPublisher(topic);

		// publish the message
		publisher.publish(session.createTextMessage("Updated Liberty PubSub Message"));


		TextMessage msg = null;
		do {
			msg = (TextMessage) sub.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);


		if (sub != null)
			sub.close();
		if (con != null)
			con.close();


		out.println("DurableSubscriber Completed");
	}// end of DurableSubscriber

	/**
	 * Test scenario: Publish messages to Topic</br>
	 * Connects to ME using connection factory jmsTCF </br>
	 * Publishes 5 messages to the topic jmsTopic </br>
	 *
	 * @param request HTTP request
	 * @param response HTTP response
	 * @throws Exception if an error occurs.
	 */
	public void publishMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("PublishMessage Started");

		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jmsTCF");
		TopicConnection con = cf1.createTopicConnection();
		int msgs = 5;

		TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jmsTopic");

		TopicPublisher publisher = session.createPublisher(topic);
		// send 5 messages
		for (int i = 0; i < msgs; i++) {
			publisher.publish(session.createTextMessage("Liberty PubSub Message : " + i));
		}
		if (con != null)
			con.close();
		out.println(msgs+ "Messages published");
		out.println("PublishMessage Completed");
	}// PublishMessage


	/**
	 * Test scenario: Unsubscribe the durable subscriber</br>
	 * Connects to ME using connection factory jmsTCF </br>
	 * Creates/Opens durable subscriber (named DURATEST) for topic jmsTopic </br>
	 * Consumes all messages to the topic jmsTopic </br>
	 * Subscriber unsubscribes from topic jmsTopic </br>
	 *
	 * @param request HTTP request
	 * @param response HTTP response
	 * @throws Exception if an error occurs.
	 */
	public void unsubscribeDurableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("UnsubscribeDurableSubscriber Started");

		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jmsTCF");
		TopicConnection con = cf1.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jmsTopic");

		TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");
		// Consume all the existing messages for durable subscriber DURATEST
		TextMessage msg = null;
		do {
			msg = (TextMessage) sub.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);

		if (sub != null)
			sub.close();

		// Unsubscribe the durable subscriber
		session.unsubscribe("DURATEST");

		if (con != null)
			con.close();

		out.println("UnsubscribeDurableSubscriber Completed");
	}//UnsubscribeDurableSubscriber

}

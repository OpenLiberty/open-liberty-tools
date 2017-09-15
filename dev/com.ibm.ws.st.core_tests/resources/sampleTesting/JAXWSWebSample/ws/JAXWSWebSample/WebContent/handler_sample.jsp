<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<link title="Style" href="<%=request.getContextPath()%>/css/result.css"
	type="text/css" rel="stylesheet">
</head>
<body>
	<div>The sample shows the functions below:</div>
	<ol>
		<li>Use @HandlerChain annotation with @WebService on the server
			side (wasdev.sample.jaxws.web.handler.RouteTracker.java)
			<div class="code">@WebService(name = "RouteTracker",
				serviceName = "RouteTrackerService", portName = "RouteTrackerPort",
				targetNamespace = "http://web.jaxws.sample.wasdev/")</div>
			<div class="code">@HandlerChain(file = "handler-test.xml")</div>
			<div class="code">public class RouteTracker {</div>
			<div class="code">......</div>
			<div class="code">}</div>
		</li>
		<li>Use @HandlerChain annotation with @WebServiceRef on the
			client side
			(wasdev.sample.jaxws.web.simple.client.SimpleStubClientServlet.java)
			<div class="code">@HandlerChain(file = "handler-client.xml")</div>
			<div class="code">@WebServiceRef(value =
				RouteTrackerService.class)</div>
			<div class="code">private RouteTracker routeTracker;</div>
		</li>
	</ol>
	<div class='outputResult'>
		Request Message:
		<c:out value="${echoParameter}" default="" />
	</div>
	<br />
	<div class='outputResult'>
		Response Message:
		<c:out value="${echoResponse}" default="" />
	</div>
	<br />
	<form action="<%=request.getContextPath()%>/HandlerClientServlet"
		target='_self' method='POST'>

		<div>Handler Chain Sample</div>
		<div>
			Request Message: <input type='text' name='echoParameter' />
		</div>
		<div>
			<input type='submit' name='submit' />
		</div>
	</form>
</body>
</html>
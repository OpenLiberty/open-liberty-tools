<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link title="Style" href="<%=request.getContextPath()%>/css/result.css"
	type="text/css" rel="stylesheet">
<title>Insert title here</title>
</head>
<body>
	<div>The sample shows the functions below:</div>
	<ol>
		<li>Create a servlet based endpoint with @WebService annotation
			(wasdev.sample.jaxws.web.simple.SimpleEcho.java)
			<div class="code">@WebService</div>
			<div class="code">public class SimpleEcho {</div>

			<div class="code">&nbsp;&nbsp;public String echo(String value)
				{</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;return "Echo Response
				[" + value + "]";</div>
			<div class="code">&nbsp;&nbsp;}</div>
			<div class="code">}</div>
		</li>
		<li>Invoke the remote web service endpoint with a stub style
			client
			(wasdev.sample.jaxws.web.simple.client.SimpleStubClientServlet.java)
			<div class="code">SimpleEchoService service = new
				SimpleEchoService(new URL(getRequestBaseURL(request) +
				"/SimpleEchoService?wsdl"), new
				QName("http://simple.web.jaxws.sample.wasdev/",
				"SimpleEchoService"));</div>
			<div class="code">SimpleEcho simpleEcho =
				service.getSimpleEchoPort();</div>

			<div class="code">setEndpointAddress((BindingProvider)
				simpleEcho, request, "SimpleEchoService");</div>

			<div class="code">String echoResponse =
				simpleEcho.echo(echoParameter);</div>
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
	<form action="<%=request.getContextPath()%>/SimpleStubClientServlet"
		target='_self' method='POST'>

		<div>Stub Client</div>
		<div>
			Echo Parameter: <input type='text' name='echoParameter' />
		</div>
		<div>
			<input type='submit' name='submit' />
		</div>
	</form>
</body>
</html>
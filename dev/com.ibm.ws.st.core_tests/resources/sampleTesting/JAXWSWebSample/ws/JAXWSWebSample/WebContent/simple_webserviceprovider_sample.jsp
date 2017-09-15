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
		<li>Create a servlet based endpoint with @WebServiceProvider
			annotation
			(wasdev.sample.jaxws.web.simple.SimpleEchoProvider.java)
			<div class="code">@WebServiceProvider(wsdlLocation =
				"WEB-INF/wsdl/SimpleEchoProviderService.wsdl", serviceName =
				"SimpleEchoProviderService", targetNamespace =
				"http://simpleEchoProvider.web.jaxws.sample.wasdev/")</div>
			<div class="code">@ServiceMode(value = Service.Mode.MESSAGE)</div>
			<div class="code">public class SimpleEchoProvider implements
				Provider &lt;SOAPMessage&gt;</div>
		</li>
		<li>Invoke the remote web service endpoint with a dynamic style
			client
			(wasdev.sample.jaxws.web.simple.client.SimpleDynamicClientServlet.java)
			<div class="code">Service service = Service.create(new
				QName("http://abc", "abc"));</div>
			<div class="code">service.addPort(new QName("http://abc",
				"anyPort"), SOAPBinding.SOAP11HTTP_BINDING,
				getRequestBaseURL(request) + "/SimpleEchoProviderService");</div>
			<div class="code">Dispatch &lt;SOAPMessage&gt; dispatch =
				service.createDispatch(new QName("http://abc", "anyPort"),
				SOAPMessage.class, Mode.MESSAGE);</div>
			<div class="code">......</div>
		</li>
	</ol>
	<div class='outputResult' style='word-wrap: break-word;'>
		Request Message:
		<c:out value="${requestMessage}" default="" escapeXml="true" />
	</div>
	<br />
	<div class='outputResult' style='word-wrap: break-word;'>
		Response Message:
		<c:out value="${responseMessage}" default="" escapeXml="true" />
	</div>
	<br />
	<form action="<%=request.getContextPath()%>/SimpleDynamicClient"
		target='_self' method='POST'>

		<div>Dynamic Client</div>
		<div>
			Echo Parameter: <input type='text' name='echoParameter' />
		</div>
		<div>
			<input type='submit' name='submit' />
		</div>
	</form>
</body>
</html>
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
		<li>Configure wsdlLocation property with a unreachable location
			(wasdev.sample.jaxws.web.catalog.client.Calculator.java)
			<div class="code">@WebServiceClient(name = "Calculator", targetNamespace = "http://catalog.web.jaxws.sample.wasdev", wsdlLocation = "http://foo.org/calculator.wsdl")</div>
			<div class="code">public class Calculator {</div>
		</li>
		<li>Create a jax-ws-catalog.xml file in WEB-INF directory
			<div class="code">&lt;?xml version="1.0" encoding="UTF-8"?&gt;</div>
			<div class="code">&lt;catalog
				xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog"
				prefer="system"&gt;</div>
			<div class="code">&lt;system
				systemId="http://foo.org/calculator.wsdl" uri="wsdl/calculator.wsdl"
				/&gt;</div>
			<div class="code">&lt;/catalog&gt;</div>
		</li>
	</ol>
	<div class='outputResult'>
		Input Parameter A :
		<c:out value="${parameter1}" default="" />
	</div>
	<div class='outputResult'>
		Input Parameter B :
		<c:out value="${parameter2}" default="" />
	</div>
	<br />
	<div class='outputResult'>
		Result :
		<c:out value="${result}" default="" />
	</div>
	<br />
	<form action="<%=request.getContextPath()%>/CatalogClientServlet"
		target='_self' method='POST'>

		<div>Catalog Facility Sample : Add Calculator</div>
		<div>
			Number A: <input type='text' name='parameter1' />
		</div>
		<div>
			Number B: <input type='text' name='parameter2' />
		</div>
		<div>
			<input type='submit' name='submit' />
		</div>
	</form>
</body>
</html>
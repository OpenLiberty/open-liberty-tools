<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<link title="Style" href="<%=request.getContextPath()%>/css/result.css" type="text/css"
	rel="stylesheet">

</head>
<body>
	<div>The sample shows the functions below:</div>
	<ol>
		<li>Use @Resource to inject WebServiceContext in the endpoint
			class
			(wasdev.sample.jaxws.web.webservicecontext.WebServiceContextQuery.java)

			<div class="code">@Resource private WebServiceContext context;</div>
		</li>
		<li>List all the properties in the current request message
			context.</li>
	</ol>

	<br />
	<div>MessageContext Properties:</div>
	<div class='outputResult'>
		<c:out value="${output}" default="" escapeXml="false" />
	</div>
	<br />
	<form action="<%=request.getContextPath()%>/WebServiceContextServlet"
		target='_self' method='POST'>

		<div>WebService Client</div>

		<div>
			<input type='submit' name='submit'
				value="Show Current MessageContext Properties" />
		</div>
	</form>
</body>
</html>
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
		<li>Use @MTOM and @Addressing with @WebService on the server side
			(wasdev.sample.jaxws.web.wsfeatures.ImageServiceImpl.java)
			<div class="code">@MTOM</div>
			<div class="code">@WebServiceRef(value =
				ImageServiceImplService.class)</div>
			<div class="code">private ImageServiceImpl
				mtomEnabledImageService;</div>
			<div class="code">@WebServiceRef(value =
				ImageServiceImplService.class)</div>
			<div class="code">private ImageServiceImpl
				mtomDisabledImageService;</div>
		</li>
		<li>Use @MTOM with @WebServiceRef on the client side
			(wasdev.sample.jaxws.web.wsfeatures.client.ImageClientServlet.java)
			<div class="code">@MTOM @WebServiceRef(value =
				ImageServiceImplService.class)</div>
			<div class="code">private ImageServiceImpl
				mtomEnabledImageService;</div>
			<div class="code">@WebServiceRef(value =
				ImageServiceImplService.class)</div>
			<div class="code">private ImageServiceImpl
				mtomDisabledImageService;</div>
		</li>
	</ol>
	<div class='outputResult'>
		Request Message:
		<c:out value="${requestMessage}" default="" escapeXml="false"/>
	</div>
	<br />
	<form action="<%=request.getContextPath()%>/ImageClientServlet"
		target='_self' method='POST'>

		<div>WebService Features Client</div>
		<div>
			Upload Id: <input type='text' name='uploadId' />
		</div>
		<div>
			<input type='submit' name='uploadMTOM' value='uploadMTOMEnabled' /><input
				type='submit' name='uploadNonMTOM' value='uploadMTOMDisabled' />
		</div>
	</form>
</body>
</html>
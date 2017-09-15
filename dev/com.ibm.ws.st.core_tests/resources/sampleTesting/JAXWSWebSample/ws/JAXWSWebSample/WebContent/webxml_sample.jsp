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
		<li>Create a servlet based endpoint, configure it in web.xml as a
			common servlet and map with a customized URL
			<div class="code">&lt;servlet&gt;</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;display-name&gt;SimpleHelloWorld&lt;/display-name&gt;</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-name&gt;SimpleHelloWorld&lt;/servlet-name&gt;</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-class&gt;wasdev.sample.jaxws.web.webxml.SimpleHelloWorldWebXml&lt;/servlet-class&gt;</div>

			<div class="code">&lt;/servlet&gt;</div>
			<div class="code">&lt;servlet-mapping&gt;</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-name&gt;SimpleHelloWorld&lt;/servlet-name&gt;</div>
			<div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;url-pattern&gt;/CustomizedHelloWorld&lt;/url-pattern&gt;</div>
			<div class="code">&lt;/servlet-mapping&gt;</div>
		</li>
		<li>Use @WebServiceRef and @Resource to inject the Service/Port
			references in the client servlet
			(wasdev.sample.jaxws.web.webxml.client.SimpleHelloWorldWebXmlClientServlet.java)</li>
	</ol>
	<c:if test="${!(empty outputs)}">
		<div class='outputResult' style='word-wrap: break-word;'>
			<table width="80%" border="1">
				<tr>
					<td align='center'>injection codes</td>
					<td align='center'>injection type</td>
					<td align='center'>request message</td>
					<td align='center'>response message</td>
				</tr>
				<c:forEach items="${outputs}" var="output">
					<tr>
						<td align='center'><c:out value="${output[0]}"
								escapeXml='false' /></td>
						<td align='center'><c:out value="${output[1]}" /></td>
						<td align='center'><c:out value="${output[2]}" /></td>
						<td align='center'><c:out value="${output[3]}" /></td>
					</tr>
				</c:forEach>
			</table>
		</div>
	</c:if>
	<br />
	<form
		action="<%=request.getContextPath()%>/SimpleHelloWorldWebXmlClientServlet"
		target='_self' method='POST'>

		<div>Client Form</div>
		<div>
			Echo Parameter: <input type='text' name='echoParameter' />
		</div>
		<div>
			<input type='submit' name='submit' />
		</div>
	</form>
</body>
</html>
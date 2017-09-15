<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link title="Style" href="css/result.css" type="text/css" rel="stylesheet">
<title>Demo of webservices.xml support</title>
</head>
<body>
<h1>Countdown EJB Sample</h1>
<div>The EJB-based web services endpoint address for this sample: <a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/CountdownImplService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/CountdownImplService</a></div>
<br/>
<div class='outputResult'>
        Response:
        <c:out value="${demoResponse}" default="" />
    </div>
    <br />
    <form action="<%=request.getContextPath()%>/CountdownClientServlet"
        target='_self' method='POST'>

        <div>Countdown Sample</div>
        <div>
            Input a number between 1 and 10: <input type='text' name='countdownfromme' />
        </div>
        
        <div>
            <input type='submit' name='submit' />
        </div>
    </form>
    <hr/>
    <h2>webservices.xml to override the annotation settings</h2>
    <span style="color: blue">
    
    <div class="code">&lt;webservices xmlns="http://java.sun.com/xml/ns/javaee" version="1.3"&gt;</div>
    <div class="code">&nbsp;&nbsp;&lt;webservice-description&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;webservice-description-name&gt;Countdown Service&lt;/webservice-description-name&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;port-component&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;port-component-name&gt;CountdownImpl&lt;/port-component-name&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;service-impl-bean&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-link&gt;CountdownImplService&lt;/servlet-link&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;/service-impl-bean&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;handler-chains&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;handler-chain&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;handler&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;handler-name&gt;ToLowerCase&lt;/handler-name&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;handler-class&gt;wasdev.sample.jaxws.ejb.webservicesxml.ToLowerCase&lt;/handler-class&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/handler&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/handler-chain&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;/handler-chains&gt;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&lt;/port-component&gt;</div>
    <div class="code">&nbsp;&nbsp;&lt;/webservice-description&gt;</div>
    <div class="code">&lt;/webservices&gt;</div>

    </span>
</body>
</html>
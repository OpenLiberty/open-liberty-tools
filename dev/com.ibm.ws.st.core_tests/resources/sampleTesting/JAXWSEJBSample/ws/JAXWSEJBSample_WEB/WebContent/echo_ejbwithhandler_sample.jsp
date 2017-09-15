<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link title="Style" href="css/result.css" type="text/css" rel="stylesheet">
<title>Demo of an EJB web services with handlers configured</title>
</head>
<body>
<h1>Echo EJB as web services with handlers</h1>
<div>The EJB-based web services endpoint address for this sample: <a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%>/AnEJBWebServicesWithHandler/EchoBeanService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%>/AnEJBWebServicesWithHandler/EchoBeanService</a></div>
<br/>
<div class='outputResult'>
        Response:
        <c:out value="${demoResponse}" default="" />
    </div>
    <br />
    <form action="<%=request.getContextPath()%>/EchoClientServlet"
        target='_self' method='POST'>

        <div>Echo Demo Client</div>
        <div>
            Input string to echo: <input type='text' name='echoString' />
        </div>
        
        <div>
            <input type='submit' name='submit' />
        </div>
    </form>
    <hr/>
    <h2>Sample code:</h2>
    <span style="color: blue">
    
    <div class="code">@Stateless</div>
    <div class="code">@WebService</div>
    <div class="code">@HandlerChain(file = "handlers.xml")</div>
    <div class="code">public class EchoBean {</div>
    

    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;public String echo(String value) {</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return value;</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;}</div>

    <div class="code">}</div>
    </span>

</body>
</html>
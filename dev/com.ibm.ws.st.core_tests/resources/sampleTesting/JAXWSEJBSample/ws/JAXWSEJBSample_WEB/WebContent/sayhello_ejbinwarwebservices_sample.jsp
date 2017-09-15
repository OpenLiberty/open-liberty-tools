<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link title="Style" href="css/result.css" type="text/css" rel="stylesheet">
<title>Demo of an EJB in WAR Web Services</title>
</head>
<body>
<h1>SayHello EJB in WAR as web services</h1>
<div>The EJB-based web services endpoint addresses for this sample:</div>
<div><a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloStatelessService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloStatelessService</a></div>
<div><a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloSingletonService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloSingletonService</a></div>
<div><a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloPOJOService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath() %>/SayHelloPOJOService</a></div>
<br/>
<div class='outputResult'>
        Response:
        <c:out value="${demoResponse}" default="" />
    </div>
    <br />
    <form action="<%=request.getContextPath()%>/EJBInWarWebServicesServlet"
        target='_self' method='POST'>

        <div>SayHello Demo Client</div>
        <div>
            What method to call: <br>
            <INPUT TYPE="RADIO" NAME="method" VALUE="SayHelloFromStatelessBean">SayHelloFromStatelessBean<BR>
            <INPUT TYPE="RADIO" NAME="method" VALUE="SayHelloFromSingletonBean"/>SayHelloFromSingletonBean<BR/>
            <INPUT TYPE="RADIO" NAME="method" VALUE="SayHelloFromPOJO"/>SayHelloFromPOJO<BR/>
            <INPUT TYPE="RADIO" NAME="method" VALUE="InvokeOtherFromStatelessBean"/>InvokeOtherFromStatelessBean<BR/>
            <INPUT TYPE="RADIO" NAME="method" VALUE="InvokeOtherFromSingletonBean"/>InvokeOtherFromSingletonBean<BR/>
            <INPUT TYPE="RADIO" NAME="method" VALUE="InvokeOtherFromPOJO"/>InvokeOtherFromPOJO<BR/>
            
        </div>
        
        
        <div>
            <input type='submit' name='submit' />
        </div>
    </form>
    <hr/>
    <h2>A Sample EJB directly in WAR</h2>
    <span style="color: blue">
    <div class="code">@WebService(serviceName = "SayHelloStatelessService", portName = "SayHelloStalelessPort")</div>
    <div class="code">@Stateless(name = "SayHelloBean")</div>
    <div class="code">public class SayHelloStatelessBean implements SayHelloLocal {</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;@Resource</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;SessionContext context;</div>

    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;@Override</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;public String invokeOther() {</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return context.getBusinessObject(SayHelloLocal.class).sayHello("Anonym");</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;}</div>

    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;@Override</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;public String sayHello(String name) {</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return "Hello, " + name + " from " + getClass().getSimpleName();</div>
    <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;}</div>
    <div class="code">}</div>
    </span>


</body>
</html>
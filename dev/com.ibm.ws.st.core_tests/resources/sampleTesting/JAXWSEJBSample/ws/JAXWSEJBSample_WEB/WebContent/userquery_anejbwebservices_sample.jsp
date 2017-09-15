<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link title="Style" href="css/result.css" type="text/css" rel="stylesheet">
<title>Demo of EJB as Web Services</title>
</head>
<body>
<h1>User Query EJB as a Web Services</h1>
<div>The EJB-based web services endpoint address for this sample: <a href="http://<%=request.getServerName()%>:<%=request.getServerPort()%>/AnEJBWebServices/UserQueryService" target="_blank">http://<%=request.getServerName()%>:<%=request.getServerPort()%>/AnEJBWebServices/UserQueryService</a></div>
<br/>
<div class='outputResult'>
        Response:
        <c:out value="${demoResponse}" default="" />
    </div>
    <br/>
    <div>Response in session: <%=request.getSession().getAttribute("demoResponse") %></div>
	<br/>
    <form action="<%=request.getContextPath()%>/EJBWebServicesWebClientServlet"
        target='_self' method='POST'>

        <div>User Query Client</div>
        <div>
            What method to call: 
            <INPUT TYPE="RADIO" NAME="method" VALUE="webQueryUser" onclick="document.getElementById('username').removeAttribute('disabled')">webQueryUser
            <INPUT TYPE="RADIO" NAME="method" VALUE="webListUsers" onclick="document.getElementById('username').disabled = true"/>webListUsers
            <INPUT TYPE="RADIO" NAME="method" VALUE="webUserNotFoundException" onclick="document.getElementById('username').disabled = true"/>webUserNotFoundException<BR/>  
        </div>
        <div>
            User name to query: <input type='text' name='username' id="username" disabled/>
        </div>
        <div>
            <input type='submit' name='submit' />
        </div>
    </form>
    <hr/>
    <h1>EJB as Web Services</h1>
<div>The general steps to create the sample as following:</div>
    <ol>
        <li>Create an EJB based endpoint with @WebService annotation
            (wasdev.sample.jaxws.ejb.ejbwebservices.UserQuery.java)<br>
            <br>
			<span style="color: blue">
            <div class="code">@Stateless</div>
			<div class="code">@WebService</div>
            <div class="code">public class UserQuery {</div>

            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;public User getUser(String userName) throws UserNotFoundException {</div>
            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return StaticUserRepository.getUser(userName);</div>
            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;}</div>

            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;public User[] listUsers() {</div>
            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return StaticUserRepository.listUsers();</div>
            <div class="code">&nbsp;&nbsp;&nbsp;&nbsp;}</div>
            <div class="code">}</div>
			</span>
        </li>
		<br>
        <li>Use "wsgen" command to generate the WSDL file
            <div>For example, on windows:</div>
			<span style="color: blue">
			<div class="code">D:\&gt;&lt;WebSphere Liberty Profile Install Home&gt;\bin\wsgen.bat \</div>
			<div class="code">-cp &lt;path_to_compiled_SEI_class&gt;;&lt;WebSphere Liberty Profile Install Home&gt;\dev\spec\com.ibm.ws.javaee.ejb.3.1_1.0.0.jar \</div>
            <div class="code">-d D:\tmp\beta\wsgen_results -keep -r D:\tmp\beta\wsgen_results -s D:\tmp\beta\wsgen_results -wsdl \</div>
            <div class="code">wasdev.sample.jaxws.ejb.ejbwebservices.UserQuery</div>
			</span>
			<br>
            <div>The WSDL file will be generated in folder "D:\tmp\beta\wsgen_results".</div>			
        </li>
		<br>
		<li>Use "wsimport" to generate service endpoint interface for client use
			<div>For example, on windows:</div>
			<span style="color: blue">
			<div class="code">D:\&gt;&lt;WebSphere Liberty Profile Install Home&gt;\bin\wsimport.bat -d D:\tmp\beta\wsimport_results -keep \</div>
			<div class="code">-p wasdev.sample.jaxws.ejb.ejbwebservices.webclient -s D:\tmp\beta\wsimport_results -verbose \</div>
            <div class="code">D:\tmp\beta\wsgen_results\UserQueryService.wsdl</div>
			</span>
			<br>
            <div>The client artifacts will be generated in folder "D:\tmp\beta\wsimport_results".</div>
		</li>
		<li>Code client logic with the generated web services client artifacts
        </li>
    </ol>
</body>
</html>
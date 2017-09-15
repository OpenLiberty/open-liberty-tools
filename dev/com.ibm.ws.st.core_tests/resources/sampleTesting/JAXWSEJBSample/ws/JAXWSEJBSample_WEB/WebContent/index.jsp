<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4/01 Transitional//EN" "http://www/w3/org/TR/html4/loose/dtd">
<HTML style="height: 100%">

<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<style type="text/css">
.menu_navigation_1 {
	font-family: Comic Sans MS;
	font-size: 16px;
}

.menu_navigation_2 {
	font-family: monospace;
	font-size: 15px;
}
</style>
<TITLE>JAX-WS Demo - EJB as Web Services</TITLE>

</HEAD>

<BODY style="height: 100%">
	<table width="100%" height="100%" border="1" cellpadding="0"
		cellspacing="0">
		<tr>
			<td colspan="2" align="center"><h2
					style='font-family: Comic Sans MS'>JAX-WS Demo - EJB as Web Services</h2></td>
		</tr>
		<tr>
			<td height="100%" width="20%" valign="top">
				<table cellpadding="10" cellspacing="10">
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/userquery_anejbwebservices_sample.jsp">DEMO
									H: @WebService on an EJB</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/sayhello_ejbinwarwebservices_sample.jsp">DEMO
									I: @WebService on an EJB in WAR</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/echo_ejbwithhandler_sample.jsp">DEMO
									J: An EJB with web services handlers</a>
							</div>
						</td>
					</tr>
					<tr>
						<td><div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/countdown_webservicesxmlsupport_sample.jsp">DEMO
									K: webservices.xml support</a>
							</div></td>
					</tr>					
				</table>
			</td>
			<td height="100%" align='center'><iframe height="100%"
					width="100%" name="displayFrame" src="serverconfig.html"
					id="displayFrame"> </iframe></td>
		</tr>
	</table>
</BODY>


</HTML>

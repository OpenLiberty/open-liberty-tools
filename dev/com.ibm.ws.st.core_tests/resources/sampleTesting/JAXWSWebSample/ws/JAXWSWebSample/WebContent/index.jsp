<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4/01 Transitional//EN" "http://www/w3/org/TR/html4/loose/dtd">
<!--NewPage-->
<HTML style="height: 100%">
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<style type="text/css">
.menu_navigation_1 {
	font-family: Comic Sans MS;
	font-size: 16px;
}

.menu_navigation_2 {
	font-family: Comic Sans MS;
	font-size: 15px;
}
</style>
<TITLE>jaxws demo</TITLE>

</HEAD>

<BODY style="height: 100%">
	<table width="100%" height="100%" border="1" cellpadding="0"
		cellspacing="0">
		<tr>
			<td colspan="2" align="center"><h2
					style='font-family: Comic Sans MS'>JaxWs Demo --- Liberty</h2></td>
		</tr>
		<tr>
			<td height="100%" width="20%" valign="top">
				<table cellpadding="10" cellspacing="10">
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/simple_webservice_sample.jsp">DEMO
									A: @WebService/Stub Client</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/simple_webserviceprovider_sample.jsp">DEMO
									B: @WebServiceProvider/Dynamic Client</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/webxml_sample.jsp">DEMO
									C: POJO endpoint in web.xml/@WebServiceRef</a>
							</div>
						</td>
					</tr>
					<tr>
						<td><div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/handler_sample.jsp">DEMO
									D: @HandlerChain Server/Client Support</a>
							</div></td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/webservicecontext_sample.jsp">DEMO
									E:@Resource WebServiceContext</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/catalog_sample.jsp">DEMO
									F:Catalog Support</a>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div class='menu_navigation_1'>
								<a target="displayFrame"
									href="<%=request.getContextPath()%>/wsfeatures_sample.jsp">DEMO
									G:MTOM Support</a>
							</div>
						</td>
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

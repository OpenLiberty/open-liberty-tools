<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<body>
<h2>Contexts and Dependency Injection (CDI) Sample</h2>
<%-- See wasdev.sample.cdi.NamedBean (cdiSample.zip/cdiSample/dropins/cdiApp.war/WEB-INF/classes/wasdev/sample/cdi/NamedBean.java) --%>
<c:out value="${namedBean.message}"/>
</body>
</html>

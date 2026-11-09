<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.UserDAO" %>
<%!
    static String h(Object o) {
        if (o == null) return "";
        return o.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    List<UserDAO.GuestProfile> guests = (List<UserDAO.GuestProfile>) request.getAttribute("guests");
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Guest Directory</title>
</head>
<body>
  <h1>Guest Directory</h1>
  <a href="<%= ctx %>/admin/dashboard">Back to dashboard</a>

  <% if (error != null) { %><p style="color:red"><%= h(error) %></p><% } %>
  <% if (message != null) { %><p style="color:green"><%= h(message) %></p><% } %>

  <table border="1">
    <tr>
      <th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Action</th>
    </tr>
    <% if (guests != null) {
         for (UserDAO.GuestProfile g : guests) { %>
           <tr>
        <td><%= g.userId() %></td>
        <td><input type="text"  name="name"  form="g<%= g.userId() %>" value="<%= h(g.name()) %>"  required></td>
        <td><input type="email" name="email" form="g<%= g.userId() %>" value="<%= h(g.email()) %>" required></td>
        <td><input type="text"  name="phone" form="g<%= g.userId() %>" value="<%= h(g.phone()) %>"></td>
        <td>
          <form id="g<%= g.userId() %>" method="post" action="<%= ctx %>/admin/guests">
            <input type="hidden" name="guestId" value="<%= g.userId() %>">
            <button type="submit">Save</button>
          </form>
        </td>
      </tr>
    <%   }
       } %>
  </table>
</body>
</html>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.RoomDAO" %>
<%!
    static String h(Object o) {
        if (o == null) return "";
        return o.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    RoomDAO.RoomRow room = (RoomDAO.RoomRow) request.getAttribute("room"); // null = adding
    String error = (String) request.getAttribute("error");
    boolean editing = (room != null);
    long typeId = editing ? room.typeId() : 0;
%>
<!DOCTYPE html>
<html>
<head>
  <title><%= editing ? "Edit Room" : "Add Room" %></title>
</head>
<body>
  <h1><%= editing ? "Edit Room" : "Add Room" %></h1>
  <a href="<%= ctx %>/admin/rooms">Back to rooms</a>

  <% if (error != null) { %><p style="color:red"><%= h(error) %></p><% } %>

  <form method="post" action="<%= ctx %>/admin/rooms">
    <input type="hidden" name="action" value="<%= editing ? "update" : "add" %>">
    <% if (editing) { %>
      <input type="hidden" name="roomId" value="<%= room.roomId() %>">
    <% } %>

    <p>
      Room number:
      <input type="text" name="roomNumber" value="<%= editing ? h(room.roomNumber()) : "" %>" required>
    </p>
    <p>
      Type:
      <select name="typeId">
        <%-- PLACEHOLDER: IDs match seed.sql order --%>
        <option value="1" <%= typeId == 1 ? "selected" : "" %>>Standard</option>
        <option value="2" <%= typeId == 2 ? "selected" : "" %>>Deluxe</option>
        <option value="3" <%= typeId == 3 ? "selected" : "" %>>Suite</option>
      </select>
    </p>
    <p>
      Floor:
      <input type="number" name="floor" min="1" value="<%= editing ? room.floor() : "" %>" required>
    </p>

    <button type="submit"><%= editing ? "Save changes" : "Add room" %></button>
  </form>
</body>
</html>
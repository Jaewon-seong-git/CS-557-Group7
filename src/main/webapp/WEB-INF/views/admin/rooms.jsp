<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.RoomDAO" %>
<%!
    // Escape user content before printing (required by PROJECT_RULES)
    static String h(Object o) {
        if (o == null) return "";
        return o.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    List<RoomDAO.RoomRow> rooms = (List<RoomDAO.RoomRow>) request.getAttribute("rooms");
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Rooms</title>
</head>
<body>
  <h1>Rooms</h1>
  <a href="<%= ctx %>/admin/dashboard">Back to dashboard</a>

  <% if (error != null) { %><p style="color:red"><%= h(error) %></p><% } %>
  <% if (message != null) { %><p style="color:green"><%= h(message) %></p><% } %>

  <p><a href="<%= ctx %>/admin/rooms?action=new">+ Add room</a></p>

  <table border="1">
    <tr>
      <th>Room #</th><th>Type</th><th>Floor</th><th>Capacity</th><th>Price/night</th><th>Actions</th>
    </tr>
    <% if (rooms != null) {
         for (RoomDAO.RoomRow r : rooms) { %>
      <tr>
        <td><%= h(r.roomNumber()) %></td>
        <td><%= h(r.roomTypeName()) %></td>
        <td><%= r.floor() %></td>
        <td><%= r.capacity() %></td>
        <td>$<%= r.pricePerNight() %></td>
        <td>
          <a href="<%= ctx %>/admin/rooms?action=edit&roomId=<%= r.roomId() %>">Edit</a>
          <form method="post" action="<%= ctx %>/admin/rooms" style="display:inline">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="roomId" value="<%= r.roomId() %>">
            <button type="submit">Delete</button>
          </form>
        </td>
      </tr>
    <%   }
       } %>
  </table>

  <h2>Update room type price</h2>
  <form method="post" action="<%= ctx %>/admin/room-types/price">
    <select name="typeId">
      <%-- PLACEHOLDER: IDs match seed.sql order; replace with DB list if a RoomType DAO is added --%>
      <option value="1">Standard</option>
      <option value="2">Deluxe</option>
      <option value="3">Suite</option>
    </select>
    <input type="number" name="pricePerNight" step="0.01" min="0" required>
    <button type="submit">Update price</button>
  </form>
</body>
</html>
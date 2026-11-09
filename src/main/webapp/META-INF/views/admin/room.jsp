<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.RoomDAO" %>
<%
    List<RoomDAO.RoomRow> rooms = (List<RoomDAO.RoomRow>) request.getAttribute("rooms");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Rooms</title>
</head>
<body>
  <h1>Rooms</h1>
  <a href="<%= request.getContextPath() %>/admin/dashboard">Back to dashboard</a>

  <% if (error != null) { %>
    <p style="color:red"><%= error %></p>
  <% } %>
  
  <h2>Add a room</h2>
  <form method="post" action="<%= request.getContextPath() %>/admin/rooms">
    <input type="hidden" name="action" value="add">

    Room number: <input name="roomNumber" required>

    Type:
    <select name="typeId" required>
      <option value="1">Standard</option>
      <option value="2">Deluxe</option>
      <option value="3">Suite</option>
    </select>

    Floor: <input name="floor" type="number" min="0" required>

    <button type="submit">Add room</button>
  </form>

  <table border="1">
    <tr>
      <th>Room</th><th>Type</th><th>Floor</th><th>Capacity</th><th>Price/night</th>
    </tr>
    <% if (rooms != null) {
         for (RoomDAO.RoomRow r : rooms) { %>
      <tr>
        <td><%= r.roomNumber() %></td>
        <td><%= r.roomTypeName() %></td>
        <td><%= r.floor() %></td>
        <td><%= r.capacity() %></td>
        <td>$<%= r.pricePerNight() %></td>
      </tr>
    <%   }
       } %>
  </table>
</body>
</html>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% String ctx = request.getContextPath(); %>
<!DOCTYPE html>
<html>
<head>
  <title>Admin Dashboard</title>
</head>
<body>
  <h1>Admin Dashboard</h1>

  <ul>
    <li><a href="<%= ctx %>/admin/rooms">Manage Rooms</a></li>
    <li><a href="<%= ctx %>/admin/reservations">Reservations</a></li>
    <li><a href="<%= ctx %>/admin/guests">Guest Directory</a></li>
  </ul>

  <form method="post" action="<%= ctx %>/logout">
    <button type="submit">Logout</button>
  </form>
</body>
</html>
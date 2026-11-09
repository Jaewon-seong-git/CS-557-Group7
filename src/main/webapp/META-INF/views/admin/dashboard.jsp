<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <title>Admin Dashboard</title>
</head>
<body>
  <h1>ADMIN DASHBOARD</h1>

  <a href="<%= request.getContextPath() %>/admin/rooms">Rooms</a>
  <a href="<%= request.getContextPath() %>/admin/reservations">Reservations</a>
  <a href="<%= request.getContextPath() %>/admin/guests">Guests</a>
</body>
</html>
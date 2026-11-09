<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.ReservationDAO" %>
<%!
    static String h(Object o) {
        if (o == null) return "";
        return o.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    List<ReservationDAO.AdminReservationRow> reservations =
        (List<ReservationDAO.AdminReservationRow>) request.getAttribute("reservations");
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
    String[] statuses = {"PENDING", "APPROVED", "CONFIRMED", "CANCELLED"};
%>
<!DOCTYPE html>
<html>
<head>
  <title>Reservations</title>
</head>
<body>
  <h1>Reservations</h1>
  <a href="<%= ctx %>/admin/dashboard">Back to dashboard</a>

  <% if (error != null) { %><p style="color:red"><%= h(error) %></p><% } %>
  <% if (message != null) { %><p style="color:green"><%= h(message) %></p><% } %>

  <form method="post" action="<%= ctx %>/admin/expire-overdue">
    <button type="submit">Expire overdue reservations</button>
  </form>

  <table border="1">
    <tr>
      <th>#</th><th>Guest</th><th>Room</th><th>Check-in</th><th>Check-out</th>
      <th>Total</th><th>Status</th><th>Approve</th><th>Override status</th>
    </tr>
    <% if (reservations != null) {
         for (ReservationDAO.AdminReservationRow r : reservations) { %>
      <tr>
        <td><%= r.reservationId() %></td>
        <td><%= h(r.guestName()) %></td>
        <td><%= h(r.roomNumber()) %></td>
        <td><%= r.checkIn() %></td>
        <td><%= r.checkOut() %></td>
        <td>$<%= r.totalPrice() %></td>
        <td><%= h(r.status()) %></td>
        <td>
          <% if ("PENDING".equals(r.status())) { %>
            <form method="post" action="<%= ctx %>/admin/reservations">
              <input type="hidden" name="action" value="approve">
              <input type="hidden" name="reservationId" value="<%= r.reservationId() %>">
              <button type="submit">Approve</button>
            </form>
          <% } %>
        </td>
        <td>
          <form method="post" action="<%= ctx %>/admin/reservations">
            <input type="hidden" name="action" value="override">
            <input type="hidden" name="reservationId" value="<%= r.reservationId() %>">
            <select name="status">
              <% for (String s : statuses) { %>
                <option value="<%= s %>" <%= s.equals(r.status()) ? "selected" : "" %>><%= s %></option>
              <% } %>
            </select>
            <button type="submit">Set</button>
          </form>
        </td>
      </tr>
    <%   }
       } %>
  </table>
</body>
</html>
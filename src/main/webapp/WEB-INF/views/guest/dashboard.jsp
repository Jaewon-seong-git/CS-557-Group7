<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Guest Dashboard - Hotel Reservation System</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="container">
        <header class="nav-header">
            <h1>Guest Dashboard</h1>
            <form action="${pageContext.request.contextPath}/logout" method="post" style="margin:0;">
                <button type="submit" class="btn btn-secondary">Logout</button>
            </form>
        </header>

        <p>Welcome, <strong>${sessionScope.email}</strong>. Choose an option below:</p>

        <div class="card-grid">
            <a class="card-link" href="${pageContext.request.contextPath}/guest/search">
                <h3>Search &amp; Book Rooms</h3>
                <p>Find available rooms (check-in must be at least 7 days away).</p>
            </a>
            <a class="card-link" href="${pageContext.request.contextPath}/guest/bookings">
                <h3>My Bookings</h3>
                <p>View reservations, complete payment, or cancel bookings.</p>
            </a>
            <a class="card-link" href="${pageContext.request.contextPath}/guest/profile">
                <h3>My Profile</h3>
                <p>Update your name, email, and phone number.</p>
            </a>
        </div>
    </div>
</body>
</html>

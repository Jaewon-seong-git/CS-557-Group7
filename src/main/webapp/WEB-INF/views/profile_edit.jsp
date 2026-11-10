<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.UserDAO.GuestProfile" %>

<%!
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<%
    GuestProfile profile =
            (GuestProfile) request.getAttribute("profile");
    String errorMessage =
            (String) request.getAttribute("errorMessage");
    String successMessage =
            (String) request.getAttribute("successMessage");

    String name = profile == null ? "" : profile.name();
    String email = profile == null ? "" : profile.email();
    String phone = profile == null ? "" : profile.phone();

    String profileUrl = response.encodeURL(
            request.getContextPath() + "/guest/profile");
    String logoutUrl = response.encodeURL(
            request.getContextPath() + "/logout");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Edit Profile | Hotel Reservation</title>

    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            padding: 24px 16px;
            font-family: Arial, sans-serif;
            background: #f3f4f6;
            color: #1f2937;
        }

        main {
            max-width: 560px;
            margin: 24px auto;
            padding: 32px;
            background: #ffffff;
            border: 1px solid #d1d5db;
            border-radius: 12px;
        }

        h1 {
            margin-top: 0;
        }

        .description,
        .hint {
            color: #4b5563;
            line-height: 1.5;
        }

        label {
            display: block;
            margin-top: 20px;
            margin-bottom: 8px;
            font-weight: bold;
        }

        input {
            width: 100%;
            padding: 12px;
            border: 1px solid #6b7280;
            border-radius: 6px;
            font: inherit;
        }

        input:focus-visible,
        button:focus-visible {
            outline: 3px solid #2563eb;
            outline-offset: 3px;
        }

        .hint {
            margin-top: 6px;
            font-size: 0.875rem;
        }

        .message {
            padding: 12px;
            margin: 20px 0;
            border: 1px solid;
            border-radius: 6px;
        }

        .error {
            color: #991b1b;
            background: #fef2f2;
        }

        .success {
            color: #166534;
            background: #f0fdf4;
        }

        button {
            padding: 12px 20px;
            border: 0;
            border-radius: 6px;
            font: inherit;
            cursor: pointer;
        }

        .save-button {
            margin-top: 24px;
            background: #1f2937;
            color: #ffffff;
        }

        .logout-form {
            margin-top: 28px;
            padding-top: 20px;
            border-top: 1px solid #d1d5db;
        }

        .logout-button {
            background: #e5e7eb;
            color: #1f2937;
        }
    </style>
</head>

<body>
<main>
    <h1>Edit Profile</h1>
    <p class="description">
        Update your contact details below. Name and email are required.
    </p>

    <% if (errorMessage != null) { %>
        <div class="message error" role="alert">
            <%= escapeHtml(errorMessage) %>
        </div>
    <% } %>

    <% if (successMessage != null) { %>
        <div class="message success" role="status">
            <%= escapeHtml(successMessage) %>
        </div>
    <% } %>

    <form method="post" action="<%= escapeHtml(profileUrl) %>">
        <label for="name">Full name</label>
        <input
            type="text"
            id="name"
            name="name"
            value="<%= escapeHtml(name) %>"
            maxlength="100"
            autocomplete="name"
            required
        >

        <label for="email">Email address</label>
        <input
            type="email"
            id="email"
            name="email"
            value="<%= escapeHtml(email) %>"
            maxlength="254"
            autocomplete="email"
            required
        >

        <label for="phone">Phone number (optional)</label>
        <input
            type="tel"
            id="phone"
            name="phone"
            value="<%= escapeHtml(phone) %>"
            maxlength="25"
            autocomplete="tel"
            aria-describedby="phone-hint"
        >
        <p id="phone-hint" class="hint">
            Leave blank, or enter 7–25 characters using numbers,
            spaces, +, parentheses, periods, or hyphens.
        </p>

        <button type="submit" class="save-button">Save changes</button>
    </form>

    <form method="post"
          action="<%= escapeHtml(logoutUrl) %>"
          class="logout-form">
        <button type="submit" class="logout-button">Sign out</button>
    </form>
</main>
</body>
</html>
package servlet;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import util.InputValidator;

/** Admin guest directory: view guests and edit contact info only (no password data). */
@WebServlet("/admin/guests")
public class AdminGuestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST_JSP = "/WEB-INF/views/admin/guests.jsp";
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            Long guestId = InputValidator.parsePositiveId(request.getParameter("guestId"));
            String name = request.getParameter("name");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");

            if (guestId == null) throw new IllegalArgumentException("Invalid guest.");
            if (!InputValidator.isValidName(name)) throw new IllegalArgumentException("Name is required (max 100 characters).");
            if (!InputValidator.isValidEmail(email)) throw new IllegalArgumentException("Email is not valid.");
            if (!InputValidator.isValidPhone(phone)) throw new IllegalArgumentException("Phone is not valid.");

            if (!userDAO.adminUpdateGuestProfile(guestId, name.trim(), email.trim(),
                    phone == null ? null : phone.trim())) {
                throw new IllegalArgumentException("Guest not found.");
            }
            request.setAttribute("message", "Guest #" + guestId + " updated.");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            request.setAttribute("error", "That email is already used by another account.");
        } catch (SQLException e) {
            request.setAttribute("error", "Database error. Please try again.");
        }
        showList(request, response);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("guests", userDAO.findAllGuests());
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load guests.");
        }
        request.getRequestDispatcher(LIST_JSP).forward(request, response);
    }
}
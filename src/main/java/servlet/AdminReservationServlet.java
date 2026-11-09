package servlet;

import dao.ReservationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import util.InputValidator;

/** Admin reservation list, PENDING -> APPROVED approval, status override, and expiry job. */
@WebServlet(urlPatterns = {"/admin/reservations", "/admin/expire-overdue"})
public class AdminReservationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST_JSP = "/WEB-INF/views/admin/reservations.jsp";
    private final ReservationDAO reservationDAO = new ReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = "/admin/expire-overdue".equals(request.getServletPath())
                ? "expire" : request.getParameter("action");
        try {
            String result = switch (action == null ? "" : action) {
                case "approve" -> approve(request);
                case "override" -> override(request);
                case "expire" -> expire();
                default -> throw new IllegalArgumentException("Unknown action.");
            };
            request.setAttribute("message", result);
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            request.setAttribute("error", "Database error. Please try again.");
        }
        showList(request, response);
    }

    private String approve(HttpServletRequest req) throws SQLException {
        long id = requireId(req);
        if (!reservationDAO.approveReservation(id)) {
            throw new IllegalArgumentException("Only PENDING reservations can be approved.");
        }
        return "Reservation #" + id + " approved.";
    }

    private String override(HttpServletRequest req) throws SQLException {
        long id = requireId(req);
        String status = req.getParameter("status"); // DAO rejects anything not in the 4 allowed values
        if (!reservationDAO.adminSetReservationStatus(id, status)) {
            throw new IllegalArgumentException("Reservation not found.");
        }
        return "Reservation #" + id + " set to " + status.toUpperCase() + ".";
    }

    private String expire() throws SQLException {
        int count = reservationDAO.expireOverdueReservations();
        return count + " overdue reservation(s) cancelled.";
    }

    private long requireId(HttpServletRequest req) {
        Long id = InputValidator.parsePositiveId(req.getParameter("reservationId"));
        if (id == null) throw new IllegalArgumentException("reservationId must be a positive number.");
        return id;
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("reservations", reservationDAO.findReservationsForAdmin(null));
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load reservations.");
        }
        request.getRequestDispatcher(LIST_JSP).forward(request, response);
    }
}
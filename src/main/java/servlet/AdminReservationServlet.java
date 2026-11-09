package servlet;

import dao.ReservationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/reservations")
public class AdminReservationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ReservationDAO reservationDAO = new ReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("reservations", reservationDAO.findReservationsForAdmin(null));
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load reservations.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/reservations.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        try {
            if ("approve".equals(action)) {
                long reservationId = Long.parseLong(request.getParameter("reservationId"));
                boolean approved = reservationDAO.approveReservation(reservationId);
                if (!approved) {
                    request.setAttribute("error", "Only PENDING reservations can be approved.");
                    doGet(request, response);
                    return;
                }
            }
        } catch (SQLException e) {
            request.setAttribute("error", "Could not update reservation: " + e.getMessage());
            doGet(request, response);
            return;
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", "Invalid input: " + e.getMessage());
            doGet(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/reservations");
    }
}
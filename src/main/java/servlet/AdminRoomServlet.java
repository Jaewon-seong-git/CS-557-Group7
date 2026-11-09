package servlet;

import dao.RoomDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {"/admin/rooms", "/admin/room-types/price"})
public class AdminRoomServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final RoomDAO roomDAO = new RoomDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("rooms", roomDAO.findAllRooms()); //get all room data 
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load rooms.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/rooms.jsp").forward(request, response); //forward "room" to room.jsp 
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        try {
            if ("/admin/room-types/price".equals(request.getServletPath())) {
                long typeId = Long.parseLong(request.getParameter("typeId"));
                BigDecimal price = new BigDecimal(request.getParameter("pricePerNight"));
                roomDAO.updateRoomTypePrice(typeId, price);
            } else if ("add".equals(action)) {
                String roomNumber = request.getParameter("roomNumber");
                long typeId = Long.parseLong(request.getParameter("typeId"));
                int floor = Integer.parseInt(request.getParameter("floor"));
                roomDAO.addRoom(roomNumber, typeId, floor);
            } else if ("delete".equals(action)) {
                long roomId = Long.parseLong(request.getParameter("roomId"));
                roomDAO.deleteRoom(roomId);
            } else if ("update".equals(action)) {
                long roomId = Long.parseLong(request.getParameter("roomId"));
                String roomNumber = request.getParameter("roomNumber");
                long typeId = Long.parseLong(request.getParameter("typeId"));
                int floor = Integer.parseInt(request.getParameter("floor"));
                roomDAO.updateRoom(roomId, roomNumber, typeId, floor);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                request.setAttribute("error", "This room has reservations and cannot be deleted.");
            } else {
                request.setAttribute("error", "Could not save room: " + e.getMessage());
            }
            doGet(request, response);
            return;
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", "Invalid input: " + e.getMessage());
            doGet(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/rooms");
    }
}
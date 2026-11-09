package servlet;

import dao.RoomDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/rooms")
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
}
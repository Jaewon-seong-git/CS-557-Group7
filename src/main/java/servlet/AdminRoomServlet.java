package servlet;

import dao.RoomDAO;
import dao.RoomDAO.RoomRow;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import util.InputValidator;

/** Admin room CRUD and room type price updates. RoleFilter guards /admin/*. */
@WebServlet(urlPatterns = {"/admin/rooms", "/admin/room-types/price"})
public class AdminRoomServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST_JSP = "/WEB-INF/views/admin/rooms.jsp";
    private static final String FORM_JSP = "/WEB-INF/views/admin/room-form.jsp";
    private final RoomDAO roomDAO = new RoomDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("new".equals(action)) {
            request.getRequestDispatcher(FORM_JSP).forward(request, response);
            return;
        }
        if ("edit".equals(action)) {
            Long roomId = InputValidator.parsePositiveId(request.getParameter("roomId"));
            RoomRow room = (roomId == null) ? null : findRoom(roomId);
            if (room == null) {
                request.setAttribute("error", "Room not found.");
                showList(request, response);
                return;
            }
            request.setAttribute("room", room);
            request.getRequestDispatcher(FORM_JSP).forward(request, response);
            return;
        }
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = "/admin/room-types/price".equals(request.getServletPath())
                ? "price" : request.getParameter("action");
        try {
            String result = switch (action == null ? "" : action) {
                case "add" -> addRoom(request);
                case "update" -> updateRoom(request);
                case "delete" -> deleteRoom(request);
                case "price" -> updatePrice(request);
                default -> throw new IllegalArgumentException("Unknown action.");
            };
            request.setAttribute("message", result);
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            request.setAttribute("error", "delete".equals(action)
                    ? "Cannot delete: this room has reservations."
                    : "That room number already exists.");
        } catch (SQLException e) {
            request.setAttribute("error", "Database error. Please try again.");
        }
        showList(request, response);
    }

    private String addRoom(HttpServletRequest req) throws SQLException {
        String roomNumber = requireRoomNumber(req);
        long typeId = requireId(req, "typeId");
        int floor = requireFloor(req);
        roomDAO.addRoom(roomNumber, typeId, floor);
        return "Room added.";
    }

    private String updateRoom(HttpServletRequest req) throws SQLException {
        long roomId = requireId(req, "roomId");
        String roomNumber = requireRoomNumber(req);
        long typeId = requireId(req, "typeId");
        int floor = requireFloor(req);
        if (!roomDAO.updateRoom(roomId, roomNumber, typeId, floor)) {
            throw new IllegalArgumentException("Room not found.");
        }
        return "Room updated.";
    }

    private String deleteRoom(HttpServletRequest req) throws SQLException {
        long roomId = requireId(req, "roomId");
        if (!roomDAO.deleteRoom(roomId)) {
            throw new IllegalArgumentException("Room not found.");
        }
        return "Room deleted.";
    }

    private String updatePrice(HttpServletRequest req) throws SQLException {
        long typeId = requireId(req, "typeId");
        BigDecimal price;
        try {
            price = new BigDecimal(req.getParameter("pricePerNight").trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Price must be a number.");
        }
        if (!InputValidator.isNonNegativeMoney(price)) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (!roomDAO.updateRoomTypePrice(typeId, price)) {
            throw new IllegalArgumentException("Room type not found.");
        }
        return "Price updated.";
    }

    // ---------- validation helpers ----------

    private long requireId(HttpServletRequest req, String name) {
        Long id = InputValidator.parsePositiveId(req.getParameter(name));
        if (id == null) throw new IllegalArgumentException(name + " must be a positive number.");
        return id;
    }

    private String requireRoomNumber(HttpServletRequest req) {
        String value = req.getParameter("roomNumber");
        if (value == null || value.isBlank() || value.trim().length() > 20) {
            throw new IllegalArgumentException("Room number is required (max 20 characters).");
        }
        return value.trim();
    }

    private int requireFloor(HttpServletRequest req) {
        try {
            int floor = Integer.parseInt(req.getParameter("floor").trim());
            if (floor < 1) throw new IllegalArgumentException("Floor must be 1 or higher.");
            return floor;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Floor must be a number.");
        }
    }

    // ---------- page helpers ----------

    private RoomRow findRoom(long roomId) {
        try {
            for (RoomRow r : roomDAO.findAllRooms()) {
                if (r.roomId() == roomId) return r;
            }
        } catch (SQLException e) {
            // treated as "not found"
        }
        return null;
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("rooms", roomDAO.findAllRooms());
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load rooms.");
        }
        request.getRequestDispatcher(LIST_JSP).forward(request, response);
    }
}
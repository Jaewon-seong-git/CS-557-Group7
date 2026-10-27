package servlet;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import util.InputValidator;

@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String rawPassword = request.getParameter("password");
        if (!InputValidator.isValidName(name) || !InputValidator.isValidEmail(email)
                || !InputValidator.isValidPhone(phone) || rawPassword == null || rawPassword.length() < 8) {
            request.setAttribute("errorMessage", "Check your name, email, phone, and password (8+ characters).");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }
        char[] password = rawPassword.toCharArray();
        try {
            userDAO.registerGuest(name, email, phone, password);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Registration could not be completed.", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}

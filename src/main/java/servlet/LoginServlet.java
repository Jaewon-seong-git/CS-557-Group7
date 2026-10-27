package servlet;

import dao.UserDAO;
import dao.UserDAO.Credentials;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import util.InputValidator;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String submittedPassword = request.getParameter("password");
        if (!InputValidator.isValidEmail(email) || submittedPassword == null || submittedPassword.isEmpty()) {
            showLoginError(request, response);
            return;
        }

        char[] password = submittedPassword.toCharArray();
        try {
            Credentials user = userDAO.findCredentialsByEmail(email.trim());
            if (user == null || !UserDAO.verifyPassword(password, user.passwordHash())) {
                showLoginError(request, response);
                return;
            }
            String role = user.role() == null ? "" : user.role().toUpperCase(Locale.ROOT);
            if (!"ADMIN".equals(role) && !"GUEST".equals(role)) {
                showLoginError(request, response);
                return;
            }
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("user_id", user.userId());
            session.setAttribute("role", role);
            session.setAttribute("email", user.email());

            String destination = "ADMIN".equals(role) ? "/admin/dashboard" : "/guest/dashboard";
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + destination));
        } catch (SQLException e) {
            throw new ServletException("Login could not be completed.", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void showLoginError(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", "Email or password is incorrect.");
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }
}

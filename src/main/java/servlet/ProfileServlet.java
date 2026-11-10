package servlet;

import dao.UserDAO;
import dao.UserDAO.GuestProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import util.InputValidator;

@WebServlet("/guest/profile")
public class ProfileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final UserDAO userDAO = new UserDAO();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Long userId = getGuestId(request, response);
		if (userId == null)
			return;

		try {
			GuestProfile profile = userDAO.findGuestProfileById(userId);

			if (profile == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Guest profile was not found.");
				return;
			}

			request.setAttribute("profile", profile);

			HttpSession session = request.getSession(false);
			Object successMessage = session.getAttribute("profileSuccess");
			if (successMessage != null) {
				request.setAttribute("successMessage", successMessage);
				session.removeAttribute("profileSuccess");
			}

			showProfile(request, response);
		} catch (SQLException e) {
			getServletContext().log("Could not load guest profile.", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Your profile could not be loaded. Please try again later.");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		Long userId = getGuestId(request, response);
		if (userId == null)
			return;

		String name = trim(request.getParameter("name"));
		String email = trim(request.getParameter("email"));
		String phone = trim(request.getParameter("phone"));

		// Preserve submitted values if validation or saving fails.
		request.setAttribute("profile", new GuestProfile(userId, name, email, phone));

		if (!InputValidator.isValidName(name)) {
			request.setAttribute("errorMessage", "Enter a name between 1 and 100 characters.");
		} else if (!InputValidator.isValidEmail(email)) {
			request.setAttribute("errorMessage", "Enter a valid email address, up to 254 characters.");
		} else if (!InputValidator.isValidPhone(phone)) {
			request.setAttribute("errorMessage", "Enter a valid phone number, or leave it blank.");
		} else {
			try {
				GuestProfile existing = userDAO.findGuestProfileById(userId);

				if (existing == null) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Guest profile was not found.");
					return;
				}

				// An unchanged form is also a successful save.
				boolean unchanged = name.equals(existing.name()) && email.equals(existing.email())
						&& phone.equals(trim(existing.phone()));

				if (!unchanged && !userDAO.updateProfile(userId, name, email, phone)) {
					request.setAttribute("errorMessage", "Your profile could not be updated. Please try again.");
				} else {
					HttpSession session = request.getSession(false);
					session.setAttribute("email", email);
					session.setAttribute("profileSuccess", "Your profile has been updated.");

					response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/guest/profile"));
					return;
				}
			} catch (SQLException e) {
				getServletContext().log("Could not update guest profile.", e);

				String message = e.getErrorCode() == 1062 ? "That email address is already in use."
						: "Your profile could not be saved. Please try again later.";

				request.setAttribute("errorMessage", message);
			}
		}

		showProfile(request, response);
	}

	private Long getGuestId(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);

		if (session == null || !(session.getAttribute("user_id") instanceof Long)) {
			response.sendRedirect(request.getContextPath() + "/login");
			return null;
		}

		if (!"GUEST".equals(session.getAttribute("role"))) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}

		Long userId = (Long) session.getAttribute("user_id");
		if (userId <= 0) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}

		return userId;
	}

	private void showProfile(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.getRequestDispatcher("/WEB-INF/views/profile_edit.jsp").forward(request, response);
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
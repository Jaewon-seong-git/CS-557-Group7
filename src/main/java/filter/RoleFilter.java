package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;

/** Protects the agreed /admin/* and /guest/* route families. */
@WebFilter(urlPatterns = {"/admin/*", "/guest/*"})
public class RoleFilter implements Filter {
    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        String requiredRole = httpRequest.getRequestURI().contains("/admin/") ? "ADMIN" : "GUEST";
        String role = session == null ? null : (String) session.getAttribute("role");
        if (role == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        String normalized = role.toUpperCase(Locale.ROOT);
        if ("ADMINISTRATOR".equals(normalized)) normalized = "ADMIN";
        if (!requiredRole.equals(normalized)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}

package it.polimi.tiw.image_manager.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CheckUserLogged implements Filter {
	
	public CheckUserLogged() {}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		HttpSession session = req.getSession();
		if(session != null) {
			if(session.isNew() || session.getAttribute("user") == null) {
				// if the user is not logged, redirect to login page
				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().println("Login before accessing the page!");
				return;
			}
		}
		chain.doFilter(req, res);
	}
	
	@Override
	public void destroy() {}
}

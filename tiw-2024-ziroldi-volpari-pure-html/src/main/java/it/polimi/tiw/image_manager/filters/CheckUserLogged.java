package it.polimi.tiw.image_manager.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import it.polimi.tiw.image_manager.utils.TemplateHandler;

public class CheckUserLogged implements Filter {
	TemplateEngine templateEngine = null;
	
	public CheckUserLogged() {}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		ServletContext servletContext = filterConfig.getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		HttpSession session = req.getSession();
		if(session != null) {
			if(session.isNew() || session.getAttribute("user") == null) {
				// if the user is not logged, redirect to login page
				String loginPath = "/login.html";
				ServletContext servletContext = req.getServletContext();
				final WebContext ctx = new WebContext(req, res, servletContext, request.getLocale());
				ctx.setVariable("errorMsgLogin", "Login before accessing the page!");
				templateEngine.process(loginPath, ctx, response.getWriter());		
				return;
			}
		}
		chain.doFilter(req, res);
	}
	
	@Override
	public void destroy() {}
}

package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.UserDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	TemplateEngine templateEngine;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		this.connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// if a user was logged, logout
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		
		String loginPath = "/login.html";
		
		// get parameters
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		
		// check data
		if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
			String errorMsg = "Missing or empty credential values";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgLogin", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
	
		// check DB to authenticate 
		UserDAO userDao = new UserDAO(connection);
		User user = null;
		try {
			user = userDao.checkCredentials(username, password);
		}catch (SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}

		if (user == null) {
			// credentials are wrong, redirect to login page
			String errorMsg = "Wrong credentials!";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgLogin", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		} else {
			// right credentials, redirect to home page
			request.getSession().setAttribute("user", user);
			// GoToHomePage will set albums
			String homePath = getServletContext().getContextPath() + "/GoToHomePage";
			response.sendRedirect(homePath);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
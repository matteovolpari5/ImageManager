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

import org.apache.commons.validator.routines.EmailValidator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.UserDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/CheckSignup")
public class CheckSignup extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	String imagesPath;
	TemplateEngine templateEngine;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		this.connection = ConnectionHandler.getConnection(servletContext);
		imagesPath = servletContext.getInitParameter("imagesPath");
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
		
		// get data
		String username = request.getParameter("username");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		String repeatPassword = request.getParameter("repeatPassword");	
	 	
		// check data
		if (	username == null || email == null || password == null || repeatPassword == null || 
				username.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {	
			String errorMsg = "Missing or empty credential values";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgSignup", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
		
		if(!password.equals(repeatPassword)) {
			String errorMsg = "Different values for password and repeat password.";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgSignup", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
		
		// check email format
		if(!EmailValidator.getInstance().isValid(email)) {
			String errorMsg = "Email has wrong format!";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgSignup", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
		
		UserDAO userDAO = new UserDAO(connection);
		User user = null;
		
		// check email unique
		try {
			user = userDAO.getUserByEmail(email);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase");
			return;
		}
		if(user != null) {
			String errorMsg = "Email already in use";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgSignup", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
		
		// check username unique
		try {
			user = userDAO.getUserByUsername(username);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase");
			return;
		}
		if(user != null) {
			String errorMsg = "Username already in use";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsgSignup", errorMsg);
			templateEngine.process(loginPath, ctx, response.getWriter());
			return;
		}
		
		try {
			user = userDAO.registerUser(username, email, password);
			if(user == null)
				throw new RuntimeException();
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase");
			return;
		}
			
		response.sendRedirect(getServletContext().getContextPath()+ loginPath);
	}
	
	@Override
	protected void doGet (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.validator.routines.EmailValidator;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.UserDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/CheckSignup")
@MultipartConfig
public class CheckSignup extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	String imagesPath;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.connection = ConnectionHandler.getConnection(servletContext);
		imagesPath = servletContext.getInitParameter("imagesPath");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		request.setCharacterEncoding("UTF-8");
		
		// if a user was logged, logout
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
				
		// get data
		String username = request.getParameter("username");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		String repeatPassword = request.getParameter("repeatPassword");	
	 	
		// check data
		if (	username == null || email == null || password == null || repeatPassword == null || 
				username.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {	
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or empty credential values.");
			return;
		}
		
		// check repeat password
		if(!password.equals(repeatPassword)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Different values for password and repeat password.");
			return;
		}
		
		// check email format
		if(!EmailValidator.getInstance().isValid(email)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Email has wrong format.");
			return;
		}
		
		UserDAO userDAO = new UserDAO(connection);
		User user = null;
		
		// check email unique
		try {
			user = userDAO.getUserByEmail(email);
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		if(user != null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println("Email already in use.");
			return;
		}
		
		// check username unique
		try {
			user = userDAO.getUserByUsername(username);
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		if(user != null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println("Username already in use.");
			return;
		}
		
		try {
			user = userDAO.registerUser(username, email, password);
			if(user == null)
				throw new RuntimeException();
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
				
		request.getSession().setAttribute("user", user);
		response.setStatus(HttpServletResponse.SC_OK);

		
		response.setContentType("application/json");		// TODO ?
		response.setCharacterEncoding("UTF-8");		// TODO ?	
		response.getWriter().println(username);			// TODO ????? cosa ritorno???
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

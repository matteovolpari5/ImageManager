package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.UserDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/CheckLogin")
@MultipartConfig
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.connection = ConnectionHandler.getConnection(servletContext);
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
				
		// get parameters
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		
		// check data
		if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or empty credential values.");
			return;
		}
		
		// check DB to authenticate 
		UserDAO userDao = new UserDAO(connection);
		User user = null;
		try {
			user = userDao.checkCredentials(username, password);
		}catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}

		if (user == null) {
			// wrong credentials
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println("Wrong credentials.");
			return;
		} else {
			// right credentials
			request.getSession().setAttribute("user", user);
			response.setStatus(HttpServletResponse.SC_OK);
		}
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
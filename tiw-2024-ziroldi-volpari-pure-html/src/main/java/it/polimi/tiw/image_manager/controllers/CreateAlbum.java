package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/CreateAlbum")
public class CreateAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	TemplateEngine templateEngine;

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
				
		// get album title
		String albumTitle = request.getParameter("title");
		
		if(albumTitle == null || albumTitle.isEmpty()) {
			ErrorHandler.displayError(request, response, "Missing or empty title.");
			return;
		}
					
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		AlbumDAO albumDAO = new AlbumDAO(connection);
		
		// check if an album with the same name already exists
		List<Album> userAlbums = null;
		try {
			userAlbums = albumDAO.findAlbumsByUserSorted(user.getId());
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		for(Album a: userAlbums) {
			if(a.getTitle().equals(albumTitle)) {
				ErrorHandler.displayError(request, response, "You can't create two albums with the same title.");
				return;
			}
		}

		// if the album title is new for the user, create album
		try {
			albumDAO.createAlbum(albumTitle, user.getId()); 
		} catch (SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}

		String homePath = getServletContext().getContextPath() + "/GoToHomePage";
		response.sendRedirect(homePath);
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

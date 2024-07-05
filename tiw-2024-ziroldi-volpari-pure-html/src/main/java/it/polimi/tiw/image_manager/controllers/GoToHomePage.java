package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/GoToHomePage")
public class GoToHomePage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private TemplateEngine templateEngine;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get albums
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		AlbumDAO albumDAO = new AlbumDAO(connection);
		List<Album> userAlbums = new ArrayList<Album>();
		List<Album> otherAlbums = new ArrayList<Album>();
		
		// user's albums
		try {
			userAlbums = albumDAO.findAlbumsByUserSorted(user.getId());
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}

		// albums of others
		try {
			otherAlbums = albumDAO.findAlbumsOfOthersSorted(user.getId());
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		
		String homePath = "/WEB-INF/home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("userAlbums", userAlbums);
		ctx.setVariable("otherAlbums", otherAlbums);
		templateEngine.process(homePath, ctx, response.getWriter());
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
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

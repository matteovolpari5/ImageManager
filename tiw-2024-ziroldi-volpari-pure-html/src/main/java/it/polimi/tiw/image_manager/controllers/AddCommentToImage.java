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

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.CommentDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/AddCommentToImage")
public class AddCommentToImage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection connection = null;
	TemplateEngine templateEngine = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
	
		// get image id and content
		Integer albumId = null;
		Integer imageId = null;
		String content = null;
		try {
			imageId = Integer.parseInt(request.getParameter("imageId"));
			albumId = Integer.parseInt(request.getParameter("albumId"));
		}catch (NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Incorrect param values");
			return;
		}
		content = request.getParameter("content");
		// check non empty content
		if(content == null || content.isEmpty()) {
			ErrorHandler.displayError(request, response, "The comment cannot be empty.");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
				return;
			}
			// check existing image
			if(imageDAO.findImageById(imageId) == null) {
				ErrorHandler.displayError(request, response, "The image was not found.");
				return;
			}
			// check album contains image
			if(!imageDAO.albumContainsImage(albumId, imageId)) {
				ErrorHandler.displayError(request, response, "The album doesn't contain the image.");
				return;
			}
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
					
		// add comment to DB
		CommentDAO commentDAO = new CommentDAO(connection);
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		int userId = user.getId();
		try {
			commentDAO.addComment(content, userId, imageId);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
			
		String imagePath = getServletContext().getContextPath() + "/GoToImagePage?albumId=" + albumId + "&imageId=" + imageId;
		response.sendRedirect(imagePath);
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
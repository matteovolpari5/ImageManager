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

import org.thymeleaf.TemplateEngine;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/AddExistingImageToAlbum")
public class AddExistingImageToAlbum extends HttpServlet {
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
		
		// get request data 
		Integer oldAlbumId = null;
		Integer newAlbumId = null;
		Integer imageId = null;
		try {
			oldAlbumId = Integer.parseInt(request.getParameter("oldAlbumId"));
			newAlbumId = Integer.parseInt(request.getParameter("newAlbumId"));
			imageId = Integer.parseInt(request.getParameter("imageId"));
		}catch(NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Inorrect param values.");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		User user = (User) request.getSession().getAttribute("user");
		try {
			// check existing old album
			if(albumDAO.findAlbumById(oldAlbumId) == null) {
				ErrorHandler.displayError(request, response, "The source album was not found.");
				return;
			}
			// check existing new album
			if(albumDAO.findAlbumById(newAlbumId) == null) {
				ErrorHandler.displayError(request, response, "The destination album was not found.");
				return;
			}
			// check existing image
			if(imageDAO.findImageById(imageId) == null) {
				ErrorHandler.displayError(request, response, "The image was not found.");
				return;
			}
			// check album property of new album
			if(user.getId()!= albumDAO.getUserIdByAlbumId(newAlbumId)) {
				ErrorHandler.displayError(request, response, "You can't upload images to the album of another user.");
				return;
			}
			// check image property (and old album property)
			if(user.getId()!= imageDAO.getUserIdByImageId(imageId)) {
				ErrorHandler.displayError(request, response, "You can't upload other users' images to your album.");
				return;	
			}
			// check old album contains image
			if(!imageDAO.albumContainsImage(oldAlbumId, imageId)) {
				ErrorHandler.displayError(request, response, "The provided album does not contain the image.");
				return;
			}
			// check new album does not contain image
			if(imageDAO.albumContainsImage(newAlbumId, imageId)) {
				ErrorHandler.displayError(request, response, "The new album already contains the image.");
				return;
			}
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		
		// add image to album
		try {
			imageDAO.addExistingImageToAlbum(newAlbumId, imageId);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		
		// redirect will update the list of other albums
		String imagePath = getServletContext().getContextPath() + "/GoToImagePage?albumId=" + oldAlbumId + "&imageId=" + imageId;
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

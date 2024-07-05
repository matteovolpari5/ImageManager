package it.polimi.tiw.image_manager.controllers;

import java.io.File;
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

import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;

@WebServlet("/DeleteImageFromAlbum")
public class DeleteImageFromAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get album id and image id
		HttpSession session = request.getSession();
		Integer albumId = null;
		Integer imageId = null;
		try {
			albumId = Integer.parseInt(request.getParameter("albumId"));
			imageId = Integer.parseInt(request.getParameter("imageId"));
		}catch(NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Wrong input format.");
			return;
		}
		
		// check data 
		AlbumDAO albumDAO = new AlbumDAO(connection);
		User user = (User) session.getAttribute("user");
		ImageDAO imageDAO = new ImageDAO(connection);
		Image image = null;
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
				return;
			}
			// check album property
			if(user.getId()!= albumDAO.getUserIdByAlbumId(albumId)) {
				ErrorHandler.displayError(request, response, "You can't delete images from other users' albums");
				return;
			}
			// check existing image
			image = imageDAO.findImageById(imageId);
			if(image == null) {
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
		
		// delete image from album
		String imagePath = image.getFilePath();
		boolean imageDeleted;
		try {
			imageDeleted = imageDAO.deleteImageFromAlbum(albumId, imageId);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		if(imageDeleted) {
			// delete image from file system
			File file = new File(imagePath);
			try {
				file.delete();
			}catch(Exception e) {}
		}
		
		String albumPath = getServletContext().getContextPath() + "/GoToAlbumPage?albumId=" + albumId; 
		response.sendRedirect(albumPath);
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
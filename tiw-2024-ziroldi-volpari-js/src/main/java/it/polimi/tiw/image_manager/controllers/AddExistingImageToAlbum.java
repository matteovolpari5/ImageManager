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

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/AddExistingImageToAlbum")
@MultipartConfig
public class AddExistingImageToAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection connection = null;

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get request data 
		Integer oldAlbumId = null;
		Integer imageId = null;
		Integer newAlbumId = null;
		try {
			oldAlbumId = Integer.parseInt(request.getParameter("oldAlbumId"));
			imageId = Integer.parseInt(request.getParameter("imageId"));
			newAlbumId = Integer.parseInt(request.getParameter("newAlbumId"));
		}catch(NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect param values.");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		User user = (User) request.getSession().getAttribute("user");
		try {
			// check existing old album
			if(albumDAO.findAlbumById(oldAlbumId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The source album was not found.");
				return;
			}
			// check existing new album
			if(albumDAO.findAlbumById(newAlbumId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The destination album was not found.");
				return;
			}
			// check existing image
			if(imageDAO.findImageById(imageId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The image was not found.");
				return;
			}
			// check album property of new album
			if(user.getId()!= albumDAO.getUserIdByAlbumId(newAlbumId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("You can't upload images to the album of another user.");
				return;
			}
			// check image property (and old album property)
			if(user.getId()!= imageDAO.getUserIdByImageId(imageId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("You can't upload other users' images to your album.");
				return;
			}
			// check old album contains image
			if(!imageDAO.albumContainsImage(oldAlbumId, imageId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The provided album does not contain the image.");
				return;
			}
			// check new album does not contain image
			if(imageDAO.albumContainsImage(newAlbumId, imageId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The new album already contains the image.");
				return;
			}
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		
		// add image to album
		try {
			imageDAO.addExistingImageToAlbum(newAlbumId, imageId);
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
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

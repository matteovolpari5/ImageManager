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
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.CommentDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/AddCommentToImage")
@MultipartConfig
public class AddCommentToImage extends HttpServlet {
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
		
		request.setCharacterEncoding("UTF-8");
		
		// get image id and content
		Integer albumId = null;
		Integer imageId = null;
		String content = null;
		try {
			imageId = Integer.parseInt(request.getParameter("imageId"));
			albumId = Integer.parseInt(request.getParameter("albumId"));
		}catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or empty parameters.");
			return;
		}
		
		content = request.getParameter("content");
		// check non empty content
		if(content == null || content.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("The comment cannot be empty.");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The album was not found.");
				return;
			}
			// check existing image
			if(imageDAO.findImageById(imageId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The image was not found.");
				return;
			}
			// check album contains image
			if(!imageDAO.albumContainsImage(albumId, imageId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Provided album doesn't contain provided image.");
				return;
			}
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
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
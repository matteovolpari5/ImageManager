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
import it.polimi.tiw.image_manager.beans.Comment;
import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.CommentDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/GoToImagePage")
public class GoToImagePage extends HttpServlet {
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
		
		// get album id and image id
		Integer albumId = null;
		Integer imageId = null;
		try {
			albumId = Integer.parseInt(request.getParameter("albumId"));
			imageId = Integer.parseInt(request.getParameter("imageId"));
		}catch (NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Incorrect param values");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		Image image = null;
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
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
			ErrorHandler.displayError(request, response,"Error while interacting with DataBase.");
			return;
		}
		
		// check album property
		boolean userProperty = false;
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		int userId;
		try {
			userId = albumDAO.getUserIdByAlbumId(albumId);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response,"Error while interacting with DataBase.");
			return;
		}
		if(userId == -1) {
			// no album found with the given id
			ErrorHandler.displayError(request, response, "The album was not found.");
			return;
		}else if(user.getId() == userId) {
			userProperty = true;
		}
		
		// get image's comments
		CommentDAO commentDAO = new CommentDAO(connection);
		List<Comment> comments = null;
		List<String> commentUser = null;
		try {
			comments = commentDAO.findCommentsByImageId(imageId);
			// if there are no comments, returns an empty list
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
			
		commentUser = new ArrayList<>();
		if(comments.size() > 0) {
			for (Comment c: comments) {
				try {
					commentUser.add(commentDAO.getUsernameByCommentId(c.getId()));
				}catch(SQLException e) {
					ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
					return;
				}
			}
			// if there are no comments, return an empty list
		}
		
		// get other user's albums	
		List<Album> userAlbums = null;
		List<Album> otherUserAlbums = null;
		if(userProperty) {
			try {
				userAlbums = albumDAO.findAlbumsByUserSorted(userId);
			}catch(SQLException e) {
				ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
				return;
			}
			otherUserAlbums = new ArrayList<>();
			for(Album album: userAlbums) {
				boolean contains;
				try {
					contains = imageDAO.albumContainsImage(album.getId(), imageId);
				}catch(SQLException e) {
					ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
					return;
				}
				if(album.getId() != albumId && !contains) {
					otherUserAlbums.add(album);
				}
			}
		}
		
		String imagePath = "/WEB-INF/image.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("image", image);
		ctx.setVariable("albumId", albumId);
		ctx.setVariable("otherUserAlbums", otherUserAlbums);
		ctx.setVariable("userProperty", userProperty);
		ctx.setVariable("comments", comments);
		ctx.setVariable("commentUser", commentUser);
		templateEngine.process(imagePath, ctx, response.getWriter());
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

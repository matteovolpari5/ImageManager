package it.polimi.tiw.image_manager.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.Comment;
import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.CommentDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/GetImageData")
@MultipartConfig
public class GetImageData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get data
		Integer imageId = null;
		Integer albumId = null;
		try {
			imageId = Integer.parseInt(request.getParameter("imageId"));
			albumId = Integer.parseInt(request.getParameter("albumId"));
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect input parameters.");
			return;
		}
		
		// check data
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		Image image = null;
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The album was not found.");
				return;
			}
			// check existing image
			image = imageDAO.findImageById(imageId);
			if(image == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The image was not found.");
				return;
			}
			// check album contains image
			if(!imageDAO.albumContainsImage(albumId, imageId)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The album doesn't contain the image.");
				return;
			}
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Error while interacting with DataBase.");
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
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		if(userId == -1) {
			// no album found with the given id
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("The album was not found.");
			return;
		}else if(user.getId() == userId) {
			userProperty = true;
		}
			
		// get image content string
		String filePath = image.getFilePath();
		File file = new File(filePath);
		if (!file.exists() || file.isDirectory()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while retrieving images.");
			return;
		}
		String imageString = "data:image/" + FilenameUtils.getExtension(filePath) + ";base64," + Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
	
		// get comments
		CommentDAO commentDAO =  new CommentDAO(connection);
		List<Comment> comments = null;
		List<String> commentUser = null;
		try {
			comments = commentDAO.findCommentsByImageId(imageId);
			// if there are no comments, returns an empty list
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		commentUser = new ArrayList<>();
		// if there are no comments, return an empty list
		if(comments.size() > 0) {
			for (Comment c: comments) {
				try {
					commentUser.add(commentDAO.getUsernameByCommentId(c.getId()));
				}catch(SQLException e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("Error while interacting with DataBase.");
					return;
				}
			}
		}
		
		// if the album is of user's property, get other user's albums
		List<Album> userAlbums = null;
		List<Album> otherUserAlbums = null;
		if(userProperty) {
			// get user's albums
			try {
				userAlbums = albumDAO.findAlbumsByUserSorted(userId);
			}catch(SQLException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Error while interacting with DataBase.");
				return;
			}
			// get user's albums without this image 
			otherUserAlbums = new ArrayList<>();
			for(Album album: userAlbums) {
				boolean contains;
				try {
					contains = imageDAO.albumContainsImage(album.getId(), imageId);
				}catch(SQLException e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("Error while interacting with DataBase.");
					return;
				}
				if(album.getId() != albumId && !contains) {
					otherUserAlbums.add(album);
				}
			}
		}
		
		String imageBeanJson = new Gson().toJson(image);
		String imageContentJson = new Gson().toJson(imageString);
		String commentsJson = new Gson().toJson(comments);
		String commentUserJson = new Gson().toJson(commentUser);
		String userPropertyJson = new Gson().toJson(userProperty);
		String otherAlbums = new Gson().toJson(otherUserAlbums);
		
		String imageJson = "["+imageBeanJson+","+imageContentJson+ ","+commentsJson +","+commentUserJson+","+userPropertyJson+","+otherAlbums+"]";
		
		// returns image's information and comments
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(imageJson);
		response.setStatus(HttpServletResponse.SC_OK);
	}
}

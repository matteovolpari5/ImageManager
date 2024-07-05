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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/GetAlbumImagesData")
@MultipartConfig
public class GetAlbumImagesData extends HttpServlet {
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
		Integer albumId = null;
		try {
			albumId = Integer.parseInt(request.getParameter("albumId"));
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect input parameters.");
			return;
		}
		
		// check data 
		AlbumDAO albumDAO = new AlbumDAO(connection);
		Album album = null; 
		try {
			// check existing album
			album = albumDAO.findAlbumById(albumId);
			if(album == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The album was not found.");
				return;
			}
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		
		// get images from album
		ImageDAO imageDAO = new ImageDAO(connection);
		User user = (User) request.getSession().getAttribute("user");
		List<Image> images = null;
		try {
			images = imageDAO.findImagesForAlbumActualSort(albumId, user.getId());
		} catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		
		// get images content
		List<String> stringifiedImages = new ArrayList<>();
		for(Image image: images) {
			String filePath = image.getFilePath();
			File file = new File(filePath);
			if (!file.exists() || file.isDirectory()) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Error while retrieving images.");
				return;
			}
			String imageString = "data:image/" + FilenameUtils.getExtension(filePath) + ";base64," + Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
			stringifiedImages.add(imageString);
		}
		
		String imagesBeanJson = new Gson().toJson(images);
		String imagesContentJson = new Gson().toJson(stringifiedImages);
		String imagesJson = "["+imagesBeanJson+","+imagesContentJson+"]";

		// returns all images and files in the album
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(imagesJson);
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

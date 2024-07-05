package it.polimi.tiw.image_manager.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.thymeleaf.TemplateEngine;

import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;

@WebServlet("/AddNewImageToAlbum")
@MultipartConfig
public class AddNewImageToAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection connection = null;
	TemplateEngine templateEngine = null;
	
	String imagesPath;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
		imagesPath = servletContext.getInitParameter("imagesPath");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get image data
		Integer albumId = null;
		String title = null;
		String description = null;
		
		try {
			albumId = Integer.parseInt(request.getParameter("albumId"));
		}catch(NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Incorrect param values.");
			return;
		}
		title = request.getParameter("title");
		description = request.getParameter("description");
		
		// check data
		if( title == null || title.isEmpty() ||
			description == null || description.isEmpty()) {
			ErrorHandler.displayError(request, response, "Incorrect param values.");
			return;
		}
		AlbumDAO albumDAO = new AlbumDAO(connection);
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
				return;
			}
			// check album property
			if(user.getId()!= albumDAO.getUserIdByAlbumId(albumId)) {
				ErrorHandler.displayError(request, response, "You can't add images to other users' albums");
				return;	
			}
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		
		// get image
		Part image;
		try {
			image = request.getPart("image");
			if (image == null || image.getSize() <= 0) {
				throw new Exception();
			}
		}catch(Exception e) {
			ErrorHandler.displayError(request, response, "Missing image");
			return;
		}
	
		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image")) {
			ErrorHandler.displayError(request, response, "File format not permitted");
			return;
		}
		
		// compute output path
		String fileName = Paths.get(image.getSubmittedFileName()).getFileName().toString();
		String outputPath = imagesPath + fileName;
		File file = new File(outputPath);
		if(file.exists()) {
			String baseName = FilenameUtils.getBaseName(fileName);
			String extension = FilenameUtils.getExtension(fileName);
			int copy = 1;
			do {
				fileName = baseName + "_" + copy + "." + extension;
				outputPath = imagesPath + fileName;
				file = new File(outputPath);
				copy++;
			}while(file.exists());
		}
		
		// save the image on the server 
		try (InputStream fileContent = image.getInputStream()) {
			Files.copy(fileContent, file.toPath());
		}catch(Exception e) {
			ErrorHandler.displayError(request, response, "Error while saving file");
			return;
		}
		
		// add image to DB
		ImageDAO imageDAO = new ImageDAO(connection);
		try { 
			imageDAO.addNewImageToAlbum(title, description, outputPath, albumId);
		}catch(SQLException e) {
			// remove image from file system
			try {
				file.delete();
			}catch(Exception e1) {}
			ErrorHandler.displayError(request, response, "Not possible to add image");
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

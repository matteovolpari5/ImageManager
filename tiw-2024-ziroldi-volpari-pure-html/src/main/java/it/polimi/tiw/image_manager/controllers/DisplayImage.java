package it.polimi.tiw.image_manager.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;

import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/DisplayImage")
public class DisplayImage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	TemplateEngine templateEngine = null;
	Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get file path
		Integer imageId = null;
		try {
			imageId = Integer.parseInt(request.getParameter("imageId"));
		}catch(NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Wrong input format.");
			return;
		}
		
		// check data 
		ImageDAO imageDAO = new ImageDAO(connection);
		Image image = null;
		try {
			// check existing image
			image = imageDAO.findImageById(imageId);
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		if(image == null) {
			ErrorHandler.displayError(request, response, "The image was not found.");
			return;
		}
		
		String filePath = image.getFilePath();
		File file = new File(filePath);
		if (!file.exists() || file.isDirectory()) {
			ErrorHandler.displayError(request, response, "The file does not exist");
			return;
		} else {	
			String fileName = file.getName();
			if(fileName == null || fileName.isEmpty()) {
				ErrorHandler.displayError(request, response, "Empty file name");
				return;
			}
			response.setHeader("Content-Type", getServletContext().getMimeType(fileName));
			response.setHeader("Content-Length", String.valueOf(file.length()));
			response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
			Files.copy(file.toPath(), response.getOutputStream());
		}
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

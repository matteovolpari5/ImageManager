package it.polimi.tiw.image_manager.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/GoToAlbumPage")
public class GoToAlbumPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private TemplateEngine templateEngine;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
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
			ErrorHandler.displayError(request, response, "Incorrect input parameters.");
			return;
		}
		
		// check data 
		AlbumDAO albumDAO = new AlbumDAO(connection);
		Album album = null; 
		try {
			// check existing album
			album = albumDAO.findAlbumById(albumId);
			if(album == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
				return;
			}
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
		
		// get images from an album
		ImageDAO imageDAO = new ImageDAO(connection);
		List<Image> images = null;
		try {
			images = imageDAO.findImagesForAlbum(albumId);
		} catch (SQLException e) {
			ErrorHandler.displayError(request, response, "Error while interacting with DataBase.");
			return;
		}
			
		// get only first five (or less) images
		boolean hasNext = false;
		int maxImages = 5;
		while(images.size() > maxImages) {
			images.remove(images.size()-1);
			// if I have removed images, there are others
			hasNext = true;
		}
			
		String albumPath = "/WEB-INF/album.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("currImages", images);
		ctx.setVariable("hasPrevious", false);
		ctx.setVariable("hasNext", hasNext);
		ctx.setVariable("albumId", albumId);
		ctx.setVariable("pageNumber", 1);
		templateEngine.process(albumPath, ctx, response.getWriter());
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

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

import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;
import it.polimi.tiw.image_manager.utils.ErrorHandler;
import it.polimi.tiw.image_manager.utils.TemplateHandler;

@WebServlet("/ShowNextImages")
public class ShowNextImages extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	TemplateEngine templateEngine = null;
	Connection connection = null;
	private final static int maxImages = 5;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext);
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get album and last showed image id
		Integer albumId = null;
		Integer pageNumber = null;
		try {
			albumId = Integer.parseInt(request.getParameter("albumId"));
			pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
		}catch(NumberFormatException | NullPointerException e) {
			ErrorHandler.displayError(request, response, "Wrong input parameters.");
			return;
		}
		
		// check existing album
		AlbumDAO albumDAO = new AlbumDAO(connection);
		try {
			if(albumDAO.findAlbumById(albumId) == null) {
				ErrorHandler.displayError(request, response, "The album was not found.");
				return;
			}
		}catch(SQLException e) {
			ErrorHandler.displayError(request, response,"Error while interacting with DataBase.");
			return;
		}

		// get album's images
		ImageDAO imageDAO = new ImageDAO(connection);
		List<Image> images = null;
		try {
			images = imageDAO.findImagesForAlbum(albumId);
		} catch (SQLException e) {
			ErrorHandler.displayError(request, response,"Error while interacting with DataBase.");
			return;
		}
		assert(images != null);
		
		boolean hasPrevious = false;
		boolean hasNext = false;
		
		// check provided page number
		int numPages = images.size() / maxImages;
		if(images.size() % maxImages != 0) {
			numPages++;
		}
		
		if(pageNumber <= 0 || pageNumber >= numPages) {
			ErrorHandler.displayError(request, response, "Tha page was not found.");
			return;
		}
		// compute hasPrevious and hasNext
		hasPrevious = true;
		if(pageNumber == numPages - 1) {
			hasNext = false;
		}else {
			hasNext = true;
		}
		// select images to show
		for(int i = 0; i < pageNumber * maxImages; i++) {
			images.remove(0);
		}
		while(images.size() > maxImages) {
			images.remove(images.size()-1);
		}
		
		String albumPath = "/WEB-INF/album.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("currImages", images);
		ctx.setVariable("hasPrevious", hasPrevious);
		ctx.setVariable("hasNext", hasNext);
		ctx.setVariable("pageNumber", pageNumber + 1);
		ctx.setVariable("albumId", albumId);
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

package it.polimi.tiw.image_manager.controllers;

import java.io.BufferedReader;
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.dao.ContainsDAO;
import it.polimi.tiw.image_manager.dao.ImageDAO;
import it.polimi.tiw.image_manager.utils.AlbumOrder;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/ChangeImagesOrder")
public class ChangeImagesOrder extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection connection;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// reads text from a character-input stream
        BufferedReader reader = request.getReader();
        // read a line
        String jsonString = reader.readLine();
        if(jsonString == null) {
        	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or empty parameters.");
			return;
        }
         
        // parse JSON string
        Gson gson = new Gson();
        int albumId;
        List<Integer> imageIds = new ArrayList<>();
        // convert to strings array
        try {
        	// convert JSON to AlbumOrder
        	AlbumOrder albumOrder = gson.fromJson(jsonString, AlbumOrder.class);
        	
        	// get album id
        	albumId = Integer.parseInt(albumOrder.getAlbumId());
        	
        	// convert to integer list
            for (String imageId : albumOrder.getImageIds()) {
                imageIds.add(Integer.parseInt(imageId));
            }
        }catch(JsonSyntaxException | NumberFormatException | NullPointerException e) {
        	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Wrong input format.");
			return;
        }
        
        // check data 
        AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		try {
			// check existing album
			if(albumDAO.findAlbumById(albumId) == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("The source album was not found.");
				return;
			}
			
			// for all provided images
			for(int imageId: imageIds) {				
				// check existing image
				if(imageDAO.findImageById(imageId) == null) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().println("The image was not found.");
					return;
				}
				// check album contains image
				if(!imageDAO.albumContainsImage(albumId, imageId)) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().println("The provided album does not contain one of the images.");
					return;
				}
			}
			
			// check all album images received 
			if(imageDAO.findImagesForAlbumDefaultSort(albumId).size() != imageIds.size()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Album contains images that have not been provided.");
				return;
			}
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		  
        // save new order
		int userId = ((User) request.getSession().getAttribute("user")).getId();
        ContainsDAO containsDAO = new ContainsDAO(connection);
        try {
        	containsDAO.changeImagesOrder(userId, albumId, imageIds);
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

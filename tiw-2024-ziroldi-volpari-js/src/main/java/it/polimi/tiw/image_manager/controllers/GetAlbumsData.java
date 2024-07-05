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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import it.polimi.tiw.image_manager.beans.Album;
import it.polimi.tiw.image_manager.beans.User;
import it.polimi.tiw.image_manager.dao.AlbumDAO;
import it.polimi.tiw.image_manager.utils.ConnectionHandler;

@WebServlet("/GetAlbumsData")
public class GetAlbumsData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionHandler.getConnection(servletContext);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// filter checks if the user is logged
		
		// get albums
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		AlbumDAO albumDAO = new AlbumDAO(connection);
		List<Album> userAlbums = new ArrayList<Album>();
		List<Album> otherAlbums = new ArrayList<Album>();
		
		// user's albums
		try {
			userAlbums = albumDAO.findAlbumsByUserSorted(user.getId());
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}

		// albums of others
		try {
			otherAlbums = albumDAO.findAlbumsOfOthersSorted(user.getId());
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error while interacting with DataBase.");
			return;
		}
		
		// create JSON objects
		Gson gson = new GsonBuilder().setDateFormat("yyyy MMM dd").create();	
		String userAlbumsJson = gson.toJson(userAlbums);
		String otherAlbumsJson = gson.toJson(otherAlbums);
		String albumsJson = "["+userAlbumsJson+","+otherAlbumsJson+"]";
		
		// send response
		response.setContentType("application/json"); 
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(albumsJson);
		
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


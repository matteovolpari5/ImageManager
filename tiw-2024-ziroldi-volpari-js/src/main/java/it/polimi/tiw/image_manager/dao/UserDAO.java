package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.image_manager.beans.Image;
import it.polimi.tiw.image_manager.beans.User;

public class UserDAO {
	private Connection connection;
	
	public UserDAO(Connection connection) {
		this.connection = connection;
	}
	
	public User checkCredentials(String usr, String pwd) throws SQLException {
		// usr  stands for username or email
		String queryUsername = "SELECT user_id, username, email, password FROM User WHERE username = ? AND password =  ?";
		String queryEmail = "SELECT user_id, username, email, password FROM User WHERE email = ? AND password =  ?";
		User user = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		// check credentials with username and password
		try {
			preparedStatement = connection.prepareStatement(queryUsername);
			preparedStatement.setString(1, usr);
			preparedStatement.setString(2, pwd);
			resultSet = preparedStatement.executeQuery();
			
			if(resultSet.isBeforeFirst()) {
				// credentials found using username and password
				resultSet.next(); // move to the first result 
				user = new User();
				user.setId(resultSet.getInt("user_id"));
				user.setUsername(resultSet.getString("username"));
				user.setEmail(resultSet.getString("email"));
			}
		}catch(SQLException e) {
			throw new SQLException();
		}finally {
			try {
				if(preparedStatement != null) {
					preparedStatement.close();
					preparedStatement = null;
				}
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(resultSet != null)
					resultSet.close();
					resultSet = null;
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}
		if(user != null) {
			return user;
		}
		
		// if not found with username
		// check credentials with email and password
		try {
			preparedStatement = connection.prepareStatement(queryEmail);
			preparedStatement.setString(1, usr);
			preparedStatement.setString(2, pwd);
			resultSet = preparedStatement.executeQuery();
			
			if(resultSet.isBeforeFirst()) {
				// credentials found using email and password
				resultSet.next(); // move to the first result 
				user = new User();
				user.setId(resultSet.getInt("user_id"));
				user.setUsername(resultSet.getString("username"));
				user.setEmail(resultSet.getString("email"));
			}	
		}catch(SQLException e) {
			throw new SQLException();
		}finally {
			try {
				if(preparedStatement != null) {
					preparedStatement.close();
					preparedStatement = null;
				}
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(resultSet != null)
					resultSet.close();
					resultSet = null;
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}
		return user;	// can be null
	}
	
	public User getUserByUsername(String username) throws SQLException {
		String query = "SELECT user_id, username, email, password FROM User WHERE username = ?";
		User user = null;

		try(PreparedStatement preparedStatement = connection.prepareStatement(query);) {
			preparedStatement.setString(1, username);
			try(ResultSet resultSet = preparedStatement.executeQuery();){
				if(resultSet.isBeforeFirst()) {
					// a user with the given username was found
					resultSet.next(); // move to the first result 
					user = new User();
					user.setId(resultSet.getInt("user_id"));
					user.setUsername(resultSet.getString("username"));
					user.setEmail(resultSet.getString("email"));
					return user;
				}
				else {
					return null;
				}
			}catch(SQLException e) {
				throw new SQLException();
			}
		}catch(SQLException e) {
			throw new SQLException();
		}
	}
	
	public User getUserByEmail(String email) throws SQLException {
		String query = "SELECT user_id, username, email, password FROM User WHERE email = ?";
		User user = null;
		
		try(PreparedStatement preparedStatement = connection.prepareStatement(query);) {
			preparedStatement.setString(1, email);
			try(ResultSet resultSet = preparedStatement.executeQuery();) {
				if(resultSet.isBeforeFirst()) {
					// a user with the given email was found
					resultSet.next(); // move to the first result 
					user = new User();
					user.setId(resultSet.getInt("user_id"));
					user.setUsername(resultSet.getString("username"));
					user.setEmail(resultSet.getString("email"));
					return user;
				}
				else {
					return null;
				}
			}catch(SQLException e) {
				throw new SQLException();
			}
		}catch(SQLException e) {
			throw new SQLException();
		}
	}
	
	public User registerUser(String username, String email, String password) throws SQLException {
		String queryRegister = "INSERT INTO `db_image_manager_js`.`User` (`username`, `email`, `password`) VALUES (?, ?, ?);";
		String queryContains = "INSERT INTO `db_image_manager_js`.`Contains` (`image_id`, `album_id`, `user_id`, `order_id`) VALUES (?, ?, ?, ?);";
		User user = null;
		
		if(getUserByUsername(username) != null || getUserByEmail(email) != null) {
			// (double) check that a user with same username or email does not exist
			throw new SQLException();
		}
		
		PreparedStatement preparedStatementRegister = null;
		PreparedStatement preparedStatementContains = null;
		int affectedRows;
		
		// disable auto-commit
		connection.setAutoCommit(false);
		
		try {
			preparedStatementRegister = connection.prepareStatement(queryRegister);
			preparedStatementContains = connection.prepareStatement(queryContains);
			
			preparedStatementRegister.setString(1, username);
			preparedStatementRegister.setString(2, email);
			preparedStatementRegister.setString(3, password);
			
			affectedRows = preparedStatementRegister.executeUpdate();
			user = getUserByUsername(username);
			
			if(affectedRows == 0 || user == null) {
				throw new SQLException();
			}
			
			// create contains tuples
			
			int userId = user.getId();
			
			AlbumDAO albumDAO = new AlbumDAO(connection);
			ImageDAO imageDAO = new ImageDAO(connection);
			
			List<Integer> albumIds = albumDAO.getAlbumIds();
			for(Integer albumId: albumIds) {

				// get album images sorted for descending date
				List<Image> albumImages = imageDAO.findImagesForAlbumDefaultSort(albumId);
				
				// add tuple
				int order = 1;
				for(Image image: albumImages) {
					//image_id`, `album_id`, `user_id`, `order_id
					preparedStatementContains.setInt(1, image.getId());
					preparedStatementContains.setInt(2, albumId);
					preparedStatementContains.setInt(3, userId);
					preparedStatementContains.setInt(4, order);
					preparedStatementContains.executeUpdate();
					order++;
				}
			}
			
			connection.commit();
			
		}catch(SQLException e) {
			connection.rollback();
			throw new SQLException("Error excuting query");
		}finally {
			try {
				if(preparedStatementRegister != null)
					preparedStatementRegister.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(preparedStatementContains != null)
					preparedStatementContains.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			// enable auto-commit 
			connection.setAutoCommit(true);	
		}
		
		return user;
	}
	
	public List<Integer> getUserIds() throws SQLException {
		String query = "SELECT user_id FROM User";
		
		List<Integer> userIds = new ArrayList<>();
		
		try(PreparedStatement preparedStatement = connection.prepareStatement(query);) {
			try(ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					userIds.add(resultSet.getInt("user_id"));
				}
			}catch(SQLException e) {
				throw new SQLException();
			}
		}catch(SQLException e) {
			throw new SQLException();
		}
		
		return userIds;
	}
}

package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
		String query = "INSERT INTO `db_image_manager_pure_html`.`User` (`username`, `email`, `password`) VALUES (?, ?, ?);";
		
		if(getUserByUsername(username) != null || getUserByEmail(email) != null) {
			// (double) check tåhat a user with same username or email does not exist
			throw new SQLException();
		}
		
		int affectedRows;
		
		try(PreparedStatement preparedStatement = connection.prepareStatement(query);) {
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, email);
			preparedStatement.setString(3, password);
			affectedRows = preparedStatement.executeUpdate(); 
		}catch(SQLException e) {
			throw new SQLException("Error excuting query");
		}
		
		if(affectedRows == 0) {
			throw new SQLException();
		}
		try {
			return getUserByUsername(username);
		}catch(SQLException e) {
			throw new SQLException("Error getting user");
		}
	}
}

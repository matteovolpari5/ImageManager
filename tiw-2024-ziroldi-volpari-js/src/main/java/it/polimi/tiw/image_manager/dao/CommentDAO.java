package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.image_manager.beans.Comment;

public class CommentDAO {
private Connection connection;
	
	public CommentDAO(Connection connection) {
		this.connection = connection;
	}
	
	public List<Comment> findCommentsByImageId(int imageId) throws SQLException{
		List<Comment> comments = new ArrayList<Comment>();
		String query = "SELECT * FROM Comment WHERE image_id =?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, imageId);
			try (ResultSet result = pstatement.executeQuery();) {
				while(result.next()) {
					Comment comment = new Comment();
					comment.setId(result.getInt("comment_id"));
					comment.setContent(result.getString("content"));
					comments.add(comment);
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return comments;
	}
	
	public void addComment(String content, int userId, int imageId) throws SQLException {
		String query = "INSERT INTO `db_image_manager_js`.`Comment` (`content`, `user_id`, `image_id`) VALUES (?, ?, ?);";
		int affectedRows = 0;
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, content);  
			pstatement.setInt(2, userId);
			pstatement.setInt(3, imageId);
			affectedRows = pstatement.executeUpdate();
		}catch(SQLException e) {
			throw new SQLException("Error excuting query");
		}
		if(affectedRows == 0) {
			throw new SQLException("Comment not added");
		}
	}
	
	public String getUsernameByCommentId(int commentId) throws SQLException{
		String queryUserId ="SELECT user_id FROM Comment WHERE comment_id=?";
		String queryUsername="SELECT username FROM User WHERE user_id=?";
		String user = null;
		int userId = -1;
		
		try (PreparedStatement pstatementId = connection.prepareStatement(queryUserId);) {
			pstatementId.setInt(1, commentId);
			try (ResultSet resultId = pstatementId.executeQuery();) {
				while (resultId.next()) {
					userId = resultId.getInt("user_id");
				}
				PreparedStatement pstatementUsername = connection.prepareStatement(queryUsername);
				pstatementUsername.setInt(1,  userId);
				ResultSet resultUser = pstatementUsername.executeQuery();
				while (resultUser.next()) {
					user = resultUser.getString("username");
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return user;
	}
}

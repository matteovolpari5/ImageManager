package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ContainsDAO {
	private Connection connection;
	
	public ContainsDAO(Connection connection) {
		this.connection = connection;
	}
	
	public void changeImagesOrder(int userId, int albumId, List<Integer> imageIds) throws SQLException {
		String query = "UPDATE db_image_manager_js.CONTAINS SET order_id = ? WHERE album_id = ? AND image_id = ? AND user_id = ?;";
		PreparedStatement pStatement = null;
		
		// disable auto-commit
		connection.setAutoCommit(false);
		
		try {
			// prepare statement
			pStatement = connection.prepareStatement(query);
			
			// change order id of every image
			for(int i = 0; i < imageIds.size(); i++) {
				// 1 based order_id
				pStatement.setInt(1, i + 1);
				pStatement.setInt(2, albumId);
				pStatement.setInt(3, imageIds.get(i));
				pStatement.setInt(4, userId);
				
				int affectedRows = pStatement.executeUpdate();
				if(affectedRows <= 0) {
					throw new SQLException();
				}	
			}
			
			// if no exception occurred, commit
			connection.commit();
			
		}catch(SQLException e) {
			connection.rollback();
			throw new SQLException(e);
		}finally {
			try {
				if(pStatement != null)
					pStatement.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			// enable auto-commit 
			connection.setAutoCommit(true);
		}
	}
}

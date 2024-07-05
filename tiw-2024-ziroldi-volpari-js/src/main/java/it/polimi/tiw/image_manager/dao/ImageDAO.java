package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.image_manager.beans.Image;

public class ImageDAO {
private Connection connection;
	
	public ImageDAO(Connection connection) {
		this.connection = connection;
	}
	
	public void addNewImageToAlbum(String title, String description, String imagePath, int albumId) throws SQLException {
		String queryImage = "INSERT INTO `db_image_manager_js`.`Image` (`image_title`, `image_creation_date`, `description`, `file_path`) VALUES (?, ?, ?, ?);";
		String queryId = "SELECT last_insert_id() as last_id FROM Image;";
		String queryShiftOrder = "UPDATE db_image_manager_js.CONTAINS SET order_id = order_id + 1 WHERE album_id = ? AND user_id = ?;";
		String queryContains = "INSERT INTO `db_image_manager_js`.`Contains` (`image_id`, `album_id`, `user_id`, `order_id`) VALUES (?, ?, ?, ?);";
		int affectedRows = 0;
		PreparedStatement preparedStatementImage = null;
		PreparedStatement preparedStatementId = null;
		PreparedStatement preparedStatementShift = null;
		PreparedStatement preparedStatementContains = null;
		ResultSet result = null;
		
		// disable auto-commit
		connection.setAutoCommit(false);
		
		try {
			// add image
			preparedStatementImage = connection.prepareStatement(queryImage);
			preparedStatementImage.setString(1, title);  
			preparedStatementImage.setDate(2, new Date(System.currentTimeMillis()));
			preparedStatementImage.setString(3, description);
			preparedStatementImage.setString(4, imagePath);
			affectedRows = preparedStatementImage.executeUpdate();
			if(affectedRows <= 0) {
				throw new SQLException();
			}
			
			// get imageId to execute the other query 
			preparedStatementId = connection.prepareStatement(queryId);
			result = preparedStatementId.executeQuery();
			int imageId = -1;
			while (result.next()) {
				imageId = result.getInt("last_id");
			}
			if(imageId < 0) {
				throw new SQLException();
			}
			
			// for every user, create tuple contains
			UserDAO userDAO = new UserDAO(connection);
			List<Integer> userIds = userDAO.getUserIds();
			
			preparedStatementShift = connection.prepareStatement(queryShiftOrder);
			for(Integer userId : userIds) {
				// order+1 for other tuples
				preparedStatementShift.setInt(1, albumId);
				preparedStatementShift.setInt(2, userId);
				preparedStatementShift.executeUpdate();
				
				// add tuple 
				// always add in first place
				int orderId = 1;
				preparedStatementContains = connection.prepareStatement(queryContains);
				preparedStatementContains.setInt(1, imageId);
				preparedStatementContains.setInt(2, albumId);
				preparedStatementContains.setInt(3, userId);
				preparedStatementContains.setInt(4, orderId);
				affectedRows = preparedStatementContains.executeUpdate();
				if(affectedRows <= 0) {
					throw new SQLException();
				}	
			}
			
			connection.commit();
		}catch(SQLException e) {
			connection.rollback();
			throw new SQLException("Error excuting query");
		}finally {
			try {
				if(preparedStatementImage != null)
					preparedStatementImage.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(preparedStatementShift != null)
					preparedStatementShift.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(preparedStatementContains != null)
					preparedStatementContains.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(preparedStatementId != null)
					preparedStatementId.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
			try {
				if(result != null)
					result.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}

			// enable auto-commit 
			connection.setAutoCommit(true);
		}
	}
	
	public Image findImageById(int imageId) throws SQLException {
		Image image = null;
		String query = "SELECT * FROM Image WHERE image_id = ?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, imageId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					image = new Image();
					image.setId(result.getInt("image_id"));
					image.setTitle(result.getString("image_title"));
					image.setCreationDate(result.getDate("image_creation_date"));
					image.setDescription(result.getString("description"));
					image.setFilePath(result.getString("file_path"));
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {				
			throw new SQLException("Error preparing the statement");
		}
		return image;
	}
	
	// returns images contained in an album in descending creation date
	public List<Image> findImagesForAlbumDefaultSort(int albumId) throws SQLException {
		ImageDAO imageDAO = new ImageDAO(connection);
		List<Image>images = new ArrayList<Image>();
		String queryId = "SELECT DISTINCT Image.image_id, Image.image_creation_date FROM Image JOIN Contains ON Image.image_id = Contains.image_id WHERE album_id = ? ORDER BY Image.image_creation_date DESC";
		try (PreparedStatement pstatement = connection.prepareStatement(queryId);) {
			pstatement.setInt(1, albumId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Image image = new Image();
					image = imageDAO.findImageById(result.getInt("image_id"));
					images.add(image);
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return images;
	}
	
	// returns images contained in user's custom order (if he has not defined one, custom order corresponds to default order)
	public List<Image> findImagesForAlbumActualSort(int albumId, int userId) throws SQLException {
		ImageDAO imageDAO = new ImageDAO(connection);
		List<Image>images = new ArrayList<Image>();
		String queryId = "SELECT Contains.image_id FROM Contains WHERE album_id = ? AND user_id = ? ORDER BY Contains.order_id ASC;";
		try (PreparedStatement pstatement = connection.prepareStatement(queryId);) {
			pstatement.setInt(1, albumId);
			pstatement.setInt(2, userId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Image image = new Image();
					image = imageDAO.findImageById(result.getInt("image_id"));
					images.add(image);
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return images;
	}
	
	public boolean deleteImageFromAlbum(int userId, int albumId, int imageId) throws SQLException {
		boolean imageDeleted = false;
		
		// check if the image is contained in more than one album
		String queryCount = "SELECT COUNT(*) AS num_albums FROM Contains WHERE image_id = ? AND user_id = ?";
		Integer numAlbums = null;
		try (PreparedStatement pstatement = connection.prepareStatement(queryCount);) {
			pstatement.setInt(1, imageId);
			pstatement.setInt(2, userId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					numAlbums = result.getInt("num_albums");
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error executing the query");
		}
		
		if(numAlbums > 1) {
			// delete a tuple from Contains for all users
			
			// disable auto-commit
			connection.setAutoCommit(false);
			
			PreparedStatement pstatement = null;
			try {
				String queryDeleteContains = "DELETE FROM Contains WHERE album_id=? AND image_id=? AND user_id=?";
				pstatement = connection.prepareStatement(queryDeleteContains);
				UserDAO userDAO = new UserDAO(connection);
				List<Integer> userIds = userDAO.getUserIds();
				for(int usr : userIds) {
					pstatement.setInt(1, albumId);
					pstatement.setInt(2, imageId);
					pstatement.setInt(3, usr);
					int affectedRows = pstatement.executeUpdate();
					if(affectedRows != 1) {
						throw new SQLException();
					}
				}	
				connection.commit();
			}catch(SQLException e) {
				connection.rollback();
				throw new SQLException("Error excuting query");
			}finally {
				try {
					if(pstatement != null)
						pstatement.close();
				}catch(SQLException e) {
					e.printStackTrace();
				}
				// enable auto-commit 
				connection.setAutoCommit(true);	
			}
			
		} else {
			// delete image
			String queryDeleteImage = "DELETE FROM Image WHERE image_id=?";
			try (PreparedStatement pstatement = connection.prepareStatement(queryDeleteImage);){
				pstatement.setInt(1, imageId);
				int affectedRows = pstatement.executeUpdate();
				if(affectedRows < 1) {
					throw new SQLException();
				}
				imageDeleted = true;
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
			// tuples in Contains and Comment will be automatically deleted thanks to CASCADE
		}
		return imageDeleted;
	}
	
	public int getUserIdByImageId(int imageId) throws SQLException {
		int userId = -1;
		String query = "SELECT album_id FROM Contains WHERE image_id = ?;";
		Integer albumId = null;
		try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, imageId);
			try (ResultSet result = preparedStatement.executeQuery();) {
				while (result.next()) {
					if(albumId == null) {
						albumId = result.getInt("album_id");
						break;
					}
 				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}
		if(albumId != null) {
			AlbumDAO albumDAO = new AlbumDAO(connection);
			try {
				userId = albumDAO.getUserIdByAlbumId(albumId);
			}catch(SQLException e) {
				throw new SQLException();
			}
		}else {
			// checked in the servlet
			throw new SQLException();
		}
		return userId;
	}
	
	public void addExistingImageToAlbum(int albumId, int imageId) throws SQLException {
		String queryShiftOrder = "UPDATE db_image_manager_js.CONTAINS SET order_id = order_id + 1 WHERE album_id = ? AND user_id = ?;";
		String queryContains = "INSERT INTO `db_image_manager_js`.`Contains` (`image_id`, `album_id`, `user_id`, `order_id`) VALUES (?, ?, ?, ?);";
		
		PreparedStatement preparedStatementShift = null;
		PreparedStatement preparedStatementContains = null;
		
		// disable auto-commit
		connection.setAutoCommit(false);
		
		try {
			// for every user, create tuple contains
			UserDAO userDAO = new UserDAO(connection);
			List<Integer> userIds = userDAO.getUserIds();
			
			preparedStatementShift = connection.prepareStatement(queryShiftOrder);
			preparedStatementContains = connection.prepareStatement(queryContains);
			
			for(Integer userId : userIds) {
				// order+1 for other tuples
				preparedStatementShift.setInt(1, albumId);
				preparedStatementShift.setInt(2, userId);
				preparedStatementShift.executeUpdate();
				
				// add tuple 
				// always add in first place
				int orderId = 1;
				int affectedRows;
				preparedStatementContains.setInt(1, imageId);
				preparedStatementContains.setInt(2, albumId);
				preparedStatementContains.setInt(3, userId);
				preparedStatementContains.setInt(4, orderId);
				affectedRows = preparedStatementContains.executeUpdate();
				if(affectedRows <= 0) {
					throw new SQLException();
				}	
			}
			
			connection.commit();
			
		}catch(SQLException e) {
			connection.rollback();
			throw new SQLException("Error excuting query");
		}finally {
			try {
				if(preparedStatementShift != null)
					preparedStatementShift.close();
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
	}
	
	public boolean albumContainsImage(int albumId, int imageId) throws SQLException {
		String query = "SELECT COUNT(*) AS num_contains FROM Contains WHERE album_id=? AND image_id=?;";
		boolean contains = false;
		try(PreparedStatement pstatement = connection.prepareStatement(query)) {
			pstatement.setInt(1, albumId);
			pstatement.setInt(2, imageId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					if(result.getInt("num_contains") > 0) {
						contains = true;
					}
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error executing the query");
		}
		return contains;
	}
}
 
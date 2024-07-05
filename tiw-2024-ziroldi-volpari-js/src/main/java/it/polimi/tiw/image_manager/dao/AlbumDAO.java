package it.polimi.tiw.image_manager.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.image_manager.beans.Album;

public class AlbumDAO {
	private Connection connection;
	
	public AlbumDAO(Connection connection) {
		this.connection = connection;
	}
	
	public List<Album> findAlbumsByUserSorted(int userId) throws SQLException {
		List<Album> userAlbums = new ArrayList<Album>();
		String query = "SELECT * FROM Album WHERE user_id = ? ORDER BY album_creation_date DESC";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, userId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Album album = new Album();
					album.setId(result.getInt("album_id"));
					album.setTitle(result.getString("album_title"));
					album.setCreationDate(result.getDate("album_creation_date"));
					userAlbums.add(album);
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return userAlbums;
	}
	
	public List<Album> findAlbumsOfOthersSorted(int userId) throws SQLException {
		List<Album> otherAlbums = new ArrayList<Album>();
		String query = "SELECT * FROM Album WHERE NOT user_id = ? ORDER BY album_creation_date DESC";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, userId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Album album = new Album();
					album.setId(result.getInt("album_id"));
					album.setTitle(result.getString("album_title"));
					album.setCreationDate(result.getDate("album_creation_date"));
					otherAlbums.add(album);
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return otherAlbums;
	}
	
	public Album findAlbumById(int albumId) throws SQLException {
		Album album = null;
		String query = "SELECT * FROM Album WHERE album_id = ?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, albumId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					album = new Album();
					album.setId(result.getInt("album_id"));
					album.setTitle(result.getString("album_title"));
					album.setCreationDate(result.getDate("album_creation_date"));
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return album;
	}
		
	public void createAlbum(String albumTitle, int userId) throws SQLException {
		String query = "INSERT INTO `db_image_manager_js`.`Album` (`album_title`, `album_creation_date`, `user_id`) VALUES (?, ?, ?);";
		int affectedRows = 0;
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, albumTitle);  
			pstatement.setDate(2, new Date(System.currentTimeMillis()));
			pstatement.setInt(3, userId);
			affectedRows = pstatement.executeUpdate();
		}catch(SQLException e) {
			throw new SQLException("Error excuting query");
		}
		if(affectedRows == 0) {
			throw new SQLException("Album not created");
		}
	}
	
	public int getUserIdByAlbumId(int albumId) throws SQLException {
		int userId = -1;
		String query = "SELECT user_id FROM Album WHERE album_id = ?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, albumId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					userId = result.getInt("user_id");
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		return userId;
	}
	
	public List<Integer> getAlbumIds() throws SQLException {
		String query = "SELECT album_id FROM Album;";
		
		List<Integer> albumIds = new ArrayList<>();
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					albumIds.add(result.getInt("album_id"));
				}
			}catch(SQLException e) {
				throw new SQLException("Error executing the query");
			}
		}catch(SQLException e) {
			throw new SQLException("Error preparing the statement");
		}
		
		return albumIds;
	}
}

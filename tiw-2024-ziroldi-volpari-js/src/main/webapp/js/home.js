/**
* Image manager home page.
*/
(function() {

	// page components
	var alertModalWindow, albumList, albumImages, imageModalWindow, sortImages;
	// main controller
	var pageOrchestrator = new PageOrchestrator();

	window.addEventListener("load", () => {
		// initialize page components
		pageOrchestrator.start();
		// display initial content
		pageOrchestrator.showAlbumList();	
	}, false);


	function AlertModalWindow(_modalWindow, _modalContent, _alertMessageDiv) {
		this.modalWindow = _modalWindow;
		this.modalContent = _modalContent;
		this.alertMessageDiv = _alertMessageDiv;
			
		this.registerEvents = function() {
			var self = this;
			
			// add close modal window event listener
			window.addEventListener("click", (e) => {
				// hide modal window
	            if(e.target == self.modalWindow) {
					self.reset();
	            }	
			});	
		}
			
		this.reset = function() {
			this.modalWindow.classList.add("hidden-element");
		}
		
		this.show = function(alertMessage){
			this.alertMessageDiv.textContent = alertMessage;
			this.modalWindow.classList.remove("hidden-element");	
		}
	}	
	

	function AlbumList(
		_albumsContainer, 
		_userAlbumsTable, _otherAlbumsTable,
		_createAlbumButton, _addImageButton, _selectAlbum, 
		_fileUpload, _confirmImage
	) {
		this.albumsContainer = _albumsContainer;
		this.userAlbumsTable = _userAlbumsTable;
		this.otherAlbumsTable = _otherAlbumsTable;
		this.createAlbumButton = _createAlbumButton;
		this.addImageButton = _addImageButton;
		this.selectAlbum = _selectAlbum;
		this.fileUpload = _fileUpload;
		this.confirmImage = _confirmImage;

		this.registerEvents = function() {
			var self = this;
			
			// add event listeners to create album abutton
			this.createAlbumButton.addEventListener("click", (e) => {
				// get form 
				var form = e.target.closest("form");
				if (form.checkValidity()) {
					// after html checks, send to server
					makeCall("POST", "CreateAlbum", form,
						function(req) {
							if (req.readyState == XMLHttpRequest.DONE) {
								var message = req.responseText;
								switch (req.status) {
									case 200:	// ok
										// update album lists
										pageOrchestrator.showAlbumList();
										break;
									case 400: // bad request
									case 500: // server error
										pageOrchestrator.showAlert(message);
										break;
									case 401: // unauthorized
										window.location.href = "login.html";
										break;
									default:
										pageOrchestrator.showAlert("An error occurred, retry.");
								}
							}
						}
					);

				} else {
					// show not valid input
					form.reportValidity();
				}
			});

			// add event listeners to add image button
			this.addImageButton.addEventListener("click", (e) => {
				// get form 
				var form = e.target.closest("form");
				if (form.checkValidity()) {
					// after html checks, send to server
					makeCall("POST", "AddNewImageToAlbum", form,
						function(req) {
							var message = req.responseText;
							switch (req.status) {
								case 200:
									// ok
									self.confirmImage.style.visibility = "hidden"; 
									break;
								case 400: // bad request
								case 500: // server error
									pageOrchestrator.showAlert(message);
									break;
								case 401: // unauthorized
									window.location.href = "login.html";
									break;
								default:
									pageOrchestrator.showAlert("An error occurred, retry.");
							}
						}
					);
				} else {
					// show not valid input
					if(self.fileUpload.files.length == 0) {
						pageOrchestrator.showAlert("No image provided.");
					}else {
						form.reportValidity();
					}
				}
			});	
			
			// add event listener to image upload
			this.fileUpload.addEventListener("change", () => {
				if (self.fileUpload.files.length > 0) {
					self.confirmImage.style.visibility = "visible";	
				}else {
					// if the user cancels the selected image, remove img
					self.confirmImage.style.visibility = "hidden";
				}
			});
			
		}

		this.reset = function() {
			this.albumsContainer.classList.add("hidden-element");
		}

		this.show = function() {
			var self = this;	// used to make this visibile to nested function
			makeCall("GET", "GetAlbumsData", null, 
				function(req) {
					// callback called when readyState = DONE
					var message = req.responseText;
					switch(req.status) {
						case 200:
							var albums = JSON.parse(message);	// all albums 
							var userAlbums = albums[0];		// user albums array 
							var otherAlbums = albums[1];	// other albums array
							
							// show update
							self.update(userAlbums, otherAlbums, self.selectAlbum);
							break;
						case 400: // bad request
						case 500: // server error
							pageOrchestrator.showAlert(message);
							break;
						case 401: // unauthorized
							window.location.href = "login.html";
							break;
						default:
							pageOrchestrator.showAlert("An error occurred, retry.");		
					}
				}
			);
		}
		
		this.update = function(userAlbums, otherAlbums, selectAlbum) {
			// get and initialize tables body
			var userAlbumsTableBody = this.userAlbumsTable.getElementsByTagName("tbody")[0];
			userAlbumsTableBody.innerHTML = "";
			var otherAlbumsTableBody = this.otherAlbumsTable.getElementsByTagName("tbody")[0];
			otherAlbumsTableBody.innerHTML = "";
			
			var self = this;
			// build user_albums_table
			userAlbums.forEach((album) => {
				buildAlbumRow(userAlbumsTableBody, album);
			});

			// build other_albums_table
			otherAlbums.forEach((album) => {
				buildAlbumRow(otherAlbumsTableBody, album);
			})
			
			// add user albums to select element
			selectAlbum.innerHTML = '<option value="">Select an album</option>';
			userAlbums.forEach((album) => {
				var option = document.createElement("option");
				option.text = album.title;
				option.value = album.id;
				selectAlbum.appendChild(option);
			});

			// reveal div
			this.confirmImage.style.visibility = "hidden";
			this.albumsContainer.classList.remove("hidden-element");
		}

		function buildAlbumRow(tableBody, album) {
			var row, albumTitleCell, albumCreationDateCell, albumAnchor;

			// create row
			row = document.createElement("tr");
				
			// create album title cell
			albumTitleCell = document.createElement("td");
			albumAnchor = document.createElement("a");
			albumTitleCell.appendChild(albumAnchor);
			albumTitle = document.createTextNode(album.title);
			albumAnchor.appendChild(albumTitle);
			albumAnchor.setAttribute("albumId", album.id);	// set custom attribute
			// set anchor behaviour
			albumAnchor.href = "#";
			// add event listener to album link
			albumAnchor.addEventListener("click", (e) => {
				// show album images
				pageOrchestrator.showAlbumImages(e.target.getAttribute("albumId"));
			}, false);
			// append cell
			row.appendChild(albumTitleCell);

			// create album date cell
			albumCreationDateCell = document.createElement("td");
			albumCreationDateCell.textContent = album.creationDate;
			// append cell
			row.appendChild(albumCreationDateCell);
		
			// append row
			tableBody.appendChild(row);
		}
	}

	function AlbumImages(_albumPageContainer, _imagesTable, _previousButton, _nextButton, _albumToHomeButton, _sortImagesButton) {
		this.albumPageContainer = _albumPageContainer;
		this.imagesTable = _imagesTable;
		this.previousButton = _previousButton;
		this.nextButton = _nextButton;
		this.albumToHomeButton = _albumToHomeButton;
		this.sortImagesButton = _sortImagesButton;
		
		this.tableSize = 5;
		this.images;
		this.imagesContent;
		this.page;
		this.albumId;
		
		this.registerEvents = function() {
			// add event listener to previous button
			this.previousButton.addEventListener("click", (e) => {
				this.page = this.page - 1;
				this.update(this.page);
			});

			// add event listener to next button
			this.nextButton.addEventListener("click", (e) => {
				this.page = this.page + 1;
				this.update(this.page);
			});

			// add go to home button event listener
			this.albumToHomeButton.addEventListener("click", (e) => {
				pageOrchestrator.showAlbumList();
			});

			// add event listener to sort button
			this.sortImagesButton.addEventListener("click", (e) => {
				pageOrchestrator.showSortImages(this.albumId, this.images);
			});
			
		}

		this.reset = function() {
			this.albumPageContainer.classList.add("hidden-element");
		}

		this.show = function(albumId) {
			var self = this;
			// request album data 
			makeCall("GET", "GetAlbumImagesData?albumId="+albumId, null, 
				function(req) {
					var message = req.responseText;
					
					switch (req.status) {
						case 200:
							var imagesData = JSON.parse(message);
							self.images = imagesData[0];
							self.imagesContent = imagesData[1];
							self.page = 1;
							if(self.images.length == 0) {
								pageOrchestrator.showAlbumList();
								pageOrchestrator.showAlert("No images contained.");
								return;
							}
							self.albumId = albumId;
							self.update();	// show first page
							break; 
						case 400: // bad request
						case 500: // server error
							pageOrchestrator.showAlert(message);
							break;
						case 401: // unauthorized
							window.location.href = "login.html";
							break;
						default:
							pageOrchestrator.showAlert("An error occurred, retry.");
					}
				}
			);
		}

		this.update = function() {
			var images = this.images;
			var timer;
			var page = this.page;
			var self = this;
			
			if(images == null || images.length == 0) {
				// already sent alert
				return;
			}
			
			var imagesTableBody = this.imagesTable.getElementsByTagName("tbody")[0];
			imagesTableBody.innerHTML = "";
			row = document.createElement("tr");
			for(let i = this.tableSize*(page-1); i < this.tableSize*page; i++) {
				imageCell = document.createElement("td");
				imageCell.classList.add("image-cell");
				if(this.images[i] != null) {
					// build image
					imageTitle = document.createTextNode(images[i].title);
					imageCell.appendChild(imageTitle);
					imageCell.appendChild(document.createElement("br"));
					imageContent = document.createElement("img");
					imageContent.src = this.imagesContent[i];
					imageContent.classList.add("image-cell-img");
					imageContent.setAttribute("imageId", images[i].id);
					imageContent.setAttribute("albumId", self.albumId);
					imageCell.appendChild(imageContent);
					imageContent.addEventListener("mouseover", (e) => {
						timer = setTimeout(() => {
							pageOrchestrator.showImageModalWindow(e.target.getAttribute("imageId"), e.target.getAttribute("albumId"));
						}, 300);
					});
					imageContent.addEventListener("mouseout", (e) => {
						clearTimeout(timer);
					});
				}
				row.appendChild(imageCell);
			}
			imagesTableBody.appendChild(row);
			
			
			// check and show previous button
			if(page > 1) {
				this.previousButton.style.visibility = "visible";
			}else {
				this.previousButton.style.visibility = "hidden";
			}
			
			// check and show next button
			numPages = Math.ceil(this.images.length / this.tableSize);
			if(page < numPages) {
				this.nextButton.style.visibility = "visible";
			}else {
				this.nextButton.style.visibility = "hidden";
			}
			
			// reveal div
			this.albumPageContainer.classList.remove("hidden-element");
		}
	}
	
	function ImageModalWindow(	_modalWindow, _modalContent, _imageContainer, _info_container, 
							_deleteImageButton, _addToOtherAlbumForm, _selectOtherUserAlbum, _addImageToOtherAlbumButton, 
							_commentsTable, _addCommentButton) {
		this.modalWindow = _modalWindow;
		this.modalContent = _modalContent;
		this.imageContainer = _imageContainer;
		this.infoContainer = _info_container;
		this.deleteImageButton = _deleteImageButton;
		this.addToOtherAlbumForm = _addToOtherAlbumForm;
		this.selectOtherUserAlbum = _selectOtherUserAlbum;
		this.addImageToOtherAlbumButton = _addImageToOtherAlbumButton;
		this.commentsTable = _commentsTable;
		this.addCommentButton = _addCommentButton;

		this.imageId;
		this.albumId;
		this.image;
		this.imageContent;
		this.comments;
		this.commentUser;
		this.userProperty;
		this.otherAlbums;
		
		this.registerEvents = function(){
			var self = this;
			
			// add close modal window event listener
			window.addEventListener("click", (e) => {
				// hide modal window
	            if(e.target == self.modalWindow) {
					self.reset();
	            }	
			});
			
			// add event listener to delete image button
			this.deleteImageButton.addEventListener("click", (e) => {
				makeCall("GET", "DeleteImageFromAlbum?imageId="+self.imageId+"&albumId="+self.albumId, null,
					function(req) {
						var message = req.responseText;
						switch (req.status) {
							case 200:
								pageOrchestrator.showAlbumImages(self.albumId);
								break;
							case 400: // bad request
							case 500: // server error
								self.reset();
								pageOrchestrator.showAlert(message);
								break;
							case 401: // unauthorized
								window.location.href = "login.html";
								break;
							default:
								self.reset();
								pageOrchestrator.showAlert("An error occurred, retry.");
						}
					}
				);
			});
			
			// add event listener to add comment button
			this.addCommentButton.addEventListener("click", (e) => {
				var form = e.target.closest("form");		
				if (form.checkValidity()) {
					// after html checks, send to server
					makeCall("POST", "AddCommentToImage", form,
						function(req) {
							var message = req.responseText;
								switch (req.status) {
									case 200:
										// ok
										pageOrchestrator.showImageModalWindow(self.imageId, self.albumId);
										break;
									case 400: // bad request
									case 500: // server error
										self.reset();
										pageOrchestrator.showAlert(message);
										break;
									case 401: // unauthorized
										window.location.href = "login.html";
										break;
									default:
										self.reset();
										pageOrchestrator.showAlert("An error occurred, retry.");
								}
						}
					);
				} else {
					// show not valid input
					form.reportValidity();
				}
			});
			
			// add event listener to add image to other album button
			this.addImageToOtherAlbumButton.addEventListener("click", (e) => {
				var self = this;
				var form = e.target.closest("form");
				if (form.checkValidity()) {
					// after html checks, send to server
					makeCall("POST", "AddExistingImageToAlbum", form,
						function(req) {
							var message = req.responseText;
							switch (req.status) {
								case 200:
									// ok
									// reload modal window (new other albums)
									pageOrchestrator.showImageModalWindow(self.imageId, self.albumId);
									break;
								case 400: // bad request
								case 500: // server error
									self.reset();
									pageOrchestrator.showAlert(message);
									break;
								case 401: // unauthorized
									window.location.href = "login.html";
									break;
								default:
									self.reset();
									pageOrchestrator.showAlert("An error occurred, retry.");
							}
						}
					);
				} else {
					// show not valid input
					form.reportValidity();
				}
			});
		}
		
		this.reset = function() {
			this.modalWindow.classList.add("hidden-element");
		}
		
		this.show = function(imageId, albumId) {
			var self = this;
			// request album data 
			makeCall("GET", "GetImageData?imageId="+imageId+"&albumId="+albumId, null,
				function(req) {
					var message = req.responseText;
					switch (req.status) {
						case 200:
							// ok
							var imageData = JSON.parse(message);
							self.image = imageData[0];
							self.imageContent = imageData[1];
							self.comments = imageData[2];
							self.commentUser = imageData[3];
							self.userProperty = imageData[4];
							self.otherAlbums = imageData[5];
									
							self.update();
							break;
						case 400: // bad request
						case 500: // server error
							pageOrchestrator.showAlert(message);
							break;
						case 401: // unauthorized
							window.location.href = "login.html";
							break;
						default:
							pageOrchestrator.showAlert("An error occurred, retry.");
					}
				}
			);
			this.imageId = imageId;
			this.albumId = albumId;
		}
		
		this.update = function(){
			var image = this.image;
			var self = this;
			if (image == null){
				return
			}
			
			// displya image content
			var imageView = document.getElementById("image");
			imageView.src = this.imageContent;
			
			// display image data
			document.getElementById("title").textContent = "Title: "+image.title;
			document.getElementById("description").textContent = "Description: "+image.description;
			document.getElementById("creation_date").textContent = "Creation date: "+image.creationDate;
			
			// show or hide delete image button and add image button
			if (this.userProperty == false){
				this.deleteImageButton.classList.add("hidden-element");
				this.addToOtherAlbumForm.classList.add("hidden-element"); 
			}
			else { 
				this.deleteImageButton.classList.remove("hidden-element");
				this.addToOtherAlbumForm.classList.remove("hidden-element"); 


				// add albums to select 
				this.selectOtherUserAlbum.innerHTML = '<option value="">Select an album</option>';
				this.otherAlbums.forEach((album) => {
					var option = document.createElement("option");
					option.text = album.title;
					option.value = album.id;
					self.selectOtherUserAlbum.appendChild(option);
				});
				document.getElementById("hidden_album_id_other_album").value = this.albumId;
				document.getElementById("hidden_image_id_other_album").value = this.imageId;
			}
			
			// build comments table
			var commentsTableBody = this.commentsTable.getElementsByTagName("tbody")[0];
			commentsTableBody.innerHTML ="";
			for(let i = 0; i < this.comments.length; i++) {
				buildCommentRow(commentsTableBody, self.comments[i].content, self.commentUser[i]);
			}
			document.getElementById("hidden_image_id_comment").value = this.imageId;
			hiddenAlbumId = document.getElementById("hidden_album_id_comment").value = this.albumId;
			
			// reveal div
			this.modalWindow.classList.remove("hidden-element");	
		}
		
		function buildCommentRow(tableBody, content, user){
			var row, commentUserCell, commentContentCell;
			
			// create row
			row = document.createElement("tr");
			// create comment user cell
			commentUserCell = document.createElement("td");
			commentUserCell.textContent = user;
			// append cell
			row.appendChild(commentUserCell);
			// create comment content cell
			commentContentCell = document.createElement("td");
			commentContentCell.textContent = content;
			// append cell
			row.appendChild(commentContentCell);
			// append row
			tableBody.appendChild(row);
		}
	}

	function SortImages(_sortImagesContainer, _imagesTitlesTable, _goToAlbumButton, _saveOrderButton) {
		this.sortImagesContainer = _sortImagesContainer;
		this.imagesTitlesTable = _imagesTitlesTable;
		this.goToAlbumButton = _goToAlbumButton;
		this.saveOrderButton = _saveOrderButton;

		// start title of the drag event
		this.startTitle;
		this.albumId;
		
		this.registerEvents = function() {
			this.saveOrderButton.addEventListener("click", (e) => {
				var rowsArray = Array.from(this.imagesTitlesTable.querySelectorAll('tbody > tr'));
				var imageIdsArray = new Array();
				rowsArray.forEach((row) => {
					imageIdsArray.push(row.getAttribute("imageId"));
				})
				
				imageIdsObj = {
					"albumId": this.albumId,
					"imageIds": imageIdsArray
				}
				var imageIdsJson = JSON.stringify(imageIdsObj); 
				
				var self = this;
				
				makeCallJson("ChangeImagesOrder", imageIdsJson,
					function(req) {
						var message = req.responseText;
							switch (req.status) {
								case 200:	// ok
									pageOrchestrator.showAlbumImages(self.albumId);
									break;
								case 400: // bad request
								case 500: // server error
									pageOrchestrator.showAlert(message);
									break;
								case 401: // unauthorized
									window.location.href = "login.html";
									break;
								default:
									pageOrchestrator.showAlert("An error occurred, retry.");
							}
					} 
				);
			});	
		}

		this.reset = function() {
			this.sortImagesContainer.classList.add("hidden-element");
		}

		// no show method, because I don't have to make a server call
		
		this.update = function(albumId, images) {
			// set album id
			this.albumId = albumId;
			
			// add event listener for goToAlbumButton
			this.goToAlbumButton.addEventListener("click", (e) => {
				pageOrchestrator.showAlbumImages(albumId);
			});

			// create table and add listeners for sorting 

			var imagesTableBody = this.imagesTitlesTable.getElementsByTagName("tbody")[0];
			imagesTableBody.innerHTML = "";
			var self = this;
			
			images.forEach((image) => {
				self.buildImageRow(imagesTableBody, image);
			});

			// reveal div
			this.sortImagesContainer.classList.remove("hidden-element");
		}

		this.buildImageRow = function(imagesTableBody, image) {
			// create row
			row = document.createElement("tr");
			row.setAttribute("imageId", image.id);
			
			// create album cell
			imageTitleCell = document.createElement("td");
			imageTitle = document.createTextNode(image.title);
			imageTitleCell.appendChild(imageTitle);
			
			// manage drag event 
			row.draggable = true;
			var self = this;
			row.addEventListener("dragstart", (e) => {
				self.titleDragStart(e);
			});
			row.addEventListener("dragover", (e) => {
				self.titleDragOver(e);
			});
			row.addEventListener("dragleave", (e) => {
				self.titleDragLeave(e);
			});
			row.addEventListener("drop", (e) => {
				self.titleDrop(e);
			});			

			row.appendChild(imageTitleCell);
			imagesTableBody.appendChild(row);
		}

		this.titleDragStart = function(event) {
			// when the drag starts, save the start element
			this.startTitle = event.target.closest("tr");
		}

		this.titleDragOver = function(event) {
			event.preventDefault();
			// change style for the element
			var dest = event.target.closest("tr");
			dest.classList.add("selected");
		}

		this.titleDragLeave = function(event) {
			// set default style for the element
			var dest = event.target.closest("tr");
			dest.classList.remove("selected");
		}

		this.titleDrop = function(event) {
			// move elements

			event.preventDefault();

			// get elements
			var dest = event.target.closest("tr");
			var table = this.imagesTitlesTable;
        	var rowsArray = Array.from(table.querySelectorAll('tbody > tr'));
        	var indexDest = rowsArray.indexOf(dest);
			var indexStart = rowsArray.indexOf(this.startTitle);
	
			// change lines
			if(indexStart < indexDest) {
				this.startTitle.parentElement.insertBefore(this.startTitle, rowsArray[indexDest + 1]);
			}else {
				this.startTitle.parentElement.insertBefore(this.startTitle, rowsArray[indexDest]);
			}
			
			// unselect all rows
			this.unselectRows(rowsArray);
		}

		this.unselectRows = function(rowsArray) {
			for (var i = 0; i < rowsArray.length; i++) {
				rowsArray[i].classList.remove("selected");
			}
		}
	}

	function PageOrchestrator() {
			
		this.start = function() {
			// initialize components
			
			alertModalWindow = new AlertModalWindow(
				document.getElementById("alert_modal_window"), 
				document.getElementById("alert_modal_content"),
				document.getElementById("alert_message")
			);
			alertModalWindow.registerEvents();

			albumList = new AlbumList(
				document.getElementById("albums_container"),
				document.getElementById("user_albums_table"), 
				document.getElementById("other_albums_table"),
				document.getElementById("create_album_button"),
				document.getElementById("add_image_button"),
				document.getElementById("select_album"),
				document.getElementById("file_upload"),
				document.getElementById("confirm_image")
			);
			albumList.registerEvents();

			albumImages = new AlbumImages(
				document.getElementById("album_page_container"),
				document.getElementById("images_table"),
				document.getElementById("previous_button"),
				document.getElementById("next_button"),
				document.getElementById("album_to_home_button"),
				document.getElementById("sort_images_button")
			);
			albumImages.registerEvents();
			
			imageModalWindow = new ImageModalWindow(
				document.getElementById("image_modal_window"),
				document.getElementById("image_modal_content"),
				document.getElementById("image_container"), 
				document.getElementById("info_container"),
				document.getElementById("delete_image_button"),
				document.getElementById("add_to_other_album_form"),
				document.getElementById("select_other_user_album"),
				document.getElementById("add_image_to_other_album_button"),
				document.getElementById("comments_table"),
				document.getElementById("add_comment_button")
			);
			imageModalWindow.registerEvents();
			
			sortImages = new SortImages(
				document.getElementById("sort_images_container"),
				document.getElementById("images_titles_table"),
				document.getElementById("sort_to_album_page"), 
				document.getElementById("save_custom_order_button")
			);
			sortImages.registerEvents();
		}

		this.refresh = function() {
			// reset everything 
			alertModalWindow.reset();
			albumList.reset();
			albumImages.reset();
			imageModalWindow.reset();
			sortImages.reset();
		}
		
		this.showAlert = function(alertMessage) {
			alertModalWindow.show(alertMessage);
		}
		
		this.showAlbumList = function(){
			this.refresh();
			albumList.show();
		}

		this.showAlbumImages = function(albumId) {
			this.refresh();
			albumImages.show(albumId);
		}
		
		this.showImageModalWindow = function(imageId, albumId){
			imageModalWindow.show(imageId, albumId);
		}

		this.showSortImages = function(albumId, images) {
			this.refresh();
			sortImages.update(albumId, images);
		}
	}
})();
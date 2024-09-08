# Web technologies project - Politecnico di Milano

## Description
Final project created for the Web technologies course at Politecnico di Milano.  
The project consists of a web app that allows users to manage their photos and view and interact with photos of other users.  
A user can:
- Create an album
- Upload and delete images
- Move images between albums
- Sort images in an album
- Add comments
- See albums and photos of other users

The full requirements can be found [here](https://github.com/kevinziroldi/image-manager-webapp/blob/main/requirements.pdf).

## Documentation 
The project documentation and database dump can be found at the following links:
- [Documentation pure HTML](https://github.com/kevinziroldi/image-manager-webapp/tree/main/tiw-2024-ziroldi-volpari-pure-html/documents)
- [Documentation JS](https://github.com/kevinziroldi/image-manager-webapp/tree/main/tiw-2024-ziroldi-volpari-js/documents)

## Installation 
In order to run the project, follow these steps:
1. Clone the repository
2. Import the project into Eclipse
3. Set up MySQL and import the database dump provided
4. Modify the file `web.xml` with correct JDBC parameters and images folder
5. Run the project on Tomcat

## Technologies and architecture
The web app was realized using:
- Relational database powered by **MySQL**
- **Java Servlets** for the backend 
- **HTML**, **CSS** and **JS** (in the JS version) for the frontend 
For educational purposes, we avoided using any JS framework and opted for plain JavaScript.

## Tools 
- [Eclipse](https://www.eclipse.org)
- [Tomcat](https://tomcat.apache.org)
- [Gson](https://github.com/google/gson)
- [Apache Commons](https://commons.apache.org)

## Preview
### Login and signup
<img src="https://github.com/kevinziroldi/image-manager-webapp/blob/main/tiw-2024-ziroldi-volpari-pure-html/photos/login_pure_html.png">

### Home page
<img src="https://github.com/kevinziroldi/image-manager-webapp/blob/main/tiw-2024-ziroldi-volpari-pure-html/photos/home_pure_html.png">

### Album page
<img src="https://github.com/kevinziroldi/image-manager-webapp/blob/main/tiw-2024-ziroldi-volpari-js/photos/album_js.png">

### Image page
<img src="https://github.com/kevinziroldi/image-manager-webapp/blob/main/tiw-2024-ziroldi-volpari-js/photos/image_js.png">


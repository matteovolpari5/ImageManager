package it.polimi.tiw.image_manager.utils;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

public class ErrorHandler {
	public static void displayError(HttpServletRequest request, HttpServletResponse response, String errorMsg) throws ServletException, IOException {
		ServletContext servletContext = request.getServletContext();
		TemplateEngine templateEngine = TemplateHandler.getEngine(servletContext);
		String errorPath = "/WEB-INF/error.html";
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("errorMsg", errorMsg);
		templateEngine.process(errorPath, ctx, response.getWriter());
	}
}

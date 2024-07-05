package it.polimi.tiw.image_manager.utils;

import javax.servlet.ServletContext;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

public class TemplateHandler {
	public static TemplateEngine getEngine(ServletContext servletContext) {
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
		return templateEngine;
	}
}

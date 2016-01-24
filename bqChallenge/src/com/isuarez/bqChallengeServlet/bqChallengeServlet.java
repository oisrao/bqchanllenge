package com.isuarez.bqChallengeServlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class bqChallengeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String webPath = "./bqChallenge/web/";
	private static final String configPath = "./bqChallenge/conf/";
	private Map<String, String> conf = new HashMap<String, String>();
	private String currentPath = "";
	private String baseURL = "";

	public bqChallengeServlet() {
		super();
		try {
			initialize();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void initialize() throws Exception {
		File confFile = new File(configPath + "config.conf");
		File currentPathFile = new File(".");
		currentPath = currentPathFile.getAbsolutePath().replace("\\", "/");
		if (!confFile.exists()) {
			throw new Exception("File " + currentPath + "/" + configPath + "config.conf not found.");
		}
		String content = FileUtils.readFileToString(new File(configPath + "config.conf"));
		String[] lines = content.split("\n");
	
		for (String line : lines) {
			String[] fields = line.split("=");
			conf.put(fields[0].trim(), fields[1].trim());
		}
		baseURL = conf.get("serverProtocol") + "://" + conf.get("serverName") + ":" + conf.get("serverPort") + "/bqChallenge";
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		String content = "";
		try {
			response.setContentType("text/html");
			if (request.getParameterMap().size() == 0) {
				String URI = request.getRequestURI();
				String type = StringUtils.substringBefore(StringUtils.substringAfter(URI, "/bqChallenge/"), "/");
				if ("files".equals(type)) {
					String fileName = StringUtils.substringAfterLast(URI, "/");
					File file = new File(webPath + "/" + fileName);
					if (file.exists()) {
						content = FileUtils.readFileToString(file);
					}
				}
				else if ("home".equals(type)) {
					content = FileUtils.readFileToString(new File(webPath + "main.html"));
					content = StringUtils.replace(content, "@serverURL", baseURL + "/process/");
				}
				else {
					//TODO
				}
			}
			else {
				String requestType = request.getParameter("type");
				if ("login".equals(requestType)) {
					
				}
				else if ("newuser".equals(requestType)) {
					
				}
			}
		}
		catch (Exception ex){
			StringBuffer error = new StringBuffer();
			error.append("The following error has occurred: ");
			error.append(getExceptionTrace(ex));
			content = error.toString();
		}
		finally {
			try {
				writeResponse(writer, content);
				writer.close();
			}
			catch (Exception ex) {}
		}
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private void writeResponse(PrintWriter writer, String content) throws Exception {
		try {
			writer.append(content);
		}
		catch (Exception ex) {
			StringBuffer error = new StringBuffer();
			error.append("An error has occurred when trying to write the response: ");
			error.append(getExceptionTrace(ex));
			writer.flush();
			writer.append(error.toString());
		}
	}

	private String getExceptionTrace(Exception ex) {
		StringBuffer text = new StringBuffer();
		for (StackTraceElement elem : ex.getStackTrace()) {
			text.append(elem.toString());
			text.append("\n");
		}
		return text.toString();
	}
}

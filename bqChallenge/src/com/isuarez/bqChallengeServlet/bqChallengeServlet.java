package com.isuarez.bqChallengeServlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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
	private Connection conn = null;
	private Statement st = null;

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
		
		//Open connection to the database
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection("jdbc:mysql://localhost/bqChallenge", conf.get("dbuser"), conf.get("dbpass"));
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
					content = getMainContent();
				}
				else {
					//TODO
				}
			}
			else {
				st = conn.createStatement();
				ResultSet rs = null;
				StringBuffer sql = new StringBuffer();
				Map<String, String[]> params = request.getParameterMap();
				String requestType = params.get("type")[0];
				if ("login".equals(requestType)) {
					String nick = params.get("lusernick")[0];
					String password = params.get("luserpass")[0];
					
					sql = new StringBuffer();
					sql.append("SELECT code, active FROM users WHERE nick='");
					sql.append(nick);
					sql.append("' AND password='");
					sql.append(password);
					sql.append("'");
					
					rs = st.executeQuery(sql.toString());
					if (rs.next()) {
						content = getProfile(nick);
					}
					else {
						throw new ControlledException("It has not been found a user with the requested nick and password. Please, revise the data and try again.", "error");
					}
				}
				else if ("newuser".equals(requestType)) {
					String nick = params.get("usernick")[0];
					String email = params.get("useremail")[0];
					sql = new StringBuffer();
					sql.append("SELECT nick, email FROM users WHERE nick='");
					sql.append(nick);
					sql.append("' OR email='");
					sql.append(email);
					sql.append("'");
					
					rs = st.executeQuery(sql.toString());
					if (rs.next()) {
						if (rs.getString(1).equals(nick)) {
							throw new ControlledException("It already exists a user with nick '" + nick + "'.", "error");
						}
						else {
							throw new ControlledException("It already exists a user with email '" + email + "'.", "error");
						}
					}
					else {
						String password = params.get("userpass")[0];
						String userName = params.get("username")[0];
						String activationToken = UUID.randomUUID().toString();
						sql = new StringBuffer();
						sql.append("INSERT INTO users (nick, email, password, name, activationToken) VALUES (");
						sql.append("'");
						sql.append(nick);
						sql.append("', '");
						sql.append(email);
						sql.append("', '");
						sql.append(password);
						sql.append("', '");
						sql.append(userName);
						sql.append("', '");
						sql.append(activationToken);
						sql.append("')");
						
						st.executeUpdate(sql.toString());
						
						//Send email
						Properties props = new Properties();
						props.put("mail.smtp.auth", "true");
						props.put("mail.smtp.starttls.enable", "true");
						props.put("mail.smtp.host", "smtp.gmail.com");
						props.put("mail.smtp.port", "587");
						props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

						final String emailUser = conf.get("emailUser");
						final String emailPass = conf.get("emailPass");
						String URL = baseURL + "/process?type=register&token=" + activationToken;

						Session session = Session.getDefaultInstance(props,	new javax.mail.Authenticator() {
							@Override
							protected PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(emailUser, emailPass);
							}
						});

						try {
							Message message = new MimeMessage(session);
							message.setFrom(new InternetAddress(emailUser));
							message.setRecipients(Message.RecipientType.TO,	InternetAddress.parse(email));
							message.setSubject("New account created");
							StringBuffer txt = new StringBuffer();
							txt.append("<b>Congratulations!</b><br/><br/>");
							txt.append("You have correctly created a new account for user ");
							txt.append("<i>");
							txt.append(nick);
							txt.append("</i>");
							txt.append(" in bq Challenge App.<br/>");
							txt.append("In order to can access to the app, please make click <a href=\"");
							txt.append(URL);
							txt.append("\">here</a>");
							txt.append(" and activate your account. Once you have done it, you will be able to access to the app with your nick and your password.");
							message.setContent(txt.toString(), "text/html");

							Transport.send(message);
						}
						catch (Exception ex) {
							ex.printStackTrace();
							throw new ControlledException("There was an error while trying to send you the registration email, please register a new user.", "error");
						}
						throw new ControlledException("Your user has been correctly created, please revise your mail and activate your account in order to can access to the app.", "info");
					}
				}
				else if ("register".equals(requestType)) {
					String activationToken = params.get("token")[0];
					sql = new StringBuffer();
					sql.append("SELECT code FROM users WHERE activationToken='");
					sql.append(activationToken);
					sql.append("'");
					
					rs = st.executeQuery(sql.toString());
					if (rs.next()) {
						int code = rs.getInt(1);
						sql = new StringBuffer();
						sql.append("UPDATE users SET active=1 WHERE code=");
						sql.append(code);
						
						st.executeUpdate(sql.toString());
						throw new ControlledException("Your user has been correctly activated. Now you can access to the app.", "info");
					}
					else {
						throw new ControlledException("The requested activation URL is not valid, please send a valid URL.", "error");
					}
				}
			}
		}
		catch (ControlledException cex) {
			content = getMainContent();
			StringBuffer error = new StringBuffer();
			error.append("openWindow(\"");
			error.append(cex.getMessage());
			error.append("\", '");
			error.append(cex.getType());
			error.append("');");
			content = StringUtils.replace(content, "//@action", error.toString());
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

	private String getMainContent() {
		String content = "";
		try {
			content = FileUtils.readFileToString(new File(webPath + "main.html"));
			content = StringUtils.replace(content, "@serverURL", baseURL + "/process");
		}
		catch (Exception ex) {}
		return content;
	}
	
	private String getProfile(String user) {
		String content = "";
		try {
			content = FileUtils.readFileToString(new File(webPath + "profile.html"));
			content = StringUtils.replace(content, "@user", user);
		}
		catch (Exception ex) {}
		return content;
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

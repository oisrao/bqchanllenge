package com.isuarez.bqChallengeServlet;

@SuppressWarnings("serial")
public class ControlledException extends Exception {
	private String message = "";
	private String type = "";

	public ControlledException(String message, String type) {
		this.message = message;
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}	
}

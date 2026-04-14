package com.shared.network;

import java.io.Serializable;

public class Response implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String status;
	private String errorCode;
	private String message;
	private Object data;

	public Response() {
	}

	public Response(String status, String message, Object data){
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public Response(String status, String errorCode, String message, Object data){
		this.status = status;
		this.errorCode = errorCode;
		this.message = message;
		this.data = data;
	}
	
	public String getStatus(){return this.status;}
	public String getErrorCode(){return this.errorCode;}
	public String getMessage(){return this.message;}
	public Object getData(){return this.data;}
}

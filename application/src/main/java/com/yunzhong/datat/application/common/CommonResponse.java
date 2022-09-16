package com.yunzhong.datat.application.common;

public class CommonResponse {

	public static final String SUCCESS_CODE = "200";

	public static final String FAIL_CODE = "500";

	private String code;
	private String message;
	private Object data;

	public CommonResponse() {

	}

	public CommonResponse(String code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public CommonResponse(Object data) {
		this.code = SUCCESS_CODE;
		this.data = data;
	}

	public static CommonResponse success(Object data) {
		return new CommonResponse(data);
	}

	public static CommonResponse fail() {
		return new CommonResponse(FAIL_CODE, null, null);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}

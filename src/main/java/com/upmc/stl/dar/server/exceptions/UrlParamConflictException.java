package com.upmc.stl.dar.server.exceptions;

import java.lang.reflect.Method;

public class UrlParamConflictException extends ParamNotAcceptableException {
	private static final long serialVersionUID = 8379981265154333734L;

	protected UrlParamConflictException(String value, Method method, Class<?> clazz) {
		super(value, method, clazz);
	}

	@Override
	public String getMessage() {
		return "Param is contained twice param value \"" + value + "\" inside method + \" " + 
				method.getName() + "\" of class \" " + clazz.getName() + " \"";
	}
}

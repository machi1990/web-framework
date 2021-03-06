package com.upmc.stl.dar.server.examples.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.upmc.stl.dar.server.annotation.CONSUMES;
import com.upmc.stl.dar.server.annotation.CONSUMES.Consumed;
import com.upmc.stl.dar.server.configuration.views.Model2View;
import com.upmc.stl.dar.server.exceptions.ExceptionCreator;
import com.upmc.stl.dar.server.exceptions.ExceptionCreator.ExceptionKind;
import com.upmc.stl.dar.server.exceptions.ServerException;
import com.upmc.stl.dar.server.annotation.GET;
import com.upmc.stl.dar.server.annotation.PARAM;
import com.upmc.stl.dar.server.annotation.PATH;
import com.upmc.stl.dar.server.annotation.POST;
import com.upmc.stl.dar.server.annotation.PRODUCES;
import com.upmc.stl.dar.server.request.ContentType;
import com.upmc.stl.dar.server.request.Request;
import com.upmc.stl.dar.server.response.Response;
import com.upmc.stl.dar.server.response.Status;
import com.upmc.stl.dar.server.tools.Session;

@PATH("/demo")
public class Chat {
	public static HashMap<String, User> activeSession = new HashMap<>();
	public static List<Message> messages = new ArrayList<>(200);
	public static List<User> users = new ArrayList<>();
	
	@GET
	@PATH("/logout")
	public Response disconnect(Request request) {	
		Response response = Response.redirect("/demo/home.html");
		clearSession(request, response);
		return response;
	}
	
	@POST
	@CONSUMES(Consumed.JSON)
	@PATH("/login")
	public Response login(Request request,User user) {	
		Response response;
		if (!users.contains(user)) {
			response = Response.response(Status.UNAUTHORIZED);
			response.build("build error message : Username not found");
			return response;
		}
		
		User user_ = null;
		for (User account : users) {
			if (account.equals(user)) {
				user_ = account;
				break;
			}
		}
		
		if (!user_.getPassword().equals(user.getPassword())) {
			response = Response.response(Status.UNAUTHORIZED);
			response.build("build error message : wrong password");
			return response;
		} else {
			return addToActiveSession(request, user_);
		}
	}
	
	@POST
	@CONSUMES(Consumed.JSON)
	@PATH("/signup")
	public Response signup(User user, Request request) {
		Response response;
		if (!users.contains(user)) {
			request.clearSession();
			response = Response.response(Status.OK);
			Session session = Session.newInstance();
			response.addSession(session);
			users.add(user);
			activeSession.put(session.getValue(), user);
			Message message = new Message();
			message.setContent(user.getUsername() + " has joined the chatroom.");
			message.setPostedBy("ChatBot");
			addMessage(message);
		} else {
			response = Response.response(Status.UNAUTHORIZED);
			response.build("Username already exists. Choose a different one.");
		}

		return response;
	}
	
	@GET
	@PATH("/list/messages")
	@PRODUCES(ContentType.JSON)
	public List<Message> messages(Request request) throws ServerException {	
		if (!request.hasActiveSession() || !activeSession.containsKey(request.sessionInstance().getValue())) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}

		return messages;
	}
	
	@GET
	@PATH("/list/users")
	@PRODUCES(ContentType.JSON)
	public List<User> users(Request request) throws ServerException {	
		if (!request.hasActiveSession() || !activeSession.containsKey(request.sessionInstance().getValue())) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}
		
		ArrayList<User> list = new ArrayList<>(users);
		
		for (User user:list) {
			user.setImage("");
			user.setPassword(null);
			user.setNumberOfConnections(null);
		}
		
		return list;
	}
	

	@GET
	@PATH("/users")
	public Model2View usersHTML(Request request) throws ServerException {	
		if (!request.hasActiveSession() || !activeSession.containsKey(request.sessionInstance().getValue())) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}
		
		ArrayList<String> list = new ArrayList<>();
		ArrayList<User> _users = new ArrayList<>(activeSession.values());
		for (User user:_users) {
			list.add(user.getUsername());
		}
		
		Model2View model2view  = new Model2View("/views/demo/users.html");
		model2view.put("users", list);
		
		return model2view;
	}
	
	@GET
	@PATH("/users/<username>")
	@PRODUCES(ContentType.JSON)
	public User getUser(@PARAM("<username>") String username, Request request) throws ServerException {	
		if (!request.hasActiveSession() || !activeSession.containsKey(request.sessionInstance().getValue())) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}
		
		User user = new User();
		
		for (User user_: users) {
			if (user_.getUsername().equals(username)) {
				user.setFullName(user_.fullName);
				user.setImage(user_.getImage());
				user.setNumberOfConnections(user_.getNumberOfConnections());
				user.setPassword(user_.getPassword());
				user.setId(user_.getId());
				break;
			}
		}
		
		if (user.getUsername() == null) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}
		
		return user;
	}
	
	@POST
	@CONSUMES(Consumed.JSON)
	@PATH("/message")
	public Response newMessage(Message message, Request request) throws ServerException {
		if (!request.hasActiveSession() || !activeSession.containsKey(request.sessionInstance().getValue())) {
			throw ExceptionCreator.creator().create(ExceptionKind.NOT_SUPPORTED);
		}
		
		addMessage(message);
		
		return Response.response(Status.OK);
	}

	private void addMessage(Message message) {
		synchronized (messages) {
			if (messages.size() == 200) {
				messages.remove(0);
			}
			message.setPostedAt(new Date());
			messages.add(message);
		}
	}
	
	private void clearSession(Request request, Response response) {
		if (request.hasActiveSession()) {
			User user = activeSession.get(request.sessionInstance().getValue());
			Message message = new Message();
			message.setContent(user.getUsername() + " has left the chatroom.");
			message.setPostedBy("ChatBot");
			addMessage(message);
			activeSession.remove(request.sessionInstance().getValue());
			Session session = request.sessionInstance();
			session.clear();
			response.addSession(session);
			request.clearSession();
		}
	}
	
	private Response addToActiveSession(Request request, User user) {
		Response response = Response.response(Status.OK);
		user.setNumberOfConnections(user.getNumberOfConnections() + 1);
		if (!request.hasActiveSession()) {
			Session session = Session.newInstance();
			activeSession.put(session.getValue(), user);
			response.addSession(session);
		} else {
			activeSession.put(request.sessionInstance().getValue(), user);
		}
		
		Message message = new Message();
		message.setContent(user.getUsername() + " has joined the chatroom.");
		message.setPostedBy("ChatBot");
		addMessage(message);
		
		return response;
	}
}

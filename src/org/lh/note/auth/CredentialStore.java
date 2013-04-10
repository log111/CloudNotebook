package org.lh.note.auth;

import java.util.HashMap;
import java.util.Map;

public class CredentialStore {
	
	private static CredentialStore inst = null;
	private Map<String, User> map = new HashMap<String, User>(); 
	
	public static CredentialStore getInstance(){
		if(null == inst){
			inst = new CredentialStore();
		}	
		return inst;
	}
	
	public boolean setUser(User usr){
		if(null == map.get(usr.username) ){
			map.put(usr.username, usr);
			return true;
		}
		return false;
	}
	
	public User getUser(String username, String password){
		User usr = map.get(username);
		if(usr.password.equals(password)){
			return usr;
		}
		return null;
	}
	
	public static class User{
		public String username;
		public String password;
		public String email;		
	}
}

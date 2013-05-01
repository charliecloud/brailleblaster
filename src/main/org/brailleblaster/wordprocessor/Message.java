package org.brailleblaster.wordprocessor;

import java.util.HashMap;

public class Message {
	public BBEvent type;
	HashMap<String, Object> args;
	
	public Message(BBEvent type){
		this.type = type;
		this.args = new HashMap<String, Object>();
	}
	
	public void put(String key, Object value){
		args.put(key, value);
	}
	
	public <T> Object getValue(String key){
		return args.get(key);
	}
	
	public void clearMessage(){
		this.args.clear();
	}
}

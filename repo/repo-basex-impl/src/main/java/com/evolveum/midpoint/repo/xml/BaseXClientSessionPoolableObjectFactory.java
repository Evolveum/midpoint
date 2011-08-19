package com.evolveum.midpoint.repo.xml;

import org.apache.commons.pool.PoolableObjectFactory;
import org.basex.server.ClientSession;

public class BaseXClientSessionPoolableObjectFactory implements PoolableObjectFactory {

	private String host;
	private int port;
	private String username;
	private String password;
	private String dbName;
	
	public BaseXClientSessionPoolableObjectFactory(String host, int port, String username, String password, String dbName) {
		this.host = host; 
		this.port = port;
		this.username = username; 
		this.password = password;
		this.dbName = dbName;
	}
	
	@Override
	public Object makeObject() throws Exception {
		ClientSession session = new ClientSession(host, port, username, password);
		session.execute("OPEN " + dbName);
		return session;
	}

	@Override
	public void destroyObject(Object obj) throws Exception {
		ClientSession session = (ClientSession) obj;
		session.close();
	}

	@Override
	public boolean validateObject(Object obj) {
		ClientSession session = (ClientSession) obj;
		return true;
	}

	@Override
	public void activateObject(Object obj) throws Exception {
		//No action
	}

	@Override
	public void passivateObject(Object obj) throws Exception {
		//No action
	}

}

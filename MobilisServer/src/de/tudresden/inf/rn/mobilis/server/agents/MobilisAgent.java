/*******************************************************************************
 * Copyright (C) 2010 Technische Universit�t Dresden
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Dresden, University of Technology, Faculty of Computer Science
 * Computer Networks Group: http://www.rn.inf.tu-dresden.de
 * mobilis project: http://mobilisplatform.sourceforge.net
 ******************************************************************************/
package de.tudresden.inf.rn.mobilis.server.agents;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.jivesoftware.smack.BOSHConfiguration;
//import org.jivesoftware.smack.BOSHConnection;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.keepalive.KeepAliveManager;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import de.tudresden.inf.rn.mobilis.server.MobilisManager;
import de.tudresden.inf.rn.mobilis.server.services.MobilisService;

/**
 *
 * @author Christopher, Philipp Grubitzsch
 */
public class MobilisAgent implements NodeInformationProvider, ConnectionListener {

	private String mIdentifier;
	private Connection mConnection = null;
	private String fullJid = null;
	private EntityCapsManager capsMaganger;
	private ServiceDiscoveryManager serviceDiscoveryManager;
	private String discoName="";
	private String discoVer="";
	private String mode; //for entity caps: tells if a service is running in single or multi mode
	
	private final Set<MobilisService> mServices = Collections.synchronizedSet(new HashSet<MobilisService>());
	private final Map<String, Object> mDefaultSettings = Collections.synchronizedMap(new HashMap<String, Object>());
	

	public MobilisAgent(String ident) {
		this(ident, true);
	}
	
	/**
	 * This constructor is used by the main services (coordinator, admin, deployment)
	 * @param ident
	 * @param loadConfig
	 */
	public MobilisAgent(String ident, boolean loadConfig) {
		mIdentifier = ident;

		if (loadConfig) {
			try {
				synchronized(mDefaultSettings) {
					mDefaultSettings.putAll(MobilisManager.getInstance().getSettings("agents", ident));
				}
			} catch (Exception e) {
				MobilisManager.getLogger().warning("Mobilis Agent (" + getIdent() + ") could not read configuration, using hardcoded settings instead.");
			}

			String hostname = null;

			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException uh) {
				hostname = "localhost";
			}

			synchronized (mDefaultSettings) {
				if (!mDefaultSettings.containsKey("host")) {
					mDefaultSettings.put("host", hostname);
				}
				if (!mDefaultSettings.containsKey("port")) {
					mDefaultSettings.put("port", "5222");
				}
				if (!mDefaultSettings.containsKey("service")) {
					mDefaultSettings.put("service", hostname);
				}
				if (!mDefaultSettings.containsKey("resource")) {
					mDefaultSettings.put("resource", "MobilisServer");
				}
			}
		}
	}
	
	public MobilisAgent(String ident, boolean loadConfig, String resource) {
		mIdentifier = ident;
		
		if (loadConfig) {
			try {
				synchronized(mDefaultSettings) {
					// start new services in same connection mode as coordinator service
					mDefaultSettings.put("conType", MobilisManager.getInstance().getAgent("coordinator").getSettingString("conType"));
					
					mDefaultSettings.putAll(MobilisManager.getInstance().getSettings("agents", ident));
				}
			} catch (Exception e) {
				e.printStackTrace();
				MobilisManager.getLogger().warning("Mobilis Agent (" + getIdent() + ") could not read configuration, using hardcoded settings instead.");
			}

			String hostname = null;

			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException uh) {
				hostname = "localhost";
			}

			synchronized (mDefaultSettings) {
				if (!mDefaultSettings.containsKey("host")) {
					mDefaultSettings.put("host", hostname);
				}
				if (!mDefaultSettings.containsKey("port")) {
					mDefaultSettings.put("port", "5222");
				}
				if (!mDefaultSettings.containsKey("service")) {
					mDefaultSettings.put("service", hostname);
				}				
				mDefaultSettings.put("resource", resource);
			}
		}
	}
	
	public MobilisAgent(String username, String password, MobilisAgent refAgentForConfig) {
		this(username, false);
		synchronized (mDefaultSettings) {
			mDefaultSettings.putAll(refAgentForConfig.getAllSettings());
			mDefaultSettings.put("username", username);
			mDefaultSettings.put("password", password);
		}
	}

	protected Map<String, Object> getAllSettings() {
		return mDefaultSettings;
	}
	
	public Object getSettingString(String name) {
		synchronized (mDefaultSettings) {
			Object o = mDefaultSettings.get(name); 
			return (o instanceof String ? (String)o : null);
		}
	}

	@SuppressWarnings("unchecked")
	public Object getSettingStrings(String name) {
		synchronized (mDefaultSettings) {
			Object o = mDefaultSettings.get(name); 
			return (o instanceof Map<?,?> ? (Map<String,String>)o : null);
		}
	}

	public void startup() throws XMPPException {
		String host;
		Integer port;
		String service;

		synchronized (mDefaultSettings) {
			host = (String)mDefaultSettings.get("host");
			port = Integer.parseInt((String)mDefaultSettings.get("port"));
			service = (String)mDefaultSettings.get("service");
		}

		if (mDefaultSettings.get("conType") != null && mDefaultSettings.get("conType").equals("bosh")) {
			//BOSHConfiguration connConfig = new BOSHConfiguration(false, host, port, "/http-bind/", service);
			//mConnection = new BOSHConnection(connConfig);
			System.out.println("BOSH is not supported by Smack 3.3 .");
		} else {
			// default: XMPP connection
			ConnectionConfiguration connConfig = new ConnectionConfiguration(host, port, service);
			mConnection = new XMPPConnection(connConfig);
		}
		mConnection.connect();
		//stops auto pinging the Server, which is enabled by default in smack 3.3
		KeepAliveManager.getInstanceFor(mConnection).stopPinging();
		
		String username = null;
		String password = null;
		String resource = null;

		synchronized (mDefaultSettings) {
			username = (String)mDefaultSettings.get("username");
			password = (String)mDefaultSettings.get("password");
			resource = (String)mDefaultSettings.get("resource");
			
			if (username != null && service != null && resource != null) {
				fullJid = username + "@" + service + "/" + resource;
			}
		}

		mConnection.login(username, password, resource);
		mConnection.addConnectionListener(this);

		serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(mConnection);
		capsMaganger = EntityCapsManager.getInstanceFor(mConnection);
		capsMaganger.enableEntityCaps();
		serviceDiscoveryManager.setEntityCapsManager(capsMaganger);
		
		// Set on every established connection that this client supports the Mobilis
		// protocol. This information will be used when another client tries to
		// discover whether this client supports Mobilis or not.
/*		try {
			serviceDiscoveryManager.addFeature(MobilisManager.discoNamespace);
		} catch (Exception e) {
			MobilisManager.getLogger().warning("Problem with ServiceDiscoveryManager: " + e.getMessage());
		}*/
		//discoinfo for a concrete service instance
		for(MobilisService ms : mServices){
			String runtimeJID = MobilisManager.getInstance().getAgent("deployment").getSettingString("username") + "@" + MobilisManager.getInstance().getAgent("deployment").getSettingString("host");
			//String discoNamespace = ms.get_serviceNamespace().replace("http://mobilis.inf.tu-dresden.de#services/", "");
			serviceDiscoveryManager.addFeature(MobilisManager.discoNamespace + "/instance#" + "servicenamespace=" + ms.get_serviceNamespace() + ",version=" + ms.getVersion() + ",name=" + ms.getName() + ",rt=" + runtimeJID);
		}
		//discoinfo for service installed on runtime
		if(discoName.length()>0 && discoVer.length()>0){
			String runtimeJID = MobilisManager.getInstance().getAgent("deployment").getSettingString("username") + "@" + MobilisManager.getInstance().getAgent("deployment").getSettingString("host");
			serviceDiscoveryManager.addFeature(MobilisManager.discoNamespace + "/service#" + "servicenamespace=" + discoName + ",version=" + discoVer + ",mode=" + mode + ",rt=" + runtimeJID);
			//discoName="";
			discoVer="";
		}
		capsMaganger.updateLocalEntityCaps();

		synchronized(mServices) {
			for (MobilisService ms : mServices) {
				try {
					ms.startup();
					serviceDiscoveryManager.setNodeInformationProvider(ms.getNode(), ms);
				} catch (Exception e) {
					MobilisManager.getLogger().warning("Couldn't startup Mobilis Service (" + ms.getIdent() + ") because of " + e.getClass().getName() + ": " + e.getMessage());
				}
			}
		}

		// Set the NodeInformationProvider that will provide information about the
		// offered services whenever a disco request is received
		try {
			serviceDiscoveryManager.setNodeInformationProvider(MobilisManager.discoServicesNode, this);
		} catch (Exception e) {
			MobilisManager.getLogger().warning("Problem with NodeInformationProvider: " + MobilisManager.discoServicesNode + " (" + getIdent() + ") " + e.getMessage());
		}

		//try {
		//	String entityID = mJid;
		//	DiscoverItems discoverItems = new DiscoverItems();
		//	Item item = new Item(entityID);
		//	item.setAction(Item.UPDATE_ACTION);
		//	item.setName("Mobilis Agent");
		//	item.setNode(null);
		//	discoverItems.addItem(item);
		//	sdm.publishItems(entityID, discoverItems);
		//} catch (Exception e) {
		//	MobilisManager.getLogger().warning("Problem with ServiceDiscoveryManager: " + e.getMessage());
		//}

		// logging
		MobilisManager.getLogger().info("Mobilis Agent (" + getIdent() + ") started up.");
	}
	
	/**
	 * Deletes the session agent account, frees all resources and disconnects the XMPP connection.
	 * @throws org.jivesoftware.smack.XMPPException
	 */
	public void shutdown() throws XMPPException {
		
		//remove Agent from MobilisManager mAgents Map
		MobilisManager.getInstance().removeAgent(StringUtils.parseResource(fullJid));
		
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
		
		// ServiceDiscovery (feature) http://rn.inf.tu-dresden.de/mobilis

		//not used anymore. Cause of introduction of Entity Caps, the capManager sends new presence if features
		//have changed. If the updated presence send by the entity caps manager received clients after 
		//the unaivailable presence send by the shutdown sequence, it could result in online presence 
		//state on distant clients while node is offline.
//		try {
//			sdm.removeFeature(MobilisManager.discoNamespace);
//		} catch (Exception e) {
//			MobilisManager.getLogger().warning("Problem with ServiceDiscoveryManager: " + e.getMessage());
//		}

		// ServiceDiscovery (info+items) http://rn.inf.tu-dresden.de/mobilis#services
		try {
			sdm.removeNodeInformationProvider(MobilisManager.discoServicesNode);
		} catch (Exception e) {
			MobilisManager.getLogger().warning("Problem with NodeInformationProvider: " + MobilisManager.discoServicesNode + " (" + getIdent() + ") " + e.getMessage());
		}
		
		for (MobilisService service : mServices) {
			try {
				sdm.removeNodeInformationProvider(service.getNode());
				service.shutdown();
			} catch (Exception e) {
				// TODO Auto-generated catch block
			}
		}

		if ((mConnection != null) && mConnection.isConnected()) {
			mConnection.removeConnectionListener(this);
			mConnection.disconnect();
		}
		mConnection = null;

		// logging
		MobilisManager.getLogger().info("Mobilis Agent (" + getIdent() + ") shut down.");
		
		MobilisManager.getInstance().notifyOfAgentShutdown(this);
		
	}

	public void registerService(MobilisService service) {
		synchronized (mServices) {
			mServices.add(service);
		}

		// logging
		MobilisManager.getLogger().config("Mobilis Service (" + service.getIdent() + ") registered at Mobilis Agent (" + getIdent() + ").");
	}

	public void unregisterService(MobilisService service) {
		synchronized (mServices) {
			mServices.remove(service);
		}

		// logging
		MobilisManager.getLogger().config("Mobilis Service (" + service.getIdent() + ") unregistered at Mobilis Agent (" + getIdent() + ").");
	}

	/**
	 * Returns the connection of the session agent.
	 * @return Connection
	 */
	public Connection getConnection() {
		return mConnection;
	}
	
	/**
	 * Returns the full jid of the session agent.
	 * @return String
	 */
	public String getFullJid() {
		return fullJid;
	}
	
	/**
	 * Returns the XMPP resource of the session agent.
	 * @return String
	 */
	public String getResource() {
		Object o = mDefaultSettings.get("resource");
		if (o!= null && o instanceof String)
			return (String) o;
		return null;
	}

	/**
	 * Returns the identifier of the session agent.
	 * @return String
	 */
	public String getIdent() {
		return mIdentifier;
	}
	
	// XMPP related functions
	
	@Override
	public List<DiscoverInfo.Identity> getNodeIdentities() {
		List<DiscoverInfo.Identity> identities = new ArrayList<DiscoverInfo.Identity>();
		if (getNodeItems().size() > 0) {
			identities.add(new DiscoverInfo.Identity("hierarchy", "branch"));
		} else {
			identities.add(new DiscoverInfo.Identity("hierarchy", "leaf"));
		}
		return identities;
	}
	
	@Override
	public List<String> getNodeFeatures() {
		List<String> features = new ArrayList<String>();
		features.add(MobilisManager.discoNamespace);
		return features;
	}
	
	@Override
	public List<DiscoverItems.Item> getNodeItems() {
		List<DiscoverItems.Item> items = new ArrayList<DiscoverItems.Item>();
		synchronized(mServices) {
			// services offered by this agent
			for (MobilisService service : mServices) {
				try {
					DiscoverItems.Item item = service.getDiscoverItem();
					if (item != null) {
						items.add(item);
					}
				} catch (Exception e) {

				}
			}
		}
		return items;
	}
	
	public String servicesToString(){
		StringBuilder sb = new StringBuilder("Services[");
		
		for ( MobilisService service : mServices ) {
			sb.append( "NS=").append( service.getNamespace() ).append( ",Version=" ).append( service.getVersion() );
		}
		
		sb.append( "]" );
		
		return sb.toString();
	}

	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub
		// Notification that the connection was closed normally or that the reconnection process has been aborted.
		MobilisManager.getLogger().info("MobilisAgent (" + getIdent() + ") connection was closed normally or the reconnection process has been aborted.");
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		// TODO Auto-generated method stub
		// Notification that the connection was closed due to an exception.
		MobilisManager.getLogger().warning("MobilisAgent (" + getIdent() + ") connection was closed due to " + e.getClass().getName() + ": " + e.getMessage());
	}

	@Override
	public void reconnectingIn(int arg0) {
		// TODO Auto-generated method stub
		// The connection will retry to reconnect in the specified number of seconds.
		MobilisManager.getLogger().fine("MobilisAgent (" + getIdent() + ") will retry to reconnect in " + arg0 + " seconds.");
	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		// TODO Auto-generated method stub
		// An attempt to connect to the server has failed.
		MobilisManager.getLogger().warning("MobilisAgent (" + getIdent() + ") attempt to connect to the server has failed.");
	}

	@Override
	public void reconnectionSuccessful() {
		// TODO Auto-generated method stub
		// The connection has reconnected successfully to the server.
		MobilisManager.getLogger().info("MobilisAgent (" + getIdent() + ") reconnected successfully to the server.");
	}

	@Override
	public List<PacketExtension> getNodePacketExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	public ServiceDiscoveryManager getServiceDiscoveryManager() {
		return serviceDiscoveryManager;
	}

	public void setServiceDiscoveryManager(
			ServiceDiscoveryManager serviceDiscoveryManager) {
		this.serviceDiscoveryManager = serviceDiscoveryManager;
	}

	public String getDiscoName() {
		return discoName;
	}

	public void setDiscoName(String discoName) {
		this.discoName = discoName;
	}

	public String getDiscoVer() {
		return discoVer;
	}

	public void setDiscoVer(String discoVer) {
		this.discoVer = discoVer;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}

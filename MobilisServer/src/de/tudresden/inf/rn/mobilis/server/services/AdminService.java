package de.tudresden.inf.rn.mobilis.server.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;

import de.tudresden.inf.rn.mobilis.server.MobilisManager;
import de.tudresden.inf.rn.mobilis.server.agents.MobilisAgent;
import de.tudresden.inf.rn.mobilis.server.deployment.container.ServiceContainer;
import de.tudresden.inf.rn.mobilis.server.deployment.container.ServiceContainerState;
import de.tudresden.inf.rn.mobilis.server.deployment.exception.InstallServiceException;
import de.tudresden.inf.rn.mobilis.server.deployment.exception.RegisterServiceException;
import de.tudresden.inf.rn.mobilis.server.deployment.exception.UpdateServiceException;
import de.tudresden.inf.rn.mobilis.server.deployment.helper.FileHelper;
import de.tudresden.inf.rn.mobilis.server.deployment.helper.FileUploadInformation;
import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.AdministrationBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.ConfigureServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.InstallServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.RegisterServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.UninstallServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.UnregisterServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.admin.UpdateServiceBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.deployment.PrepareServiceUploadBean;
import de.tudresden.inf.rn.mobilis.xmpp.beans.helper.DoubleKeyMap;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanHelper;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanIQAdapter;
import de.tudresden.inf.rn.mobilis.xmpp.server.BeanProviderAdapter;

/**
 * The Class AdminService is used to handle the life cycle of a deployed
 * service.
 */
public class AdminService extends MobilisService {

	public static final String UPLOADED_SERVICE_DICTIONARY_PATH = "service";

	// private DateFormat _dateFormatter;
	//
	// public void log( String str ) {
	// System.out.println( "[" + _dateFormatter.format(
	// System.currentTimeMillis() ) + "] " + str );
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tudresden.inf.rn.mobilis.server.services.MobilisService#
	 * registerPacketListener()
	 */
	@Override
	protected void registerPacketListener() {
		XMPPBean installServiceBean = new InstallServiceBean();
		XMPPBean configureServiceBean = new ConfigureServiceBean();
		XMPPBean registerServiceBean = new RegisterServiceBean();
		XMPPBean unregisterServiceBean = new UninstallServiceBean();
		XMPPBean uninstallServiceBean = new UnregisterServiceBean();
		XMPPBean updateServiceBean = new UpdateServiceBean();

		( new BeanProviderAdapter( installServiceBean ) ).addToProviderManager();
		( new BeanProviderAdapter( configureServiceBean ) ).addToProviderManager();
		( new BeanProviderAdapter( registerServiceBean ) ).addToProviderManager();
		( new BeanProviderAdapter( unregisterServiceBean ) ).addToProviderManager();
		( new BeanProviderAdapter( uninstallServiceBean ) ).addToProviderManager();
		( new BeanProviderAdapter( updateServiceBean ) ).addToProviderManager();

		IQListener iqListener = new IQListener();
		PacketTypeFilter locFil = new PacketTypeFilter( IQ.class );
		getAgent().getConnection().addPacketListener( iqListener, locFil );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tudresden.inf.rn.mobilis.server.services.MobilisService#shutdown()
	 */
	@Override
	public void shutdown() throws Exception {
		super.shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tudresden.inf.rn.mobilis.server.services.MobilisService#startup(de
	 * .tudresden.inf.rn.mobilis.server.agents.MobilisAgent)
	 */
	@Override
	public void startup( MobilisAgent agent ) throws Exception {
		super.startup( agent );

		// _dateFormatter = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss:SSS" );
	};

	/**
	 * Gets the existing jar file which was uploaded previously.
	 * 
	 * @param filepath
	 *            the filepath of the jar file
	 * @param filename
	 *            the filename of the jarfile
	 * @return the jar file
	 */
	private File getExistingJarFile( String filepath, String filename ) {
		File jarFile = null;

		try {
			ArrayList< String > jarFiles = FileHelper.getFilenames( filepath,
					new String[] { ".jar" } );

			for ( String file : jarFiles ) {
				if ( file.matches( String.format( "(?i)(.*?)(%s)$", filename ) ) ) {
					jarFile = new File( file );
					break;
				}
			}
		} catch ( IOException e ) {
			MobilisManager.getLogger().log(
					Level.WARNING,
					String.format( "Couldn't find jar file [%s] at [%s]: %s", filename, filepath,
							e.getMessage() ) );
		}

		return jarFile;
	}

	/**
	 * The listener interface for receiving IQ events. The class that is
	 * interested in processing a IQ event implements this interface, and the
	 * object created with that class is registered with a component using the
	 * component's <code>addIQListener<code> method. When
	 * the IQ event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see IQEvent
	 */
	private class IQListener implements PacketListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware
		 * .smack.packet.Packet)
		 */
		@Override
		public void processPacket( Packet packet ) {
			// log( "AdminService Incoming: " + packet.toXML() );

			if ( packet instanceof BeanIQAdapter ) {
				XMPPBean inBean = ( (BeanIQAdapter)packet ).getBean();
				
				//alle AdministrationBeans haben die Felder ServiceNamespace + ServiceVersion die f�r den Abgleich der Berechtigung n�tig sind
				if ( inBean instanceof AdministrationBean){
										
					//check user permission from Rostergroup
					Boolean permission = false;
					permission = checkServiceModifyPermission(((AdministrationBean) inBean).ServiceNamespace, ((AdministrationBean) inBean).ServiceVersion, inBean.getFrom());
										
					//if user has no permission handle as error
					if(!permission){
						handlePermissionError((AdministrationBean) inBean);
					} else if ( inBean instanceof InstallServiceBean ) {
						handleInstallServiceBean( (InstallServiceBean)inBean );
					} else if ( inBean instanceof ConfigureServiceBean ) {
						handleConfigureServiceBean( (ConfigureServiceBean)inBean );
					} else if ( inBean instanceof RegisterServiceBean ) {
						handleRegisterServiceBean( (RegisterServiceBean)inBean );
					} else if ( inBean instanceof UnregisterServiceBean ) {
						handleUnregisterServiceBean( (UnregisterServiceBean)inBean );
					} else if ( inBean instanceof UninstallServiceBean ) {
						handleUninstallServiceBean( (UninstallServiceBean)inBean );
					} else if ( inBean instanceof UpdateServiceBean ) {
						handleUpdateServiceBean( (UpdateServiceBean)inBean );
					} 
				
				}else {
					handleUnknownBean( inBean );
				}
			}
		}

		private void handlePermissionError(AdministrationBean inBean) {
			XMPPBean outBean = null;
			
			// generate error bean
			outBean = BeanHelper.CreateErrorBean( inBean, "modify", "not-acceptable",
					("User " + StringUtils.parseBareAddress(inBean.getFrom()) + " is not authorized to modify service " + inBean.ServiceNamespace + " version " + inBean.ServiceVersion + "!") );
			
			getAgent().getConnection().sendPacket( new BeanIQAdapter( outBean ) );
		}
			


		/**
		 * Handle error bean.
		 * 
		 * @param inBean
		 *            the unknown bean
		 */
		private void handleUnknownBean( XMPPBean inBean ) {
			getAgent().getConnection().sendPacket(
					new BeanIQAdapter( BeanHelper.CreateErrorBean( inBean, "wait",
							"unexpected-request", "This request is not supported" ) ) );
		}

		/**
		 * Handle install service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleInstallServiceBean( InstallServiceBean inBean ) {
			boolean installationSuccessful = false;
			String message = null;
			InstallServiceBean responseBean = new InstallServiceBean();
			
			
			//MANUELL INSTALLATION DISABLED
			//bei container.install() wird standardm��ig der defaultAgent der Runtime verwendet. Das f�hrt dann bei einem uninstall des Service zum l�schen des Runtime XMPP Accounts.
			//Um manuelles installieren wieder zu erm�glichen sind sehr umfangreiche �nderungen n�tig. Daher wird diese Methode der Dienstinstalltion bis auf weiteres deaktiviert.
			
			/*Boolean deployAllowed = true;
			// If user is in the deploy security rostergroup of the runtime, proceed. Else send not authorized error.
			if(!MobilisManager.getInstance().getRuntimeRoster().getGroup("deploy").contains(inBean.getFrom())){
				responseBean = (InstallServiceBean) BeanHelper.CreateErrorBean( inBean, "deploy", "not-acceptable",
						("User " + StringUtils.parseBareAddress(inBean.getFrom()) + " is not authorized to deploy services on runtime " + inBean.getTo() + "!") );
				deployAllowed = false;
			}
			
			//enter if installing user is in deploy group
			if(deployAllowed){
				
				
				// get previously uploaded service via DeploymentService
				ServiceContainer container = MobilisManager.getInstance().getPendingServiceByFileName(
						inBean.FileName );

				
				// Look for previously installed service containers
				if ( null == container ) {
					container = MobilisManager.getInstance().getServiceContainerByFileName(
							inBean.FileName );
				}
	
				// Look for all Jar-Archives in service folder
				if ( null == container ) {
					File jarFile = getExistingJarFile( UPLOADED_SERVICE_DICTIONARY_PATH,
							inBean.FileName );
	
					if ( null != jarFile ) {
						container = new ServiceContainer( jarFile );
					}
				}
				
				
				
				try {
					if ( null != container ) {
						ServiceContainer tempContainer = container;
						tempContainer.extractServiceContainerConfig();
						String servicename = tempContainer.getServiceName();
						int version = tempContainer.getServiceVersion();
						tempContainer = null;
						System.out.println("install: " + servicename + version);
						Roster runtimeRoster = MobilisManager.getInstance().getRuntimeRoster();
						RosterGroup rg = runtimeRoster.getGroup(servicename+version);
						Boolean modifyAllowed = true;
						
						//if rostergroup not null, service must be already existing
						if(rg!=null){
							//check if rostergroup contains requesting user
							modifyAllowed = rg.contains(StringUtils.parseBareAddress(inBean.getFrom()));
						} else {
							//if rostergroup doesn't exist, create it and add requesting user
							rg = runtimeRoster.createGroup(servicename+version);
							try {
								rg.addEntry(runtimeRoster.getEntry(StringUtils.parseBareAddress(inBean.getFrom())));
							} catch (XMPPException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						//reinstall just if allowed for requesting user
						if(modifyAllowed){
							container.install();
		
							// if installation was successful remove service from
							// pending services and add it to all service containers
							if ( container.getContainerState() == ServiceContainerState.INSTALLED ) {
								MobilisManager.getInstance().addServiceContainer( container );
		
								MobilisManager.getInstance().removePendingServiceByFileName(
										inBean.FileName );
		
								installationSuccessful = true;
							} else {
								message = "Installation failed: Couldn't move service from pending to regular.";
		
								MobilisManager.getLogger().log( Level.WARNING, message );
							}
		
							// create response
							responseBean = (InstallServiceBean)BeanHelper.CreateResultBean( inBean,
									responseBean );
							responseBean.InstallationSucessful = installationSuccessful;
							responseBean.ServiceNamespace = container.getServiceNamespace();
							responseBean.ServiceVersion = container.getServiceVersion();
							responseBean.Message = message;
						} else {
							//if modify is not allowed create Error Bean with Reason information
							responseBean = (InstallServiceBean) BeanHelper.CreateErrorBean( inBean, "modify", "not-acceptable",
									("User " + StringUtils.parseBareAddress(inBean.getFrom()) + " is not authorized to modify service" + container.getServiceNamespace() + " version " + container.getServiceVersion() + "!") );
						}
						
					} else {
						responseBean = (InstallServiceBean)BeanHelper
								.CreateErrorBean(
										inBean,
										"cancel",
										"item-not-found",
										"Service archive not found. Please upload service archive in jar-format via DeploymentService first." );
	
						MobilisManager.getLogger().log( Level.WARNING,
								"Service installation problem: " + responseBean.toXML() );
					}
				} catch ( NumberFormatException e ) {
					responseBean = (InstallServiceBean)BeanHelper.CreateErrorBean( inBean, "modify",
							"bad-request", "Service installation error: " + e.getMessage() );
	
					MobilisManager.getLogger().log( Level.WARNING,
							"Service installation error: " + responseBean.toXML() );
				} catch ( InstallServiceException e ) {
					responseBean = (InstallServiceBean)BeanHelper.CreateErrorBean( inBean, "wait",
							"internal-server-error", "Service installation error: " + e.getMessage() );
	
					MobilisManager.getLogger().log( Level.WARNING,
							"Service installation error: " + responseBean.toXML() );
				}
			}*/
			
			
			responseBean = (InstallServiceBean)BeanHelper.CreateErrorBean( inBean, "install", "not-acceptable",
					"Command INSTALL is not supported any longer, please use SEND instead!");
			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

		/**
		 * Handle configure service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleConfigureServiceBean( ConfigureServiceBean inBean ) {
			ConfigureServiceBean responseBean = new ConfigureServiceBean();

			// get service container
			ServiceContainer container = MobilisManager.getInstance().getServiceContainer(
					inBean.ServiceNamespace, inBean.ServiceVersion );

			// store configuration in container
			if ( null != container ) {
				DoubleKeyMap< String, String, Object > configuration = new DoubleKeyMap< String, String, Object >(
						false );
				
				// use data from admins agent / default if nothing was set
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "name",
						inBean.AgentConfig.Name != null
							? inBean.AgentConfig.Name
							: container.getServiceName() );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "mode",
						inBean.AgentConfig.Mode != null
							? inBean.AgentConfig.Mode
							: "single" );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "port",
						inBean.AgentConfig.Port != null
							? inBean.AgentConfig.Port
							: getAgent().getSettingString( "port" ) );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "username",
						inBean.AgentConfig.Username != null
							? inBean.AgentConfig.Username
							: getAgent().getSettingString( "username" ) );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "host",
						inBean.AgentConfig.Host != null
							? inBean.AgentConfig.Host
							: getAgent().getSettingString( "host" ) );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "start",
						inBean.AgentConfig.Start );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "description",
						inBean.AgentConfig.Description != null
							? inBean.AgentConfig.Description
							: container.getServiceName() );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "service",
						inBean.AgentConfig.Service != null
							? inBean.AgentConfig.Service
							: getAgent().getSettingString( "service" ) );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "resource",
						inBean.AgentConfig.Resource != null
							? inBean.AgentConfig.Resource
							: container.getServiceName() );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "type",
						inBean.AgentConfig.Type != null
							? inBean.AgentConfig.Type
							: "de.tudresden.inf.rn.mobilis.server.agents.MobilisAgent" );
				
				configuration.put( MobilisManager.CONFIGURATION_CATEGORY_AGENT_KEY, "password",
						inBean.AgentConfig.Password != null
							? inBean.AgentConfig.Password
							: getAgent().getSettingString( "password" ) );

				container.configure( configuration );

				responseBean = (ConfigureServiceBean)BeanHelper.CreateResultBean( inBean,
						responseBean );
			} else {
				responseBean = (ConfigureServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
						"service-unavailable", "Cannot configure unknown service" );

				MobilisManager.getLogger().log( Level.WARNING,
						"Service configuration error: " + responseBean.toXML() );
			}

			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

		/**
		 * Handle register service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleRegisterServiceBean( RegisterServiceBean inBean ) {
			RegisterServiceBean responseBean = new RegisterServiceBean();

			// get service container
			ServiceContainer container = MobilisManager.getInstance().getServiceContainer(
					inBean.ServiceNamespace, inBean.ServiceVersion );

			if ( null != container ) {
				try {
					container.register();

					responseBean = (RegisterServiceBean)BeanHelper.CreateResultBean( inBean,
							responseBean );
					responseBean.RegistrationSuccessful = container.getContainerState() == ServiceContainerState.ACTIVE;
				} catch ( RegisterServiceException e ) {
					responseBean = (RegisterServiceBean)BeanHelper.CreateErrorBean( inBean, "wait",
							"internal-server-error",
							"Service registration error: " + e.getMessage() );

					MobilisManager.getLogger().log( Level.WARNING,
							"Service registration error: " + responseBean.toXML() );
				}

			} else {
				responseBean = (RegisterServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
						"service-unavailable", "Cannot register unknown service" );

				MobilisManager.getLogger().log( Level.WARNING,
						"Service registration error: " + responseBean.toXML() );
			}

			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

		/**
		 * Handle unregister service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleUnregisterServiceBean( UnregisterServiceBean inBean ) {
			UnregisterServiceBean responseBean = new UnregisterServiceBean();
			ServiceContainer container = MobilisManager.getInstance().getServiceContainer(
					inBean.ServiceNamespace, inBean.ServiceVersion );

			if ( null != container ) {
				container.unregister();

				responseBean = (UnregisterServiceBean)BeanHelper.CreateResultBean( inBean,
						responseBean );
			} else {
				responseBean = (UnregisterServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
						"service-unavailable", "Cannot unregister unknown service" );

				MobilisManager.getLogger().log( Level.WARNING,
						"Service unregistration error: " + responseBean.toXML() );
			}

			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

		/**
		 * Handle uninstall service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleUninstallServiceBean( UninstallServiceBean inBean ) {
			UninstallServiceBean responseBean = new UninstallServiceBean();
			ServiceContainer container = MobilisManager.getInstance().getServiceContainer(
					inBean.ServiceNamespace, inBean.ServiceVersion );

			if ( null != container ) {
				container.uninstall();

				responseBean = (UninstallServiceBean)BeanHelper.CreateResultBean( inBean,
						responseBean );
			} else {
				responseBean = (UninstallServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
						"service-unavailable", "Cannot uninstall unknown service" );

				MobilisManager.getLogger().log( Level.WARNING,
						"Service unstallation error: " + responseBean.toXML() );
			}

			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

		/**
		 * Handle update service bean.
		 * 
		 * @param inBean
		 *            the bean
		 */
		private void handleUpdateServiceBean( UpdateServiceBean inBean ) {
			UpdateServiceBean responseBean = new UpdateServiceBean();
			ServiceContainer container = MobilisManager.getInstance().getServiceContainer(
					inBean.ServiceNamespace, inBean.ServiceVersion );

			if ( null != container ) {
				// look for jar file to update the old one
				File jarFile = getExistingJarFile( UPLOADED_SERVICE_DICTIONARY_PATH,
						inBean.FileName );

				if ( null != jarFile && jarFile.exists() ) {
					try {
						container.update( jarFile );

						responseBean = (UpdateServiceBean)BeanHelper.CreateResultBean( inBean,
								responseBean );
						responseBean.NewServiceNamespace = container.getServiceNamespace();
						responseBean.NewServiceVersion = container.getServiceVersion();
					} catch ( UpdateServiceException e ) {
						responseBean = (UpdateServiceBean)BeanHelper.CreateErrorBean( inBean,
								"cancel", "item-not-found",
								"Service update error: " + e.getMessage() );

						MobilisManager.getLogger().log( Level.WARNING,
								"Service update error: " + responseBean.toXML() );
					}
				} else {
					responseBean = (UpdateServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
							"item-not-found", "Cannot find new jar file to update the old one." );

					MobilisManager.getLogger().log( Level.WARNING,
							"Service update error: " + responseBean.toXML() );
				}
			} else {
				responseBean = (UpdateServiceBean)BeanHelper.CreateErrorBean( inBean, "cancel",
						"service-unavailable", "Cannot update non existing service!" );

				MobilisManager.getLogger().log( Level.WARNING,
						"Service update error: " + responseBean.toXML() );
			}

			getAgent().getConnection().sendPacket( new BeanIQAdapter( responseBean ) );
		}

	}
	
	private boolean checkServiceModifyPermission(String namespace, int version, String from){
		String serviceName ="";
		Roster runtimeRoster = MobilisManager.getInstance().getRuntimeRoster();
		if(namespace!=null && version>0){
			serviceName = MobilisManager.getInstance().getServiceContainer(namespace, version).getServiceName();
		}
		//Gruppe mit Dienstnamen und Version aus Roster holen
		//wenn gruppe existiert, existiert dienst
		if(runtimeRoster.getGroup(MobilisManager.securityUserGroup + serviceName+version)!=null){
			//In Gruppe schauen ob requestNutzer in Security Gruppe ist
			if(runtimeRoster.getGroup(MobilisManager.securityUserGroup + serviceName+version).getEntry(StringUtils.parseBareAddress(from))!=null){
				return true;
			} else {
				return false;
			}
		}
		//wenn gruppe nicht existiert, existiert dienst nicht. keine beschr�nkung f�r dienstzugriff
		return true;
	}
	
	@Override
	public List<PacketExtension> getNodePacketExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

}

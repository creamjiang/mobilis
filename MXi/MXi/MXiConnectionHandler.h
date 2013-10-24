//
//  ConnectionHandler.h
//  ACDSense
//
//  Created by Martin Weißbach on 8/24/13.
//  Copyright (c) 2013 Technische Universität Dresden. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "MXiConnection.h"
#import "MXiService.h"
#import "MXiMultiUserChatDelegate.h"

@class MXiMultiUserChatDiscovery;

/**
 *  The MXiConnectionServiceStateDelegate defines basic methods for objects to implement when information on
 *  the service availability are required.
*/
@protocol MXiConnectionServiceStateDelegate

typedef enum {
    MXiConnectionServiceConnected,
    MXiConnectionServiceUnconnected
} MXiConnectionServiceState;

/**
 *  This method is invoked whenever the service availability changes.
 *
 *  @param connectionState The new state of the client - service connection.
*/
- (void)connectionStateChanged:(MXiConnectionServiceState)connectionState;

@end

/**
 *  This block will be called when the authentication of the user finished. If the authentication was successfull
 *  the block will be called with 'YES' as parameter and 'NO' otherwise.
 *
 *  @param AuthenticationBlock The Block that will be executed after the authentication.
 */
typedef void (^ AuthenticationBlock)(BOOL);

/**
 *  This block will be called when the creation of a service finished successfully.
 *
 *  @param serviceJID The full JID of the created service instance.
*/
typedef void (^ ServiceCreateCompletionBlock)(NSString *);


/*!
    This block will be called when results of a multi user chat room discovery are available.

    @param serviceSupported This flag is 'YES' when the domain supports multi user chat rooms otherwise 'NO'
    @param discoveredRooms  A list of rooms that are discovered for the respective domain.
                            This flag will be _nil_ when the service does not support XEP-0045 and empty if no rooms were discovered.
 */
typedef void (^DiscoveryCompletionBlock)(BOOL serviceSupported, NSArray *discoveredRooms);

/**
 *  The ConnectionHandler class provides global-level information of the XMPP connection to an XMPP server.
 */
@interface MXiConnectionHandler : NSObject <MXiBeanDelegate, MXiPresenceDelegate, MXiStanzaDelegate>

@property (strong) NSArray* discoveredServiceInstances;

@property (nonatomic) MXiMultiUserChatDiscovery *multiUserChatDiscovery;

/**
 *  Returns a ConnectionHandler object that manages all relevant information on the connection and incoming and
 *  outgoing stanzas.
 *
 *  Because there is only one active connection supposed to exist at a time, always the same instance of the object
 *  will be returned.
 *
 *  @see ConnectionHandlerDelegate protocol
 *
 *  @return The ConnectionHandler object.
 */
+ (instancetype)sharedInstance;

+ (instancetype) alloc __attribute__((unavailable("alloc not available, call sharedInstance instead")));
- (instancetype) init __attribute__((unavailable("init not available, call sharedInstance instead")));
+ (instancetype) new __attribute__((unavailable("new not available, call sharedInstance instead")));

/**
 *  Set up a new Connection to a XMPP server. The credentials used in this method will automatically be
 *  stored in the Keychains which means old account information will be overridden.
 *
 *  @param jabberID        The full or bare JID of the user who's registered at the XMPP server.
 *  @param password        The user's password associated with the JID and XMPP server.
 *  @param hostName        The host name of XMPP server, e.g. 'jabber.org'.
 *  @param serviceType     The type of the service which is either MULTI or SINGLE.
 *  @param port            The port under which the XMPP server is available, usually 5222.
 *  @param authentication  Callback block to inform the application on the success of the authentication.
 */
- (void)launchConnectionWithJID:(NSString *)jabberID
                       password:(NSString *)password
                       hostName:(NSString *)hostName
                    serviceType:(enum _ServiceType)serviceType
                           port:(NSNumber *)hostPort
            authenticationBlock:(AuthenticationBlock)authentication;

/**
 *  Use already existing connection delegates and just reconfigure the connection to the server.
 *
 *  @param jabberID       The full or bare JID of the user who's registered at the XMPP server.
 *  @param password       The user's password associated with the JID and XMPP server.
 *  @param hostName       The host name of XMPP server, e.g. 'jabber.org'.
 *  @param port           The port under which the XMPP server is available, usually 5222.
 *  @param authentication Callback block to inform the sender on the success of the authentication.
 */
- (void)reconnectWithJID:(NSString *)jabberID
                password:(NSString *)password
                hostName:(NSString *)hostName
                    port:(NSNumber *)port
     authenticationBlock:(AuthenticationBlock)authentication;

/*!
    Create a new instance of the service the receiver is connected to.

    @param serviceName      The name of the service instance that is supposed to be created.
    @param completionBlock  The block that will be executed when the creation of the instance finished.
 */
- (void)createServiceWithName:(NSString *)serviceName completionBlock:(ServiceCreateCompletionBlock)completionBlock;

/**
 *  This method realizes client-server communication and sends outgoing beans to the service.
 *
 *  @param outgoingBean Bean object that should be send to the service.
 *
 *  @see MXiOutgoingBean protocol
 */
- (void)sendBean:(MXiBean<MXiOutgoingBean> *)outgoingBean;

/*!
    Send a stanza of any kind to the XMPP server.

    @param element The XML stanza that is supposed to be transfered.
 */
- (void)sendElement:(NSXMLElement *)element;

/**
 *  Various objects might be interested in incoming beans, but not all of them are interested in all incoming beans.
 *  This method allows objects to register as a delegate for only a specific bean class.
 *
 *  @param delegate  Object that wants to act as a delegate for certain bean classes.
 *  @param selector  The selector of the delegate that will be called when a bean of the given class arrives.
 *  @param beanClass The class of the bean for which the delegate registers.
 *
 */
- (void)addDelegate:(id)delegate withSelector:(SEL)selector forBeanClass:(Class)beanClass;
/*!
    Unregister an object from being notified about incoming beans.

    @param delegate     The delegate object that offers the method that should be removed from the list of invoked methods when a bean comes in.
    @param selector     The selector of the delegate that will no longer be invoked when a bean of a given class comes in.
    @param beanClass    The class of the bean for which the delegate did unregister.
 */
- (void)removeDelegate:(id)delegate withSelector:(SEL)selector forBeanClass:(Class)beanClass;

/**
 *  Objects that are interested in listening to state changes of the overall service available can register as delegates.
 *  Whenever the service becomes available or unavailable all registered delegates will be notified.
 *
 *  @param delegate The object that wants to be notified on service availability changes.
 *
 *  @see MXiConnectionServiceStateDelegate
*/
- (void)addDelegate:(id<MXiConnectionServiceStateDelegate>)delegate;

/**
 *  Remove an object from the receiver's list of objects that want to be notified on service availability changes.
 *
 *  @param delegate The object that should be removed from the receiver's list of delegates.
 *
 *  @see MXiConnectionServiceStateDelegate
*/
- (void)removeDelegate:(id<MXiConnectionServiceStateDelegate>)delegate;

- (void)addStanzaDelegate:(id)delegate;
- (void)removeStanzaDelegate:(id)delegate;

#pragma mark - Multi User Chat support

/*!
    This method is used to discover all available Multi User Chat rooms for a given domain.
    Discovery follows XEP-0045 specification.

    @param domain           The domain that offers the multi user chat rooms.
    @param completionBlock  The completion block that is called when results for the discovery are available.
                            The completion block might be called several times if more than one entity of the given domain
                            provides Multi-User-Chat-Rooms.
 */
- (void)discoverMultiUserChatRoomsInDomain:(NSString *)domain withCompletionBlock:(DiscoveryCompletionBlock)completionBlock;

/*!
    This method will connect to a multi user chat room specified by a given jabber ID of the room.

    @param roomJID The jabber ID of the multi user chat room to connect to.
    @param delegate The delegate incoming multi user chat messages will be delegated to.

    @warning *Important:* If the delegate is _nil_ and no delegate has been set before, this method will throw an exception of kind NSException.

    @see isMultiUserChatDelegateSet
 */
- (void)connectToMultiUserChatRoom:(NSString *)roomJID withDelegate:(id <MXiMultiUserChatDelegate>)delegate;

/**
    Disconnects from the room specified by a given JID.

    @param roomJID The jabberID of the room that sould be left.
*/
- (void)leaveMultiUserChatRoom:(NSString *)roomJID;
/*!
    Sends a groupchat message to a multi user chat room specified by a given JID.

    @param message  The body of the message as a string.
    @param roomJID  The jabber ID of the room this message is addressed to.
    @param userName The resource of the user within the chat room;
 */
- (void)sendMessage:(NSString *)message toRoom:(NSString *)roomJID;
- (void)sendMessage:(NSString *)message toRoom:(NSString *)roomJID toUser:(NSString *)userName;
/*!
    Tells the sender whether a multi user chat delegate is already set.

    @return `YES` if a delegate was already set, otherwise `NO`.
 */
- (BOOL)isMultiUserChatDelegateSet;

@end

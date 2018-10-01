package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Updated by gakwaya on Oct/08/2017.
 */
public class RosterConnection implements ConnectionListener {

    private static final String TAG = RosterConnection.class.getSimpleName();

    private final Context mApplicationContext;
    private final String mUsername;
    private final String mPassword;
    private final String mServiceName;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private Roster mRoster;


    public enum ConnectionState {
        CONNECTED, AUTHENTICATED, CONNECTING, DISCONNECTING, DISCONNECTED, FAILURE;
    }

    public static enum LoggedInState {
        LOGGED_IN, LOGGED_OUT;
    }


    public RosterConnection(Context context) {
        Log.d(TAG, "RosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password", null);

        if (jid != null) {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }

    public RosterConnection(Context context, String jid, String password) {
        Log.d(TAG, "RosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String mJID = jid;
        mPassword = password;

        if (mJID != null) {
            mUsername = mJID.split("@")[0];
            mServiceName = mJID.split("@")[1];
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }


    public void connect() throws IOException, XMPPException, SmackException {

        if (RosterManager.getInstance().getHost() == null) throw new IllegalStateException("Initiation Exception: Initiate RosterManager First, RosterManager.init() with ApplicationContext()");

        Log.d(TAG, "Connecting to server " + mServiceName);
        String host = RosterManager.getInstance().getHost();
        String hostAddr = host.split("//")[1];

//        InetAddress addr = InetAddress.getByName("10.14.10.20");
        InetAddress addr = InetAddress.getByName(hostAddr);
        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return false;
            }
        };

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(mServiceName)
//                .setHost("http://10.14.10.20")
                .setHost(host)
                .setHostnameVerifier(verifier)
                .setHostAddress(addr)
                .setResource("Roster")

                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
                .setKeystoreType(null) //This line seems to getInstance rid of the problem

                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setCompressionEnabled(false).build();

        Log.d(TAG, "Username : " + mUsername);
        Log.d(TAG, "Password : " + mPassword);
        Log.d(TAG, "Server : " + mServiceName);


        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(conf);

        mConnection.addConnectionListener(this);

        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                Log.d(TAG, "message.getBody() :" + message.getBody());
                Log.d(TAG, "message.getFrom() :" + message.getFrom());

                String from = message.getFrom().toString();

                String contactJid = "";
                if (from.contains("/")) {
                    contactJid = from.split("/")[0];
                    Log.d(TAG, "The real jid is :" + contactJid);
                    Log.d(TAG, "The message is from :" + from);
                } else {
                    contactJid = from;
                }

                Log.e(TAG, "Received message from :" + contactJid + " data sent.");
                RosterManager.getInstance().notifyRecieveMessage(contactJid, message.getBody());

            }
        });

        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername, mPassword);
            Log.d(TAG, " login() Called ");
        } catch (InterruptedException e) {
            RosterManager.getInstance().notifyConntectionState(null, ConnectionState.FAILURE);
            e.printStackTrace();
        }


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }

    private void setupUiThreadBroadCastMessageReceiver() {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if (action.equals(RosterConnectionService.SEND_MESSAGE)) {
                    //Send the message.
                    sendMessage(intent.getStringExtra(RosterConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(RosterConnectionService.BUNDLE_TO));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RosterConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter);
    }

    private void sendMessage(String body, String toJid) {
        Log.d(TAG, "Sending message to :" + toJid);

        EntityBareJid jid = null;


        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);

        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            chat.send(message);
            RosterManager.getInstance().notifySendMessage(toJid, body, true);

        } catch (SmackException.NotConnectedException e) {
            RosterManager.getInstance().notifySendMessage(toJid, body, false);
            e.printStackTrace();
        } catch (InterruptedException e) {
            RosterManager.getInstance().notifySendMessage(toJid, body, false);
            e.printStackTrace();
        }
    }


    public void disconnect() {
        Log.d(TAG, "Disconnecting from serser " + mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in", false).commit();


        if (mConnection != null) {
            mConnection.disconnect();
        }

        mConnection = null;
        // Unregister the message broadcast receiver.
        if (uiThreadMessageReceiver != null) {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        RosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Connected Successfully");
        RosterManager.getInstance().notifyConntectionState(connection, ConnectionState.CONNECTED);

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        RosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Authenticated Successfully");
        mRoster = Roster.getInstanceFor(connection); //
        RosterManager.getInstance().setRoster(mRoster);
        mRoster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                Log.e(TAG, "Entries Added: " + addresses.toString());
                RosterManager.getInstance().notifyContactAdded(addresses);
            }

            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                Log.e(TAG, "Entries Updated: " + addresses.toString());
                RosterManager.getInstance().notifyContactUpdated(addresses);
            }

            @Override
            public void entriesDeleted(Collection<Jid> addresses) {
                Log.e(TAG, "Entries Updated: " + addresses.toString());
                RosterManager.getInstance().notifyContactRemoved(addresses);
            }

            @Override
            public void presenceChanged(Presence presence) {
                Log.e("entry", "Presence changed: " + presence.getFrom() + " " + presence);
                Log.e(TAG, "Presence Changed :" + presence.getFrom() + " update: " + presence.getType());
                RosterManager.getInstance().notifyPresenceChanged(presence.getFrom().asBareJid().toString(), presence.getType().toString());
            }
        });

        RosterManager.getInstance().notifyConntectionState(connection, ConnectionState.AUTHENTICATED);

//        showContactListActivityWhenAuthenticated();
    }

    public Roster getRoster() {
        return mRoster;
    }

    public void reloadRosterAndWait() throws SmackException.NotLoggedInException, InterruptedException, SmackException.NotConnectedException {
        if (mRoster != null) mRoster.reloadAndWait();
    }

    @Override
    public void connectionClosed() {
        RosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        RosterManager.getInstance().notifyConntectionState(mConnection, ConnectionState.DISCONNECTED);
        Log.d(TAG, "Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        RosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        RosterManager.getInstance().notifyConntectionState(null, ConnectionState.DISCONNECTED);
        Log.d(TAG, "ConnectionClosedOnError, error " + e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        RosterConnectionService.sConnectionState = ConnectionState.CONNECTING;
        RosterManager.getInstance().notifyConntectionState(mConnection, ConnectionState.CONNECTING);
        Log.d(TAG, "ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        RosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        RosterManager.getInstance().notifyConntectionState(mConnection, ConnectionState.CONNECTED);

        Log.d(TAG, "ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        RosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        RosterManager.getInstance().notifyConntectionState(mConnection, ConnectionState.DISCONNECTED);
        Log.d(TAG, "ReconnectionFailed()");
    }

    private void showContactListActivityWhenAuthenticated() {
//        Intent i = new Intent(RosterConnectionService.UI_AUTHENTICATED);
//        i.setPackage(mApplicationContext.getPackageName());
//        mApplicationContext.sendBroadcast(i);
//        Log.d(TAG, "Sent the broadcast that we are authenticated");
    }
}

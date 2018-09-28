package com.nfs.mobility.chat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.allyants.notifyme.NotifyMe;
import com.blikoon.roster.R;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import co.intentservice.chatui.models.ChatMessage;

class RosterManager {
    private static final RosterManager ourInstance = new RosterManager();
    private static final String TAG = RosterManager.class.getSimpleName();
    private RosterConnection.ConnectionState mConnectionState;
    private boolean mShowNotifications;

    static RosterManager getInstance() {
        if (ourInstance == null)
            return new RosterManager();
        else
            return ourInstance;
    }

    private RosterManager() {
    }


    private Set<OnMessageChangeListener> mMessageChangeListeners = new CopyOnWriteArraySet<>();
    private Set<OnRosterUpdatesListener> mRosterUpdatesListeners = new CopyOnWriteArraySet<>();
    private Set<OnConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArraySet<>();

    Context mApplicationContext;

    Long authenticateTime;

    private Thread mThread;
    private Handler mTHandler;//We use this handler to post messages to
    //the background thread.
    private RosterConnection mConnection;
    private Roster mRoster;

    private void initiateThread(){
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    Looper.prepare();
                    mTHandler = new Handler();
                    //THE CODE HERE RUNS IN A BACKGROUND THREAD.
                    Looper.loop();
                }
            });
            mThread.start();
        }
    }

    public void init(final Context context){
        /**
         * Initiate Connection
         * Notify Connected
         * Notify Authenticate
         * Notify Failure
         */
        mApplicationContext = context.getApplicationContext();
        initiateThread();
    }

    public void connectUser(final String jid, final String password) {
        validateState();
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                initConnection(mApplicationContext, jid, password);
            }
        });
    }

    private void validateState() {
        if(mApplicationContext == null) throw new IllegalStateException("Initiation Exception: Initiate RosterManager First, RosterManager.init() with ApplicationContext()");
        if(mThread == null) throw new IllegalStateException("Initiation Exception: Initiate RosterManager First, Thread is not initialized");
        if(mTHandler == null) throw new IllegalStateException("Initiation Exception: Initiate RosterManager First, Thread Handler is not initialized");
    }

    public void disconnectUser(){
        validateState();
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                closeConnection();
            }
        });

        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                cleanUpManager();
            }
        });
    }

    private void initConnection(Context context, String jid, String password) {
        Log.d(TAG, "initConnection()");
        if (mConnection == null) {
            mConnection = new RosterConnection(context, jid, password);
        }
        try {
            mConnection.connect();

        } catch (IOException | SmackException | XMPPException e) {
            Log.d(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again");
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    public void loadContacts (){
        validateState();
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                updateRoster();
            }
        });
    }

    private void updateRoster() {
        if (mConnection == null) return;
        try {
            mConnection.reloadRosterAndWait();
            updateContacts();
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    public void updateContacts() {

        if(mConnection == null || mConnection.getRoster() == null) return;
//        Roster roster = mConnection.getRoster();

        Collection<RosterEntry> entries = mRoster.getEntries(); //
        Collection<Jid> addresses = new ArrayList<>();
        Log.e(TAG, entries.toString());
        List<Contact> mContacts = new ArrayList<>(); //
        for (RosterEntry entry : entries) {
            //HashMap<String, String> map = new HashMap<String,String>();
            Presence entryPresence = mRoster.getPresence(entry.getJid());
            Presence.Type type = entryPresence.getType();
            Contact contact = new Contact(entry.getName(), type.toString(), entry.getJid().toString());
            Log.e(TAG, contact.toString());
            mContacts.add(contact);
            addresses.add(entry.getJid());
        }
        ContactRepository.getInstance().setContacts(mContacts);

        for(OnRosterUpdatesListener listener : mRosterUpdatesListeners){
            listener.onUpdateContact(addresses);
        }
        return;
    }

    public Long getAuthenticateTime() {
        return authenticateTime;
    }

    public void setAuthenticateTime(Long authenticateTime) {
        this.authenticateTime = authenticateTime;
    }

    public void notifyRecieveMessage(String jid, String messageBody) {
        int count = -1;
        ChatMessage chatMessage = new ChatMessage(messageBody, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);

        count = MessageRepository.getInstance().addMessage(jid, chatMessage);

        if(mShowNotifications){
            showNotifications(jid, messageBody);
        }

        for (OnMessageChangeListener listener : mMessageChangeListeners) {
            listener.onMessageReceived(jid, messageBody, count);
        }
    }

    private void showNotifications(String fromJID, String newMessage) {
        validateState();
        Contact contact = ContactRepository.getInstance().getContact(fromJID);
        if(contact == null) return;
        int red = 0;
        int green = 102;
        int blue = 204;
        int alpha = 255;
        NotifyMe.Builder notifyMe = new NotifyMe.Builder(mApplicationContext);
        notifyMe.title(contact.getUserName());
        notifyMe.content(newMessage);
        notifyMe.color(red, green, blue, alpha);//Color of notification header
        notifyMe.delay(1000);//Delay in ms
        notifyMe.key("new_message");
        notifyMe.build();
    }

    public void notifySendMessage(String jid, String messageBody, boolean success) {
        int count = -1;
        if (success) {
            ChatMessage chatMessage = new ChatMessage(messageBody, System.currentTimeMillis(), ChatMessage.Type.SENT);
            count = MessageRepository.getInstance().addMessage(jid, chatMessage);
        }

        for (OnMessageChangeListener listener : mMessageChangeListeners) {
            listener.onMessageSent(jid, messageBody, count, success);
        }
    }

    public void notifyPresenceChanged(String jid, String status) {
        ContactRepository.getInstance().setStatus(jid, status);
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onChangePresence(jid, status);
        }
    }

    public void notifyContactAdded(Collection<Jid> addresses) {
        ContactRepository.getInstance().addContacts(addresses);
        loadContacts();
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onAddContact(addresses);
        }
    }

    public void notifyContactUpdated(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        loadContacts();
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onUpdateContact(addresses);
        }
    }

    public void notifyContactRemoved(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        loadContacts();
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onRemoveContact(addresses);
        }
    }

    public void notifyConntectionState(XMPPConnection connection, RosterConnection.ConnectionState connectionState) {
        mConnectionState = connectionState;
        loadContacts();
        for (OnConnectionStateListener listener : mConnectionStateListeners) {
            listener.onConnectionStateChange(connectionState);
        }
        if(connectionState.equals(RosterConnection.ConnectionState.AUTHENTICATED)){
            setAuthenticateTime(System.currentTimeMillis());
            updateRoster();
        }
    }

    public void addOnMessageChangeListener(OnMessageChangeListener messageChangeListener) {
        mMessageChangeListeners.add(messageChangeListener);
    }

    public void removeOnMessageChangeListener(OnMessageChangeListener messageChangeListener) {
        mMessageChangeListeners.remove(messageChangeListener);
    }

    public void addOnRosterChangeListener(OnRosterUpdatesListener rosterChangesListener){
        mRosterUpdatesListeners.add(rosterChangesListener);
    }
    public void removeOnRosterChangeListener(OnRosterUpdatesListener rosterChangesListener){
        mRosterUpdatesListeners.remove(rosterChangesListener);
    }

    public void cleanUpManager() {
        MessageRepository.getInstance().cleanUpMessages();
        ContactRepository.getInstance().cleanUpContacts();
        if (mMessageChangeListeners != null) mMessageChangeListeners.clear();
//        if (mConnectionStateListeners != null) mConnectionStateListeners.clear();
        if (mRosterUpdatesListeners != null) mRosterUpdatesListeners.clear();
    }

    public void addOnConnectionStateListener(OnConnectionStateListener connectionStateListener){
        mConnectionStateListeners.add(connectionStateListener);
    }
    public void removeOnConnectionStateListener(OnConnectionStateListener connectionStateListener){
        mConnectionStateListeners.remove(connectionStateListener);
    }

    public void setRoster(Roster roster) {
        mRoster = roster;
    }

    public void enableNotifications(boolean enable) {
        mShowNotifications = enable;
    }

    public interface OnMessageChangeListener {
        void onMessageReceived(String fromJID, String newMessage, int totalCount);

        void onMessageSent(String toJID, String newMessage, int totalCount, boolean success);

        void onMessageDeleted(String jid, String message, int totalCount);
    }

    public interface OnRosterUpdatesListener {
        void onChangePresence(String jid, String status);

        void onRemoveContact(Collection<Jid> addresses);

        void onUpdateContact(Collection<Jid> addresses);

        void onAddContact(Collection<Jid> addresses);
    }

    public interface OnConnectionStateListener {
        void onConnectionStateChange(RosterConnection.ConnectionState connectionState);
    }
}

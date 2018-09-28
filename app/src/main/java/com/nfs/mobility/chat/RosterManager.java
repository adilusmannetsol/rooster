package com.nfs.mobility.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import co.intentservice.chatui.models.ChatMessage;

class RosterManager {
    private static final RosterManager ourInstance = new RosterManager();
    private static final String TAG = RosterManager.class.getSimpleName();
    private RosterConnection.ConnectionState mConnectionState;

    static RosterManager getInstance() {
        if (ourInstance == null)
            return new RosterManager();
        else
            return ourInstance;
    }

    private RosterManager() {
    }


    Set<OnMessageChangeListener> mMessageChangeListeners = new CopyOnWriteArraySet<>();
    Set<OnRosterUpdatesListener> mRosterUpdatesListeners = new CopyOnWriteArraySet<>();
    Set<OnConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArraySet<>();

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

    public void init(final Context context, final String jid, final String password){
        /**
         * Initiate Connection
         * Notify Connected
         * Notify Authenticate
         * Notify Failure
         */
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                initConnection(context.getApplicationContext(), jid, password);
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

    private void loadContacts(){

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

        for (OnMessageChangeListener listener : mMessageChangeListeners) {
            listener.onMessageReceived(jid, messageBody, count);
        }
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
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onAddContact(addresses);
        }
    }

    public void notifyContactUpdated(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onUpdateContact(addresses);
        }
    }

    public void notifyContactRemoved(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        for (OnRosterUpdatesListener listener : mRosterUpdatesListeners) {
            listener.onRemoveContact(addresses);
        }
    }

    public void notifyConntectionState(XMPPConnection connection, RosterConnection.ConnectionState connectionState) {
        mConnectionState = connectionState;
        for (OnConnectionStateListener listener : mConnectionStateListeners) {
            listener.onConnectionStateChange(connectionState);
        }
        if(connectionState.equals(RosterConnection.ConnectionState.AUTHENTICATED)){

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
        if (mMessageChangeListeners != null) mMessageChangeListeners.clear();
        if (mConnectionStateListeners != null) mConnectionStateListeners.clear();
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

    public interface OnMessageChangeListener {
        void onMessageReceived(String fromJID, String newMessage, int totalCount);

        void onMessageSent(String toJID, String newMessage, int totalCount, boolean success);

        void onMessageDeleted(String jid, String message, int totalCount);
    }

    public interface OnRosterUpdatesListener {
        void onChangePresence(String jid, String status);

        void onRemoveContact(Collection<Jid> addresses);

        void onUpdateContact(Collection<Jid> addresses);

        void onDeleteContact();

        void onAddContact(Collection<Jid> addresses);
    }

    public interface OnConnectionStateListener {
        void onConnectionStateChange(RosterConnection.ConnectionState connectionState);
    }
}

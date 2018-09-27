package com.nfs.mobility.chat;

import org.jxmpp.jid.Jid;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import co.intentservice.chatui.models.ChatMessage;

class RoosterManager {
    private static final RoosterManager ourInstance = new RoosterManager();

    static RoosterManager getInstance() {
        if (ourInstance == null)
            return new RoosterManager();
        else
            return ourInstance;
    }

    private RoosterManager() {
    }


    Set<OnMessageChangeListener> mMessageChangeListeners = new CopyOnWriteArraySet<>();
    Set<OnRoosterUpdatesListener> mRoosterUpdatesListeners = new CopyOnWriteArraySet<>();

    Long authenticateTime;

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
        for (OnRoosterUpdatesListener listener : mRoosterUpdatesListeners) {
            listener.onChangePresence(jid, status);
        }
    }

    public void notifyContactAdded(Collection<Jid> addresses) {
        ContactRepository.getInstance().addContacts(addresses);
        for (OnRoosterUpdatesListener listener : mRoosterUpdatesListeners) {
            listener.onAddContact(addresses);
        }
    }

    public void notifyContactUpdated(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        for (OnRoosterUpdatesListener listener : mRoosterUpdatesListeners) {
            listener.onUpdateContact(addresses);
        }
    }

    public void notifyContactRemoved(Collection<Jid> addresses) {
        ContactRepository.getInstance().updateContacts(addresses);
        for (OnRoosterUpdatesListener listener : mRoosterUpdatesListeners) {
            listener.onRemoveContact(addresses);
        }
    }

    public void addOnMessageChangeListener(OnMessageChangeListener messageChangeListener) {
        mMessageChangeListeners.add(messageChangeListener);
    }

    public void removeMessageChangeListener(OnMessageChangeListener messageChangeListener) {
        mMessageChangeListeners.remove(messageChangeListener);
    }

    public void addOnRoosterChangeListener(OnRoosterUpdatesListener roosterChangesListener){
        mRoosterUpdatesListeners.add(roosterChangesListener);
    }
    public void removeOnRoosterChangeListener(OnRoosterUpdatesListener roosterChangesListener){
        mRoosterUpdatesListeners.remove(roosterChangesListener);
    }

    public void cleanUpManager() {
        MessageRepository.getInstance().cleanUpMessages();
        if (mMessageChangeListeners != null) mMessageChangeListeners.clear();
    }

    public interface OnMessageChangeListener {
        void onMessageReceived(String fromJID, String newMessage, int totalCount);

        void onMessageSent(String toJID, String newMessage, int totalCount, boolean success);

        void onMessageDeleted(String jid, String message, int totalCount);
    }

    public interface OnRoosterUpdatesListener {
        void onChangePresence(String jid, String status);

        void onRemoveContact(Collection<Jid> addresses);

        void onUpdateContact(Collection<Jid> addresses);

        void onDeleteContact();

        void onAddContact(Collection<Jid> addresses);
    }
}

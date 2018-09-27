package com.nfs.mobility.chat;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.intentservice.chatui.models.ChatMessage;

public class MessageRepository {

    private static MessageRepository sMessageRepo;
    private Map<String, List<ChatMessage>> mChatMessages = new HashMap<>();

    public static MessageRepository getInstance() {
        if (sMessageRepo == null) {
            sMessageRepo = new MessageRepository();
        }
        return sMessageRepo;
    }

    private MessageRepository() {

    }


    private Map<String, List<ChatMessage>> getChatData() {
        return mChatMessages;
    }

    private void populateWithInitialMessages(Context context) {
        //Create the ChatMessage and add them to the JID;

    }


    private void setChatData(Map<String, List<ChatMessage>> chatData) {
        this.mChatMessages = chatData;
    }

    public List<ChatMessage> getMessages(String jid) {
        if (!mChatMessages.containsKey(jid))
            mChatMessages.put(jid, new ArrayList<ChatMessage>());
        return mChatMessages.get(jid);

    }

    public int addMessage(String jid, ChatMessage chatMessage) {
        if (!mChatMessages.containsKey(jid))
            mChatMessages.put(jid, new ArrayList<ChatMessage>());
        List<ChatMessage> chatMessageList = mChatMessages.get(jid);
        chatMessageList.add(chatMessage);
        mChatMessages.put(jid, chatMessageList);
        return mChatMessages.get(jid).size();
    }

    public int removeMessage(String jid, ChatMessage chatMessage) {
        if (!mChatMessages.containsKey(jid))
            mChatMessages.put(jid, new ArrayList<ChatMessage>());
        List<ChatMessage> chatMessageList = mChatMessages.get(jid);
        chatMessageList.remove(chatMessage);
        mChatMessages.put(jid, chatMessageList);
        return mChatMessages.get(jid).size();
    }

    public void clearMessages(String jid) {
        mChatMessages.remove(jid);
    }

    public void cleanUpMessages() {
        mChatMessages.clear();
    }

}

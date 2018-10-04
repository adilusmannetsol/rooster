package com.nfs.mobility.chat;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.blikoon.roster.R;
import com.mobility.chat.xmpp.MessageRepository;
import com.mobility.chat.xmpp.RosterConnection;
import com.mobility.chat.xmpp.RosterManager;

import java.util.ArrayList;
import java.util.List;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ChatContactsActivity}
 * in two-pane mode (on tablets) or a {@link ChatDetailActivity}
 * on handsets.
 */
public class ChatMessagesFragment extends Fragment {
    public static final String ARG_ITEM_JID = "item_jid";
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */

    private static final String TAG = ChatMessagesFragment.class.getSimpleName();
    private String contactJid;

    private ChatView mChatView;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    RosterManager.OnMessageChangeListener messageChangeListener = new RosterManager.OnMessageChangeListener() {
        @Override
        public void onMessageReceived(String fromJID, String newMessage, int totalCount) {
            Log.e(TAG, "OnMessageChangeListener: onMessageReceived: " + fromJID + " ---> " + newMessage + " ---> " + totalCount);
            updateMessageList();
        }

        @Override
        public void onMessageSent(String toJID, String newMessage, int totalCount, boolean success) {
            Log.e(TAG, "OnMessageChangeListener: onMessageSent: " + toJID + " ---> " + success + " ---> " + totalCount);
            updateMessageList();
        }

        @Override
        public void onMessageDeleted(String jid, String message, int totalCount) {

        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatMessagesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_JID))
            contactJid = getArguments().getString(ARG_ITEM_JID);

        RosterManager.getInstance().addOnMessageChangeListener(messageChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_chat_view, container, false);

        mChatView = rootView.findViewById(R.id.roster_chat_view);

        mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
            @Override
            public boolean sendMessage(ChatMessage chatMessage) {
                // perform actual message sending
                if (RosterManager.getInstance().getConnectionState().equals(RosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sending Message");
                    //Send the message to the server

                    Intent intent = new Intent(RosterConnection.SEND_MESSAGE);
                    intent.putExtra(RosterConnection.BUNDLE_MESSAGE_BODY,
                            mChatView.getTypedMessage());
                    intent.putExtra(RosterConnection.BUNDLE_TO, contactJid);

                    getContext().sendBroadcast(intent);

                } else {
                    Toast.makeText(getContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMessageList();
    }

    @Override
    public void onDestroy() {
        cleanUpRosterListeners();
        super.onDestroy();
    }

    void cleanUpRosterListeners() {
        RosterManager.getInstance().removeOnMessageChangeListener(messageChangeListener);
    }

    private void updateMessageList() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                List<com.mobility.chat.xmpp.model.ChatMessage> chatMessageList = MessageRepository.getInstance().getMessages(contactJid);
                // This is done to cast the XMPP ChatMessage to UI ChatMessage
                List<ChatMessage> chatMessages = new ArrayList<>();
                for (com.mobility.chat.xmpp.model.ChatMessage chatMessage : chatMessageList) {
                    ChatMessage.Type type = ChatMessage.Type.RECEIVED;
                    switch (chatMessage.getType()) {
                        case 0: // SENT
                            type = ChatMessage.Type.SENT;
                            break;
                        case 1: // RECEIVED
                            type = ChatMessage.Type.RECEIVED;
                            break;

                        default:
                            break;
                    }
                    ChatMessage chatMessage1 = new ChatMessage(chatMessage.getMessage(),
                            chatMessage.getTimestamp(),
                            type,
                            chatMessage.getSender());
                    chatMessages.add(chatMessage1);
                }
                mChatView.clearMessages();
                mChatView.addMessages(new ArrayList<ChatMessage>(chatMessages));
            }
        });
    }

}

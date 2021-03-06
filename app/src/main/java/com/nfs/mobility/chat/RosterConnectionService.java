package com.nfs.mobility.chat;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mobility.chat.xmpp.ContactRepository;
import com.mobility.chat.xmpp.RosterConnection;
import com.mobility.chat.xmpp.model.Contact;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by adilusman on Oct/2018.
 */
public class RosterConnectionService extends Service {
    public static final String UI_AUTHENTICATED = "mobility.chat.UIAuthenticated";
    public static final String SEND_MESSAGE = "mobility.chat.SendMessage";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";
    public static final String NEW_MESSAGE = "mobility.chat.NewMessage";
    public static final String PRESENCE_CHANGED = "mobility.chat.PresenceChanged";
    public static final String CONTACTS_UPDATED = "mobility.chat.ContactSupdated";
    public static final String BUNDLE_FROM_JID = "b_from";
    public static final String BUNDLE_PRESENCE_TYPE = "b_type";
    private static final String TAG = RosterConnectionService.class.getSimpleName();
    public static RosterConnection.ConnectionState sConnectionState;
    public static RosterConnection.LoggedInState sLoggedInState;
    private boolean mActive;//Stores whether or not the thread is active
    private Thread mThread;
    private Handler mTHandler;//We use this handler to post messages to
    //the background thread.
    private RosterConnection mConnection;

    public RosterConnectionService() {

    }

    public static RosterConnection.ConnectionState getState() {
        if (sConnectionState == null) {
            return RosterConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    public static RosterConnection.LoggedInState getLoggedInState() {
        if (sLoggedInState == null) {
            return RosterConnection.LoggedInState.LOGGED_OUT;
        }
        return sLoggedInState;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }

    private void initConnection() {
        Log.d(TAG, "initConnection()");
        String jid = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("xmpp_jid", null);
        String mPassword = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("xmpp_password", null);
        if (mConnection == null) {
            mConnection = new RosterConnection(this, jid, mPassword);
        }
        try {
            mConnection.connect();

        } catch (IOException | SmackException | XMPPException e) {
            Log.d(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again");
            e.printStackTrace();
            //Stop the service all together.
            stopSelf();
        }
    }


    public void start() {
        Log.d(TAG, " Service Start() function called.");
        if (!mActive) {
            mActive = true;
            if (mThread == null || !mThread.isAlive()) {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        //THE CODE HERE RUNS IN A BACKGROUND THREAD.
                        mTHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateRoster();
                            }
                        }, 2000);
                        Looper.loop();

                    }
                });
                mThread.start();
            }
        }
    }

    public void updateContacts() {

        if (mConnection == null || mConnection.getRoster() == null) return;
        Roster roster = mConnection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries(); //
        Log.e(TAG, entries.toString());
        List<Contact> mContacts = new ArrayList<>(); //
        for (RosterEntry entry : entries) {
            //HashMap<String, String> map = new HashMap<String,String>();
            Presence entryPresence = roster.getPresence(entry.getJid());
            Presence.Type type = entryPresence.getType();
            Contact contact = new Contact(entry.getName(), type.toString(), entry.getJid().toString());
            Log.e(TAG, contact.toString());

            mContacts.add(contact);
        }
        ContactRepository.getInstance().setContacts(mContacts);

        //Bundle up the intent and send the broadcast.
        Intent intent = new Intent(RosterConnectionService.CONTACTS_UPDATED);
        intent.setPackage(this.getApplicationContext().getPackageName());
        sendBroadcast(intent);

        return;
    }

    public void updateRoster() {
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

    public void stop() {
        Log.d(TAG, "stop()");
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mConnection != null) {
                    mConnection.disconnect();
                }
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        start();
        return Service.START_STICKY;
        //RETURNING START_STICKY CAUSES OUR CODE TO STICK AROUND WHEN THE APP ACTIVITY HAS DIED.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        stop();
    }
}

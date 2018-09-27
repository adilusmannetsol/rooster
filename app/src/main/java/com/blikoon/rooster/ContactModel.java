package com.blikoon.rooster;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 4/16/2016.
 */
public class ContactModel {

    private static ContactModel sContactModel;
    private List<Contact> mContacts;

    public static ContactModel getInstance()
    {
        if(sContactModel == null)
        {
            sContactModel = new ContactModel();
        }
        return  sContactModel;
    }

    private ContactModel()
    {
        mContacts = new ArrayList<>();
       // populateWithInitialContacts(context);

    }

    private void populateWithInitialContacts(Context context)
    {
        //Create the Foods and add them to the list;


//        Contact contact1 = new Contact("testuser2@mobi020.netsolpk.com");
//        mContacts.add(contact1);

    }

    public void setContacts(List<Contact> mContacts) {
        this.mContacts.clear();
        this.mContacts.addAll(mContacts);
    }

    public List<Contact> getContacts()
    {
        return mContacts;
    }

}

package com.sinch.messagingtutorialskeleton;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.messagingtutorialskeleton.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.Sinch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andy on 14/03/2015.
 */
public class ListUsersActivity extends Activity {

    private String currentUserId;
    private ArrayAdapter<String> namesArrayAdapter;
    private ArrayList<String> names;
    private ListView usersListView;
    private Button logoutButton;
    private ProgressDialog progressDialog;
    private BroadcastReceiver receiver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listusers);

        // Save the current Installation to Parse.
        ParseInstallation.getCurrentInstallation().saveInBackground();

        // Associate the device with a user
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground();

        showSpinner();



        logoutButton = (Button) findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), MessageService.class));
                ParseUser.logOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

        //set clickable list of users
        private void setConversationsList(){

            currentUserId = ParseUser.getCurrentUser().getObjectId();
            names = new ArrayList<String>();

            ParseQuery<ParseUser> query = ParseUser.getQuery();
            //dont include current user
            query.whereNotEqualTo("objectId", currentUserId);
            query.findInBackground(new FindCallback<ParseUser>() {
                public void done(List<ParseUser> userList, com.parse.ParseException e) {
                    if (e == null) {
                        for (int i = 0; i < userList.size(); i++) {
                            names.add(userList.get(i).getUsername().toString());
                        }
                        usersListView = (ListView) findViewById(R.id.usersListView);


                        namesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.user_list_item, names);

                        usersListView.setAdapter(namesArrayAdapter);
                        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                                openConversation(names, i);
                            }
                        });
                    } else {
                        Toast.makeText(getApplicationContext(), "Error loading user list", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    //open conversation with one person
    public void openConversation(ArrayList<String> names, int pos) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", names.get(pos));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Error finding that user", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void showSpinner(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        //create broadcast reciever to listen for the broadcast from MessageService
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Boolean success = intent.getBooleanExtra("Success", false);
                progressDialog.dismiss();

                //show message if the sinch service failed to start
                if (!success){
                    Toast.makeText(getApplicationContext(), "Messaging service failed to start", Toast.LENGTH_LONG).show();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("com.sinch.messagingtutorialskeleton.ListUsersActivity"));
    }

    @Override
    public void onResume() {
        setConversationsList();
        super.onResume();
    }
}
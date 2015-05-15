package com.sinch.messagingtutorialskeleton;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.messagingtutorialskeleton.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;

/**
 * This activity is called when the user clicks the phone icon in the MessagingActivity
 * This activity provides the UI for the calling screen and methods to start and receive a call
 * This activity uses the Sinch CallClient for handling incoming calls and the Call Listener for handling active calls
 */
public class CallActivity extends Activity {
    private String callerId;
    private String recipientId;
    private String callerUsername;
    private Call call;
    private TextView callState;
    private Button callButton;
    private Button declineButton;
    private Button answerButton;
    private MessageService.MessageServiceInterface messageService;
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private CallClientListener callClientListener = new MyCallClientListener();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        bindService(new Intent(this, MessageService.class), serviceConnection, BIND_AUTO_CREATE);

        //Get intent from previous messaging activity
        Intent callIntent = getIntent();
        callerId = callIntent.getStringExtra("callerId");
        recipientId = callIntent.getStringExtra("recipientId");

        //declare buttons for accepting, declining and starting a call and TextView for display call status
        callButton = (Button) findViewById(R.id.callbutton);
        callState = (TextView) findViewById(R.id.callState);
        declineButton = (Button) findViewById(R.id.declinebutton);
        answerButton = (Button) findViewById(R.id.answerbutton);

        //set OnClick listener to either start a call or hangup an ongoing call
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //If there is no ongoing call
                if (call == null){
                    //start a call with the recipient of the previous messaging activity
                    call = messageService.startCall(recipientId);
                    //Assign a call listener to the call
                    call.addCallListener(new SinchCallListener());
                    //change call button text to hangup
                    callButton.setText("Hang Up");

                }else {
                    //if there is an ongoing call then hangup and set text back to call
                    call.hangup();
                    call = null;
                    callButton.setText("Call");
                }
            }
        });
    }


    private class MyServiceConnection implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder){
            messageService = (MessageService.MessageServiceInterface) iBinder;
            //if the Sinch Service is connected the add a call client listener to listen for incoming calls
            messageService.addCallClientListener(callClientListener);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName){
            messageService = null;
        }
    }

    private class SinchCallListener implements CallListener{
        //This method handles what happens when an ongoing call is ended
        public void onCallEnded(Call endedCall) {
            //set call to null
            call = null;
            //set callbutton text to call
            callButton.setText("Call");
            //set call state to null
            callState.setText("");
            //set volume control back to default (Ringer volume)
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        }

        //This method handles what happens what a call is connected
        @Override
        public void onCallEstablished(Call establishedCall) {

            // set callstate text to connected
            callState.setText("Connected");

            //set answer button to invisible
            View answerButtonView = findViewById(R.id.answerbutton);
            answerButtonView.setVisibility(View.INVISIBLE);

            //set decline button to invisible
            View declineButtonView = findViewById(R.id.declinebutton);
            declineButtonView.setVisibility(View.INVISIBLE);

            //set call button to visible
            View callButtonView = findViewById(R.id.callbutton);
            callButtonView.setVisibility(View.VISIBLE);

            //set callbutton text to hangup
            callButton.setText("Hang Up");

            //set volume control to control the volume of the voice call
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        // This method handles what happens when a call is progressing i.e Ringing
        @Override
        public void onCallProgressing(Call progressingCall) {
          callState.setText("ringing");

        }

        //This method determines when it is suitable to send a push notification
        //This method was left blank as it was decided that a push notification
        //should be sent on every incoming call, see the onIncomingCall method
        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {}
    }


    //This class implements a ClientCallListener which handles incoming calls
    //This method handles displaying the decline and accept buttons with this onClickListeners
    //and sending push notification
    public class MyCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {

            //set call variable as incomingCall from method
            call = incomingCall;

            //set callstate text to incoming call
            callState.setText("Incoming Call");

            //set answer button to visible
            View answerButtonView = findViewById(R.id.answerbutton);
            answerButtonView.setVisibility(View.VISIBLE);

            //set decline button to visible
            View declineButtonView = findViewById(R.id.declinebutton);
            declineButtonView.setVisibility(View.VISIBLE);

            //set callbutton to invisible
            View callButtonView = findViewById(R.id.callbutton);
            callButtonView.setVisibility(View.INVISIBLE);

            //Set declinebutton onClickListener to hangup call, set decline and accept buttons to invisible and callbutton to visible
            declineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    call.hangup();

                    View answerButtonView = findViewById(R.id.answerbutton);
                    answerButtonView.setVisibility(View.INVISIBLE);

                    View declineButtonView = findViewById(R.id.declinebutton);
                    declineButtonView.setVisibility(View.INVISIBLE);

                    View callButtonView = findViewById(R.id.callbutton);
                    callButtonView.setVisibility(View.VISIBLE);

                    callState.setText("");

                }
            });

            //Set acceptbutton onClickListener to answer call and attach a new call listener
            answerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    call.answer();
                    call.addCallListener(new SinchCallListener());
                }
            });

            //Parse.com Push notification that includes the username of the person calling
            ParseQuery<ParseUser> recipQuery = ParseUser.getQuery();
            recipQuery.whereEqualTo("objectId", recipientId);
            recipQuery.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> reciplist, ParseException e) {
                    if (e == null) {
                        callerUsername = reciplist.get(0).get("username").toString();

                        ParseQuery userQuery = ParseUser.getQuery();
                        userQuery.whereEqualTo("objectId", callerId);

                        ParseQuery pushQuery = ParseInstallation.getQuery();
                        pushQuery.whereMatchesQuery("user", userQuery);

                        //send push notification to query
                        ParsePush push = new ParsePush();
                        //set installation query
                        push.setQuery(pushQuery);
                        push.setMessage("Incoming phone call from " + callerUsername);
                        push.sendInBackground();

                    }
                }
            });




        }

    }

}
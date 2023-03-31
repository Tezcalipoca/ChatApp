package com.example.chatapp.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.Firebase.FcmNotificationsSender;
import com.example.chatapp.Models.Users;
import com.example.chatapp.Models.VideoCallModel;
import com.example.chatapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class VideoCallOutgoingActivity extends AppCompatActivity {
    CircleImageView cirAvatarVideoCalOutGoing;
    TextView tvNameVideoCallOutGoing, tvEmailVideoCallOutGoing;
    FloatingActionButton fabCallEnd;
    String receiveID, receive_token, senderID;

    DatabaseReference mUserReference, mVideoCallReference;
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    FirebaseUser mUser;
    FirebaseAuth mAuth;

    VideoCallModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_outgoing);
        setControl();
        setEvent();
    }

    private void setControl() {
        cirAvatarVideoCalOutGoing = findViewById(R.id.cirAvatarVideoCalOutGoing);
        tvNameVideoCallOutGoing = findViewById(R.id.tvNameVideoCallOutGoing);
        tvEmailVideoCallOutGoing = findViewById(R.id.tvEmailVideoCallOutGoing);
        fabCallEnd = findViewById(R.id.fabCallEnd);

        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mVideoCallReference = FirebaseDatabase.getInstance().getReference().child("VideoCallComing");
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        receiveID = getIntent().getStringExtra("friendID");
        senderID = mUser.getUid();
        model = new VideoCallModel();
    }

    private void setEvent() {
        loadProfileFriend();
        sendVideoCallInvitation();
        checkResponse();

        fabCallEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelVideoCall();
            }
        });
    }

    private void cancelVideoCall() {
        HashMap hashMap = new HashMap();
        hashMap.put("key",receiveID);
        hashMap.put("response","no");
        mVideoCallReference.child(senderID).child(receiveID).child("response").updateChildren(hashMap);
        Toast.makeText(this, "Kết thúc cuộc gọi", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(VideoCallOutgoingActivity.this, ChatActivity.class);
        intent.putExtra("userID",receiveID);
        startActivity(intent);
        finish();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoCallReference.child(senderID).child(receiveID).removeValue();
            }
        },1000);
    }


    private void loadProfileFriend() {
        mUserReference.child(receiveID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Users users = snapshot.getValue(Users.class);
                    Picasso.get().load(users.getProfilePic()).placeholder(R.drawable.default_avatar).into(cirAvatarVideoCalOutGoing);
                    tvNameVideoCallOutGoing.setText(users.getUserName());
                    tvEmailVideoCallOutGoing.setText(users.getEmail());
                } else {
                    Toast.makeText(VideoCallOutgoingActivity.this, "Không tìm thấy dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendVideoCallInvitation() {
        mUserReference.child(receiveID).child("fcmToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    receive_token = snapshot.getValue().toString();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        HashMap hashMap = new HashMap<>();
        hashMap.put("key", senderID + receiveID);
        hashMap.put("response","wait_confirm");
        mVideoCallReference.child(senderID).child(receiveID).child("response").updateChildren(hashMap);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender(receive_token,"v",senderID,getApplicationContext(),VideoCallOutgoingActivity.this);
                fcmNotificationsSender.sendNotifications();
            }
        },1000);
    }

    private void checkResponse() {
        mVideoCallReference.child(senderID).child(receiveID).child("response").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String key = snapshot.child("key").getValue().toString().trim();
                    String response = snapshot.child("response").getValue().toString().trim();

                    if (response.equals("yes")) {
                        joinMeeting(key);
                    } else if (response.equals("no")) {;
                        Intent intent = new Intent(VideoCallOutgoingActivity.this, ChatActivity.class);
                        intent.putExtra("userID",receiveID);
                        startActivity(intent);
                        finish();
                    } else {
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void joinMeeting(String key) {
        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(key)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .build();
            JitsiMeetActivity.launch(VideoCallOutgoingActivity.this, options);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }


}
package com.example.a4;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import android.view.View;
import android.widget.PopupMenu;

class User{
    private String login;
    private int color;

    private int getRandomColor(){
        Random random = new Random();
        return Color.argb(128, random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    public User(String username){
        this.login = username;
        this.color = getRandomColor();
    }

    public int getColor(){
        return color;
    }
}

public class Chat extends AppCompatActivity {
    private Vector<TextView> chatItems = new Vector();
    private Map<String, User> user = new HashMap<String, User>();
    private Boolean exit = false;
    FloatingActionButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getMessageFromDb();
        final TextView nick = findViewById(R.id.nick);
        titleBar(nick);
        ///->Widok
        final Button sendMessages = findViewById(R.id.sendMessage);
        sendMessages.setOnClickListener(v -> {
            sendMessage();
            titleBar(nick);
        });
        button = findViewById(R.id.actionButton);
        button.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(Chat.this, button);
            popup.getMenuInflater().inflate(R.menu.poupup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch(item.toString()){
                    case "Użytkownicy Online": users_online(); break;
                    case "Tryb ukrycia": spy(); break;
                    case "Wyczyść czat": clearChatContent(10); break;
                    case "Wyloguj się": logout(); break;
                    case "Wyjdź": exitAPP(); break;
                    default: Toast.makeText(Chat.this, "Opcja: "+item.getTitle()+", narazie nie jest wspierana", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popup.show();
        });
    }

    public void sendMessage() {
        EditText textMessage = findViewById(R.id.textMessage);
        String msg = textMessage.getText().toString();
        String[] split = msg.split(" ");
        if(msg.length() > 0) {
            switch(split[0]){
                case ".clear": clearChatContent(0); break;
                case ".logout": logout(); break;
                case ".spy": spy(); break;
                case ".users": users_online(); break;
                case ".delete":
                    if(split.length > 1 && split[1].matches("[0-9]+")) {
                        delete_messages(Integer.parseInt(split[1]));
                    }
                    else sendMessageToDb(getUsername(), msg);
                 break;
                case ".exit": exitAPP(); break;
                default: {
                    if (msg.matches("(.clear )\\d+"))
                        clearChatContent(Integer.valueOf(msg.substring(msg.lastIndexOf(" ") + 1)));
                    else
                        sendMessageToDb(getUsername(), msg);
                }
            }
            textMessage.setText("");
        }
    }

    public void customView(String login, View v, int borderColor){
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadii(new float[] { 40, 40, 40, 40, 40, 40, 40, 40 });
        shape.setColor(user.get(login).getColor());
        shape.setStroke(3, borderColor);
        v.setBackground(shape);
    }

    public void createViewMessage(String login, String msg){
        LinearLayout content = findViewById(R.id.content);
        TextView messages = new TextView(this);
        //messages.setBackgroundColor(getRandomColor());
        LinearLayout.LayoutParams params;
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(6,4,6,0);
        if(login.equals(getUsername())){ params.gravity = Gravity.RIGHT; }
        messages.setLayoutParams(params);
        //messages.setGravity(Gravity.RIGHT);
        messages.setPadding(20,10,20,10);
        customView(login, messages, 111);
        chatItems.add(messages);
        messages.setText(chatItems.toString());
        messages.setText(Html.fromHtml(msg));
        content.addView(messages);
        Log.d("widok", messages.toString());
        ScrollView scroll = findViewById(R.id.contentScroll);
        scroll.fullScroll(View.FOCUS_DOWN);
    }

    public String getUsername() {
        SharedPreferences sharedPref = getSharedPreferences("dane", Context.MODE_PRIVATE);
        return sharedPref.getString("login", "");
    }

    public void sendMessageToDb(String log, String messToDb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("login", log);
        data.put("text", messToDb);
        data.put("time", Timestamp.now());
        String time = timestampToSeconds(Timestamp.now().toString());

        db.collection("messages").document(time)
            .set(data)
            .addOnSuccessListener(aVoid -> Log.d("Dodanie", "Wysłano wiadomość do bazy!"))
            .addOnFailureListener(e -> {
                Log.w("Dodanie", "Błąd wysyłania", e);
                Toast.makeText(getApplicationContext(), "Błąd", Toast.LENGTH_LONG).show();
            });
    }

    public void getMessageFromDb(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("messages")
            .addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w("TAG", "listen:error", e);
                return;
            }

            assert snapshots != null;
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED:
                        Log.d("TAG", "New Msg: " + dc.getDocument().toObject(Message.class));
                        String txt = Objects.requireNonNull(dc.getDocument().getData().get("text")).toString();
                        String login = Objects.requireNonNull(dc.getDocument().getData().get("login")).toString();
                        String time = Objects.requireNonNull(dc.getDocument().getData().get("time")).toString();
                        time = filteringTimestamp(time);
                        if(!user.containsKey(login)){
                            user.put(login, new User(login));
                        }
                        String toSend;
                        if(login.equals(getUsername()))
                        	toSend = txt;
												else toSend = "<b>&lt;"+login+"&gt;</b>: "+txt+" <i>||"+time+"<i>";
                        createViewMessage(login, toSend);
                        break;
                    case MODIFIED:
                        Log.d("TAG", "Modified Msg: " + dc.getDocument().toObject(Message.class));
                        break;
                    case REMOVED:
                        Log.d("TAG", "Removed Msg: " + dc.getDocument().toObject(Message.class));
                        break;
                }
            }
        });
    }

    public void clearChatContent(int last){
        AtomicInteger i= new AtomicInteger();
        LinearLayout content = findViewById(R.id.content);
        chatItems.forEach((n) -> {
            if(i.incrementAndGet() <= (chatItems.size()-last))
                content.removeView(n);
        });
    }

    public String filteringTimestamp(String timestamp){
        String time = timestampToSeconds(timestamp);

        Integer temp_time = Integer.valueOf(time) - (Integer.valueOf(time)/3600/24)*24*3600;
        Integer houer = temp_time/3600; //14
        Integer minuts = (temp_time - (houer*3600))/60;
        Integer seconds = temp_time - (houer*3600) - (minuts*60);
        houer++;
        return round_time(houer)+":"+round_time(minuts)+":"+round_time(seconds);
    }

    public String timestampToSeconds(String timestamp){
        String [] time = timestamp.split(",");
        time = time[0].split("=");
        return time[1];
    }

    public String round_time(Integer number) {
        if(number<10)   return "0"+number.toString();
        else            return number.toString();
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void titleBar(TextView nick) {
        String stan = isConnected() ? "</b><i> Połączono</i>" : " </b><i> Rozłączono</i>";
        String msg = "Twój nick to: <b>" + getUsername() + stan;
        nick.setText(Html.fromHtml(msg));
    }

    public void toMainActivity() {
        Intent intent;
        intent = new Intent(Chat.this, MainActivity.class);
        startActivity(intent);
    }

    public void logout() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("is_online", false);
        data.put("last_time_logout", Timestamp.now());
        db.collection("users").document(getUsername())
            .update(data)
            .addOnSuccessListener(aVoid -> {
                Log.d("Wylogowanie", getUsername()+"Wylogowal się");
                Toast.makeText(getApplicationContext(), "Wylogowałeś się ! Zapraszamy ponownie!", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                Log.w("Wylogowanie", "Błąd", e);
                Toast.makeText(getApplicationContext(), "Błąd", Toast.LENGTH_LONG).show();
            });
        toMainActivity();
        sendMessageToDb(getUsername(), "Wylogował się");
    }

    public void delete_message(String id, FirebaseFirestore db){
        db.collection("messages").document(id)
            .delete()
            .addOnSuccessListener (aVoid -> Log.d("Kasowanie", "DocumentSnapshot successfully deleted!"))
            .addOnFailureListener (e ->  Log.w("Kasowanie", "Error deleting document", e));
    }

    public void delete_messages(Integer quantity) {
        LinearLayout content = findViewById(R.id.content);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("messages").orderBy("time", Query.Direction.ASCENDING).limit(quantity)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int i = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d("Document_id", document.getId() + " => " + document.getData());
                        delete_message(document.getId(), db);
                        content.removeView(chatItems.elementAt(i++));
                    }
                } else {
                    Log.e("ERROR", "Error getting documents: ", task.getException());
                }
            });
    }

    public void users_online() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").whereEqualTo("spy", false)
            .get()
            .addOnCompleteListener(task -> {
                String users = new String();
                int i = 0;
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d("Document_id", document.getId() + " => " + document.getData());
                        users += " <i>" + document.getId() +"<i>";
                        i++;
                    }
                } else {
                    Log.e("ERROR", "Error getting documents: ", task.getException());
                }
                users += " Zalogowanych: "+i;
                sendMessageToDb("Users_Online:", users);
            });
    }

    public void changeSpy(boolean status) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("spy", status);
        db.collection("users").document(getUsername())
            .update(data)
            .addOnSuccessListener(aVoid -> Log.d("SPY", "Wysłano wiadomość do bazy!"))
            .addOnFailureListener(e -> Log.w("SPY", "Błąd wysyłania", e));
            Toast.makeText(getApplicationContext(), "Jesteś Agentem :P", Toast.LENGTH_LONG).show();
    }

    public void spy() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(getUsername())
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    boolean status = document.getBoolean("spy");
                    changeSpy(!status);
                    Toast.makeText(getApplicationContext(), "Tryb śledzenia: "+!status, Toast.LENGTH_LONG).show();
                    Log.d("SPY", "Setting spy: "+ status);
                } else {
                    Log.e("ERROR", "Error getting documents: ", task.getException());
                }
            });
    }
    public void exitAPP(){
        if (exit) {
            sendMessageToDb(getUsername(), "Opuścił czat");
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Kliknij ponownie by wyjść.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(() -> exit = false, 3 * 1000);
        }
    }

    @Override
    public void onBackPressed() {
        exitAPP();
    }
}
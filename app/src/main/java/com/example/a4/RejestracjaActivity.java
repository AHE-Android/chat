package com.example.a4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RejestracjaActivity extends AppCompatActivity {

    EditText login, haslo, potwierdz;
    Button zaloguj;
    CheckBox check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rejestracja);

        login = findViewById(R.id.Login);
        haslo = findViewById(R.id.Password);
        zaloguj = findViewById(R.id.button2);
        check = findViewById(R.id.checkBox2);
        potwierdz = findViewById(R.id.Confirm);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(RejestracjaActivity.this, RegulaminActivity.class);
                startActivity(intent);
            }
        });

        zaloguj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                final String log = login.getText().toString();
                final String password = haslo.getText().toString();
                final String confirm = potwierdz.getText().toString();

                final FirebaseFirestore db = FirebaseFirestore.getInstance();

                DocumentReference docRef = db.collection("users").document(log);
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(getApplicationContext(), "Istnieje taki użytkownik", Toast.LENGTH_LONG).show();
                            } else {
                                if (confirm.equals(password) && check.isChecked()) {
                                    toRegistration(log, password, db);
                                } else if(!password.equals(confirm)){
                                    Toast.makeText(getApplicationContext(), "Hasła nie są takie same", Toast.LENGTH_LONG).show();
                                } else if(!check.isChecked()){
                                    Toast.makeText(getApplicationContext(), "Proszę potwierdzić regulamin", Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            //Log.w("Błąd", "get failed with ", task.getException());
                            Toast.makeText(getApplicationContext(), "Błąd połączenia", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    public void toRegistration(String log, String password, FirebaseFirestore db) {
        final Map<String, Object> user = new HashMap<>();
        //user.put("login", log);
        user.put("password", password);
        user.put("is_online", true);

        db.collection("users").document(log)
            .set(user)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Dodanie", "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("Dodanie", "Error writing document", e);
                }
            });

        Intent intent;
        intent = new Intent(RejestracjaActivity.this, Chat.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref = getSharedPreferences("dane", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("login", login.getText().toString());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = getSharedPreferences("dane", Context.MODE_PRIVATE);
        String txt = sharedPref.getString("login", "");
        login.setText(txt);
    }
}

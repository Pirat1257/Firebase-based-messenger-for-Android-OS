package com.example.bos_3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.EventListener;

public class MainActivity extends AppCompatActivity {
    // Подготовка к работе с бозой данных
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference messages = database.getReference("messages");
    DatabaseReference public_key = database.getReference("publicKey");
    DatabaseReference session_key = database.getReference("sessionKey");
    DatabaseReference connected = database.getReference("connected");

    // Генерирует пару ключей == 0
    // Генерирует сеансовый ключ == 1
    // Установлен сеансовый ключ == 2
    int what_the_hell_are_you = 0;
    // Создаем класс для работы с криптографией
    private Crypto crypto = new Crypto();
    // Сеансовый ключ
    private String sessionKey = null;
    // Привязываем элементы
    Button button = null;
    Button close_button = null;
    EditText message_field = null;
    TextView status_field = null;
    ListView table = null;
    private ArrayList<String> array_list; // Для вывода названий
    private ArrayAdapter adapter; // Для строк

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Привязываем элементы
        button = findViewById(R.id.save_button);
        close_button = findViewById(R.id.close_button);
        message_field = findViewById(R.id.message_input);
        status_field = findViewById(R.id.status);
        table = findViewById(R.id.table);
        array_list = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array_list);
        table.setAdapter(adapter);

        // Прослушивание событий messages
        messages.addChildEventListener(new ChildEventListener() {
            // Данное событие срабатывает для всех созданных сообщений и на каждое новое добавленное
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Мы ДОЛЖНЫ взыимодействовать с сообщениями только в случае установки сессионного ключа
                if (what_the_hell_are_you == 2) {
                    if (snapshot != null) {
                        String msg = snapshot.getValue(String.class);
                        try {
                            array_list.add(crypto.decrypt(sessionKey, Base64.decode(msg.getBytes("UTF-16LE"), Base64.DEFAULT)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // Обновляем список
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                finish();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Просматриваем события connected
        connected.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (what_the_hell_are_you == 2) {
                    if (snapshot.getValue() == null) {
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Просматриваем состояние ячейки с открытым ключем
        public_key.addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                // Если вернуло null, значит мы должны добавить открытый ключ в базу данных
                if (snapshot.getValue() == null && what_the_hell_are_you == 0) {
                    what_the_hell_are_you = 1;
                    // Уведомляем о том что тут происходит что-то
                    connected.setValue("true");
                    // Производим генерацию пары открытый-закрытый ключ и заливаем открытый в базу
                    public_key.setValue(crypto.generate_keys());
                    // Удаляем все зашифрованные сообщения в БД
                    messages.removeValue();
                }
                // В противном случае там лежит открытый ключ, подбираем его и генерируем сеансовый
                else if (snapshot.getValue() != null && what_the_hell_are_you == 0) {
                    // Получили открытый ключ
                    String public_key_str = snapshot.getValue(String.class);
                    // Удаляем открытый ключ
                    public_key.removeValue();
                    // Генерируем сеансовый
                    sessionKey = crypto.generate_key();
                    // Шифруем сенсовый ключ открытым ключем
                    String encrypted_sessionKey = null;
                    try {
                        encrypted_sessionKey = crypto.encryptByPublicKey(public_key_str, sessionKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Отправка зашифрованного сессионного ключа
                    session_key.setValue(encrypted_sessionKey);
                    // Ставим себе состояние с установленным сеансовым ключем
                    what_the_hell_are_you = 2;
                    status_field.setText("CONNECTED");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

        // Просматриваем состояние ячейки с сеансовым ключем
        session_key.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Если там есть какое-то значение и мы генерировани ключ-пару, то получаем
                // значение зашифрованного сеансового ключа
                if (snapshot.getValue() != null && what_the_hell_are_you == 1) {
                    // Получили зашифрованный сессионный ключ
                    String secret_key = snapshot.getValue(String.class);
                    // Удаляем зашифрованный сессионный ключ
                    session_key.removeValue();
                    // Расшифровка сессионного ключа
                    try {
                        sessionKey = crypto.decryptByPrivateKey(secret_key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Ставим себе состояние с установленным сеансовым ключем
                    what_the_hell_are_you = 2;
                    status_field.setText("CONNECTED");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Обработка нажатия на кнопку SEND
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Сеансовый ключ установлен
                if (what_the_hell_are_you == 2) {
                    // Считываем сообщение
                    String msg_text = message_field.getText().toString();
                    // Проверяем что там вообще есть сообщение
                    if (msg_text.length() != 0) {
                        // Обнуляем сообщение на экране
                        message_field.setText("");
                        // Шифруем сообщение
                        try {
                            msg_text = crypto.encrypt(sessionKey.getBytes("UTF-16LE"),
                                    msg_text.getBytes("UTF-16LE"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // Отправляем сообщение
                        messages.push().setValue(msg_text);
                    } else {
                        Toast.makeText(MainActivity.this, "Enter some message!",
                                Toast.LENGTH_LONG).show();
                    }
                }
                // Сеансовый ключ еще не установлен
                else {
                    Toast.makeText(MainActivity.this,
                            "Your buddy is not connected yet!", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Обработка нажатия на кнопку CLOSE
        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                public_key.removeValue();
                session_key.removeValue();
                messages.removeValue();
                connected.removeValue();
                finish();
            }
        });
    }
}
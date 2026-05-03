package com.example.walnex;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact_us);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contactRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView back = findViewById(R.id.textContactBack);
        back.setOnClickListener(v -> finish());

        TextView phone = findViewById(R.id.textContactPhoneValue);
        phone.setOnClickListener(v -> {
            String value = getString(R.string.contact_phone);
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + value));
            startActivity(intent);
        });

        TextView email = findViewById(R.id.textContactEmailValue);
        email.setOnClickListener(v -> {
            String value = getString(R.string.contact_email);
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + value));
            startActivity(Intent.createChooser(intent, getString(R.string.contact_email_action)));
        });
    }
}
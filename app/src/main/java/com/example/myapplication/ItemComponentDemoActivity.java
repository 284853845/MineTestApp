package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.security.CaptureAwareActivity;
import com.example.myapplication.widget.ConfigurableItemView;

/** Displays the supported states of {@link ConfigurableItemView}. */
public class ItemComponentDemoActivity extends CaptureAwareActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_component_demo);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ConfigurableItemView readOnlyInput = findViewById(R.id.item_read_only_input);
        readOnlyInput.setInputText(getString(R.string.item_demo_read_only_value));

        ConfigurableItemView clickableItem = findViewById(R.id.item_clickable);
        clickableItem.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ItemComponentDemoActivity.this,
                        R.string.item_demo_click_feedback, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

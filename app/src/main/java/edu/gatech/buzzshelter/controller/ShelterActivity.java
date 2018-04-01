package edu.gatech.buzzshelter.controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Optional;

import edu.gatech.buzzshelter.R;
import edu.gatech.buzzshelter.model.data.Shelter;
import edu.gatech.buzzshelter.model.facade.DataFacade;

public class ShelterActivity extends AppCompatActivity {

    public static final String ARG_SHELTER_ID = "shelter_id";

    private Shelter getShelter()
    {
        DataFacade data = DataFacade.getInstance();
        int id = getIntent().getIntExtra(ARG_SHELTER_ID, -1);

        /* Fatal error (shouldn't happen) */
        if (id == -1)
            return null;

        Optional<Shelter> result = data.getShelters()
                .stream().filter(x -> x.getKey() == id).findFirst();

        /* Fatal error (shouldn't happen) */
        return result.orElse(null);
    }

    private void setCapacity(Shelter s, TextView capacity)
    {
        List<Shelter.Capacity> all = s.getCapacity();
        StringBuilder line = new StringBuilder();

        for(int i = 0; i < all.size(); i++)
        {
            Shelter.Capacity item = all.get(i);

            if(item.getCapacity() != -1)
            {
                String cap = Integer.toString(item.getCapacity());

                line.append(cap);
                line.append(" ");
                line.append(item.getCategory());

                /* Add comma, if there are more entries */
                if ((i + 1) != all.size())
                    line.append(", ");
            }
            else
            {
                line.append("Unknown (?)");
            }
        }

        capacity.setText(line.toString());
    }

    private void setAvailable(Shelter s, TextView available)
    {
        List<Shelter.Capacity> all = s.getCapacity();
        StringBuilder line = new StringBuilder();

        for(int i = 0; i < all.size(); i++)
        {
            Shelter.Capacity item = all.get(i);

            if(item.getAvailable() != -1)
            {
                String cap = Integer.toString(item.getAvailable());

                line.append(cap);
                line.append(" ");
                line.append(item.getCategory());

                /* Add comma, if there are more entries */
                if ((i + 1) != all.size())
                    line.append(", ");
            }
            else
            {
                line.append("Unknown (?)");
            }
        }

        available.setText(line.toString());
    }

    private void setReserve(Shelter s, Button reserve)
    {
        DataFacade facade = DataFacade.getInstance();

        if(!facade.canReserve())
        {
            reserve.setError("Already have reservation");
            return;
        }

        List<Shelter.Capacity> all = s.getCapacity();
        boolean canReserve = false;

        for(int i = 0; i < all.size(); i++)
        {
            Shelter.Capacity item = all.get(i);

            if(item.getAvailable() > 0)
            {
                canReserve = true;
                break;
            }
        }

        /* Show (!) on button */
        if(!canReserve)
            reserve.setError("No space");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter);
        Toolbar toolbar = findViewById(R.id.toolbar);

        TextView restrictions = findViewById(R.id.restrictions);
        TextView capacity = findViewById(R.id.capacity);
        TextView available = findViewById(R.id.available);
        TextView address = findViewById(R.id.address);
        TextView notes = findViewById(R.id.notes);

        Button phone = findViewById(R.id.phone);
        Button reserve = findViewById(R.id.reserve);
        Button back = findViewById(R.id.backButton);

        Shelter s = getShelter();

        /* Shouldn't happen (safety) */
        if(s == null)
            return;

        /* Set the values appropriately */
        toolbar.setTitle(s.getName());
        restrictions.setText(s.getRestrict());

        setCapacity(s, capacity);
        setAvailable(s, available);
        setReserve(s, reserve);

        address.setText(s.getAddress());
        notes.setText(s.getNotes());
        phone.setText(String.format("  %s", s.getPhone()));

        phone.setOnClickListener(v ->
        {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phone.getText()));

            /* Check permissions */
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, 1);
            }

            startActivity(callIntent);
        });

        reserve.setOnClickListener(v -> {
            DataFacade facade = DataFacade.getInstance();

            if(!facade.canReserve())
            {
                View view = findViewById(android.R.id.content);

                Snackbar.make(view, "Only one reservation is allowed at a time.",
                        Snackbar.LENGTH_LONG)
                        .show();

                return;
            }

            List<Shelter.Capacity> total = s.getCapacity();
            boolean allow = false;

            /* Make sure there is space */
            for(Shelter.Capacity item : total)
            {
                if(item.getAvailable() > 0)
                {
                    allow = true;
                    break;
                }
            }

            if(!allow)
            {
                Snackbar.make(v, "Can't reserve. No space left", Snackbar.LENGTH_LONG)
                        .show();

                return;
            }


            /* Launch the reserve activity */
            Intent intent = new Intent(this, ReserveActivity.class);

            intent.putExtra(ReserveActivity.SHELTER_NAME, s.getName());
            intent.putExtra(ReserveActivity.SHELTER_ID, s.getKey());

            startActivity(intent);
        });

        back.setOnClickListener(v -> {
            Intent goBack = new Intent(this, ListActivity.class);
            startActivity(goBack);
        });

        /* Set toolbar (after title is set) */
        setSupportActionBar(toolbar);
    }

}

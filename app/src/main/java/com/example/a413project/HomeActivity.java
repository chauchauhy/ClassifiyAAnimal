package com.example.a413project;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.a413project.Adapter.HomePageAdapter;
import com.example.a413project.Classifies.Classifier;
import com.example.a413project.Database.model.Classification;
import com.example.a413project.Database.model.DataList;
import com.example.a413project.Database.model.Label;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    Context context;
    final int TAKE_CARMA_CODE = 0;
    final int TAKE_PICTURE = 1;
    final int PERMISSION_ID = 2;
    DataList dataList;
    public static HomePageAdapter adapter;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    public static final String DBRECORD = "RECORDDB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initUI();
    }


    private void callClassification(Uri image_uri) {
        try {
            Classifier classifier = new Classifier(getAssets(), 224);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);
            List<Label> result = classifier.recognizeImage(bitmap);
            // just only get the a high confidence result of the label
            Classification c = new Classification(result.get(0).getLabel(), result.get(0).confidence, "0", String.valueOf(image_uri));
            Intent i = new Intent(context, ResultActivity.class);
            i.putExtra(ResultActivity.CLASSIFICATION_CODE, c);
            startActivity(i);
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createImageSelection() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final CharSequence[] options = {"New Picture", "Select picture", "Cancel"};
        final String[] option = {"New Picture", "Select picture", "Cancel"};
        builder.setTitle("Select options ...").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (options[i].equals(option[0])) {
                    // dismiss the dialog and open gallery for selecting image
                    dialogInterface.dismiss();
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, TAKE_CARMA_CODE);
                } else if (options[i].equals(option[1])) {
                    // disappear the dialog and open camera
                    dialogInterface.dismiss();
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Please select a image from gallary"), TAKE_PICTURE);
                } else if (options[i].equals(option[2])) {
                    dialogInterface.dismiss();
                }
            }
        }).setCancelable(true).create().show();
    }

    public boolean permissionAvailable(String... permissionIDs) {
        // check permission is granted or not;
        boolean result = true;
        for (String id : permissionIDs) {
            if (ActivityCompat.checkSelfPermission(context, id) == PackageManager.PERMISSION_DENIED) {
                result = false;
            }
        }
        return result;
    }

    public void permissionsAction(String[] permissionIDs) {
        // shows dialog to request permission
        // the activityCompat.requestPermission require a String array of the permission id;
        ActivityCompat.requestPermissions(this, permissionIDs, PERMISSION_ID);
    }

    private void initUI() {
        // set all intent of this activity should be a fade-in - out style
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        context = this;
        dataList = new DataList(context);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        recyclerView = findViewById(R.id.recyclerViewHomePage);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new HomePageAdapter(context, dataList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                dataList.refreshList();
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if (resultCode == RESULT_OK && requestCode == TAKE_PICTURE && data != null && data.getData() != null) {
            Uri image_uri = data.getData();
            try {
                if (image_uri != null) {
                    // copy the image to self-folder (make a clone)
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);
                    uri = dataList.saveToGallery(bitmap);
                    callClassification(uri);
                } else {
                    Toast.makeText(context, "Cannot catch the image", Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_OK && requestCode == TAKE_CARMA_CODE && data != null) {
            Uri image_uri = data.getData();
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            uri = dataList.saveToGallery(bitmap);
            callClassification(uri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.addNewPic:
                if (permissionAvailable(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    createImageSelection();
                } else {
                    permissionsAction(new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE});
                }
                break;
            case R.id.about_app:
                Toast.makeText(context, R.string.tips, Toast.LENGTH_LONG).show();
                break;
            case R.id.clearAll:
                dataList.removeAll();
                DataList.list.clear();
                Toast.makeText(context, R.string.removeAll, Toast.LENGTH_LONG).show();
                adapter.notifyDataSetChanged();
            case android.R.id.home:
                startActivity(new Intent(context, SplashActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // in this case, nothing to do
            }
        }
    }
}
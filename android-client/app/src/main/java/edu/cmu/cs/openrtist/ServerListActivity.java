// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.openrtist;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ImageView;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.Manifest;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.content.Context;
import android.hardware.camera2.CameraManager;

import java.util.ArrayList;
import java.util.Map;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.client.socket.SocketWrapper;


public class ServerListActivity extends AppCompatActivity  {
    ListView listView;
    EditText serverName;
    EditText serverAddress;
    ImageView add;
    ArrayList<Server> ItemModelList;
    ServerListAdapter serverListAdapter;
    CameraManager camMan = null;
    private SharedPreferences mSharedPreferences;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 23;

    void loadPref(Context c, String key, Object value) {
        Const.loadPref(c, key, value);
    }

    ServerListAdapter createServerListAdapter() {
        return new ServerListAdapter(this, ItemModelList);
    }

    //activity menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(this.getString(R.string.about_message, BuildConfig.VERSION_NAME))
                        .setTitle(R.string.about_title);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //intent.putExtra("", faceTable);
                this.startActivity(intent);
                return true;
            default:
                return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
        
        setContentView(R.layout.activity_serverlist);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        listView = (ListView) findViewById(R.id.listServers);
        serverName = (EditText) findViewById(R.id.addServerName);
        serverAddress = (EditText) findViewById(R.id.addServerAddress);
        add = (ImageView) findViewById(R.id.imgViewAdd);
        ItemModelList = new ArrayList<Server>();
        serverListAdapter = createServerListAdapter();
        listView.setAdapter(serverListAdapter);
        mSharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> m = mSharedPreferences.getAll();
        for(Map.Entry<String,?> entry : m.entrySet()){
            Log.d("SharedPreferences",entry.getKey() + ": " +
                    entry.getValue().toString());
            this.loadPref(this.getApplicationContext(), entry.getKey(), entry.getValue());

        }
        camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        initServerList();
    }

    void requestPermissionHelper(String permissions[]) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            ActivityCompat.requestPermissions(this,
                    permissions,
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    void requestPermission() {
        String permissions[] = {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        this.requestPermissionHelper(permissions);
    }


    void initServerList() {
        Map<String, ?> prefs = mSharedPreferences.getAll();
        for (Map.Entry<String,?> pref : prefs.entrySet())
            if(pref.getKey().startsWith("server:")) {
                Server s = new Server(
                        pref.getKey().substring("server:".length()), pref.getValue().toString());
                ItemModelList.add(s);
                serverListAdapter.notifyDataSetChanged();
            }

        if (prefs.isEmpty()) {
            // Add demo server if there are no other servers present
            Server s = new Server(getString(R.string.demo_server), getString(R.string.demo_dns));
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("server:".concat(getString(R.string.demo_server)),getString(R.string.demo_dns));
            editor.commit();
        }

        // local execution
        Server s = new Server(getString(R.string.local_execution),
                getString(R.string.local_execution_dns_placeholder));
        ItemModelList.add(s);
        serverListAdapter.notifyDataSetChanged();
    }

    public void addValue(View v) {
        add.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        String name = serverName.getText().toString();
        String endpoint = serverAddress.getText().toString();
        if (name.isEmpty() || endpoint.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.error_empty ,
                    Toast.LENGTH_SHORT).show();
        } else if (!SocketWrapper.validUri(endpoint, Const.PORT)) {
            Toast.makeText(getApplicationContext(), R.string.error_invalidURI,
                    Toast.LENGTH_SHORT).show();
        }  else if(mSharedPreferences.contains("server:".concat(name))) {
            Toast.makeText(getApplicationContext(), R.string.error_exists,
                Toast.LENGTH_SHORT).show();
        } else {
            Server s = new Server(name, endpoint);
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
            serverName.setText("");
            serverAddress.setText("");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("server:".concat(name),endpoint);
            editor.commit();
        }
    }
}

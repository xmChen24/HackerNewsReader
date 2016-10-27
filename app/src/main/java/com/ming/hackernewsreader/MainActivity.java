package com.ming.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleKeys = new ArrayList<Integer>();

    SQLiteDatabase articleDB;

    ArrayList<String>  titles = new ArrayList<String>();
    ArrayList<String>  urls = new ArrayList<String>();
    ArrayList<String>  content = new ArrayList<String>();
    ArrayAdapter arrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listview);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), articleActivity.class);
                intent.putExtra("articleURL", urls.get(i));
                intent.putExtra("articleContent", content.get(i));
                startActivity(intent);

            }
        });

        articleDB = this.openOrCreateDatabase("Article", MODE_PRIVATE, null);

        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();


        try {

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            updateListView();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateListView(){

        try {

            Cursor c = articleDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            c.moveToFirst();

            titles.clear(); // make sure nothing there before we adding titles
            urls.clear();

            while (c != null) {

                titles.add(c.getString(titleIndex));//  get titles from DB and store them into arraylist
                urls.add(c.getString(urlIndex));  //    get urls from DB and store them into arraylist
                content.add(c.getString(contentIndex));

                c.moveToNext();
            }
        }catch (Exception e){

            e.printStackTrace();
        }


        arrayAdapter.notifyDataSetChanged();  // get title from DB then updata the Adapter
    }

    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while ( data != -1){
                    char current = (char)data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result); //article ID

                articleDB.execSQL("DELETE FROM articles");  // delete test data

                for(int i = 0; i < 20; i++){

                    String articleKey = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleKey + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleInfo = "";

                    while( data !=  -1){

                        char current = (char)data;
                        articleInfo += current;
                        data = reader.read();

                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);  // not JSONArray cuz not URL content is a array of number now

                    // Log.i("JSONObject", jsonObject.toString());

                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");

                    String articleContent = "";
                  /*
                    url = new URL(articleURL);
                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();



                    while( data !=  -1){

                        char current = (char)data;
                        articleInfo += current;
                        data = reader.read();

                    }
                    */


                    articleKeys.add(Integer.valueOf(articleKey));
                    articleURLs.put(Integer.valueOf(articleKey), articleURL);
                    articleTitles.put(Integer.valueOf(articleKey), articleTitle);

                    //  articleDB.execSQL("INSERT INTO articles ( articleId, url, title) VALUES ("+ articleKey +", "+ articleURL +", "+ articleTitle +")");
                    //  title contains ' cause issues
                    String sql = "INSERT INTO articles ( articleId, url, title, content) VALUES(?, ?, ?, ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);

                    statement.bindString(1, articleKey);
                    statement.bindString(2, articleURL);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleContent);

                    statement.execute();

                }

            }catch (Exception e){

                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            super.onPostExecute(s);
            updateListView();


        }
    }
}

package com.bishopireton.texttospeech;

import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;       // Allows the visual of the array list?
    private SwipeRefreshLayout mSwipeLayout;  // Not sure this is implement, but it will refresh URL

    private EditText etxtRSSLink;   // Allows user to enter the url of the RSS feed
    private Button btnFetchDrudge;  // Buttons on the UI that the user users
    private Button btnRead;
    private Button btnGetStory;
    private TextView txtvTitle;     // TextViews on the UI that ar efilled with data
    private TextView txtvLink;
    private TextView txtvDescription;

    private List<RssFeedModel> mFeedModelList;  // Array list of the news headlines and urls
    private TextToSpeech ttsReadMe;             // TextToSpeech allows for reading the story
    private String newsURL;                     // URL for the news stories

  //  private String mFeedTitle;
  //   private String mFeedDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        etxtRSSLink = (EditText) findViewById(R.id.rssFeedEditText);
        btnFetchDrudge = (Button) findViewById(R.id.btnFetchid);
        btnRead = (Button) findViewById(R.id.btnReadid);
        btnGetStory = (Button) findViewById(R.id.btnGetStoryid);

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        txtvTitle = (TextView) findViewById(R.id.feedTitle);
        txtvDescription = (TextView) findViewById(R.id.feedDescription);
        txtvLink = (TextView) findViewById(R.id.feedLink);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Start at the drudge report
        etxtRSSLink.setText("http://drudgereportfeed.com/rss.xml");

        ttsReadMe =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    ttsReadMe.setLanguage(Locale.UK);
                }
            }
        });

        btnFetchDrudge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FetchFeedTask().execute((Void) null);
            }
        });

        btnGetStory.setOnClickListener(new View.OnClickListener() {
            //TODO START HERE Use the new URL parser to read in the news story, must fill the link field
            @Override
            public void onClick(View view) {
                txtvLink.setText(mFeedModelList.get(2).link);
                String siteURL = txtvLink.getText().toString();
                (new ParseURL()).execute(new String[]{siteURL});
            }
        });

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak;
                RssFeedListAdapter feedAdapter = (RssFeedListAdapter) mRecyclerView.getAdapter();
                List<RssFeedModel> feed =  feedAdapter.getFeedList();
                //TODO this reads the 2nd news story - need to be able to do all the news stories
                RssFeedModel rss = feed.get(2);
                // for(RssFeedModel rss : feed) {
                newsURL = rss.link;
                Log.i("a href ",newsURL);

//                        new FetchNewsTask().execute((Void) null);
                // }
                //  }
                //   toSpeak = feed.get(1).description;
                toSpeak =  txtvDescription.getText().toString();
                Log.i("info",toSpeak);
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_LONG).show();
                ttsReadMe.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchFeedTask().execute((Void) null);
            }
        });
    }


private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        private String urlLink;

        @Override
        protected void onPreExecute() {
            mSwipeLayout.setRefreshing(true);
            urlLink = etxtRSSLink.getText().toString();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (TextUtils.isEmpty(urlLink))
                return false;

            try {
                if(!urlLink.startsWith("http://") && !urlLink.startsWith("https://"))
                   urlLink = "http://" + urlLink;

                URL url = new URL(urlLink);
                InputStream inputStream = url.openConnection().getInputStream();
                mFeedModelList = parseFeed(inputStream);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mSwipeLayout.setRefreshing(false);

            if (success) {
                txtvTitle.setText("Feed Title2: " + mFeedModelList.get(0).title);
                txtvLink.setText("Feed Link: " + mFeedModelList.get(0).link);

               // txtvDescription.setText("Feed Description2: " + txtvDescription);
               // txtvLink.setText("Feed Link: " + newsURL);
                // Fill RecyclerView
                mRecyclerView.setAdapter(new RssFeedListAdapter(mFeedModelList));
            } else {
                Toast.makeText(MainActivity.this,
                        "Enter a valid Rss feed url: " + urlLink,
                        Toast.LENGTH_LONG).show();
            }
        }

    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException,
            IOException {
        String link = null;
        String title = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null)
                    continue;

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MyXmlParser", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

               if (name.equalsIgnoreCase("link")) {
                    link = result;
               }
                if (name.equalsIgnoreCase("title")) {
                    title = result;
                }

                if (link != null) {
                    if(isItem) {
                            RssFeedModel item = new RssFeedModel(link,title);
                            items.add(item);
                        }
                    else {
                      //  newsURL = link;
                        }

                    link = null;
                    isItem = false;
                }
            }

            return items;
        } finally {
            inputStream.close();
        }
    }
    }



    private class ParseURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings){
    StringBuffer buffer = new StringBuffer();
    try {
        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
        Document doc = Jsoup.connect(strings[0]).get();
        Log.d("JSwa", "Connected to [" + strings[0] + "]");
        // Get document (HTML page) title
        String title = doc.title();
        Log.d("JSwa", "Title [" + title + "]");
        buffer.append("Title: " + title + "rn");

        // Get meta info
        Elements metaElems = doc.select("meta");
        buffer.append("META DATArn");
        for (Element metaElem : metaElems) {
            String name = metaElem.attr("name");
            String content = metaElem.attr("content");
            buffer.append("name[" + name + "] - content [" + content + "] rn");
        }
        Elements topicList = doc.select("h2.topic");
        buffer.append("Topic listrn");
        for (Element topic : topicList) {
            String data = topic.text();
            buffer.append("Data [" + data + "] rn");
        }
    }
    catch(Throwable t) {
        t.printStackTrace();
    }
    return buffer.toString();
    }
        //TODO start with onPostExecute
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            txtvDescription.setText(s);
        }
    }


}



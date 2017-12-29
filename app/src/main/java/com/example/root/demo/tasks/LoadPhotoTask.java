package com.example.root.demo.tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.root.demo.Application;
import com.example.root.demo.MainActivity;
import com.example.root.demo.R;
import com.example.root.demo.model.Photo;
import com.fivehundredpx.api.PxApi;
import com.fivehundredpx.api.auth.AccessToken;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LoadPhotoTask extends AsyncTask<Object, Void, List<Photo>> {

    private static final String TAG = "LoadPhotoTask";
    public static final int DEFAULT_NUM_RESULT = 8;
    private List<Photo> photos = new ArrayList<>();

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public interface Delegate {
        public void success(List<Photo> listPhotos);
        public void fail();
    }

    private Delegate _d;

    public LoadPhotoTask(){}

    public LoadPhotoTask(Delegate _d) {
        super();
        this._d = _d;
    }

    @Override
    protected List<Photo> doInBackground(Object... params) {

        final Context context = Application.getContext();
        AccessToken accessToken = params.length > 0 && null != params[0] ? (AccessToken) params[0] : null;
        int page = params.length > 1 && null != params[1] ? (int) params[1] : 0;
        String pageParam = "";

        if(null == accessToken) {
            SharedPreferences preferences = context.getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE);
            final String token = preferences.getString(Application.PREF_ACCES_TOKEN, null);
            final String tokenSecret = preferences
                    .getString(Application.PREF_TOKEN_SECRET, null);

            if (null != token && null != tokenSecret) {
                accessToken = new AccessToken(token, tokenSecret);
            }
        }

        if (page > 0) {
            pageParam = "&page=" + page;
            Log.e(TAG, "pageParam: " + pageParam);
        }

        final PxApi api = new PxApi(accessToken,
                context.getString(R.string.px_consumer_key),
                context.getString(R.string.px_consumer_secret));
        JSONObject json = api.get("/photos?rpp=" + DEFAULT_NUM_RESULT + pageParam);

        return parseToArrayPhoto(json);
    }

    private List<Photo> parseToArrayPhoto(JSONObject json) {
        if (json != null) {
            try {
//                PHOTOS.clear();
                // Getting JSON Array node
                JSONArray photosData = json.getJSONArray("photos");

                // looping through All Contacts
                for (int i = 0; i < photosData.length(); i++) {
                    JSONObject c = photosData.getJSONObject(i);

                    String id = c.getString("id");
                    String name = c.getString("name");
                    String description = c.getString("description");
                    String imageUrl = c.getString("image_url");
                    URL url = new URL(imageUrl);
                    Bitmap photoBmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                    Photo photo = new Photo(id, name, description, photoBmp);
                    photos.add(photo);
                }
                return photos;
            } catch (final JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
            }catch (Exception e) {
                Log.e(TAG, "Something went wrong: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Couldn't get json from server.");
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Photo> result) {
        if(null!=_d && null!=result){
            _d.success(result);
        }
    }


}

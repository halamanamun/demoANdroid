package com.example.root.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.root.demo.listener.ItemListener;
import com.example.root.demo.listener.OnLoadMoreListener;
import com.example.root.demo.model.Photo;
import com.example.root.demo.tasks.LoadPhotoTask;
import com.fivehundredpx.api.FiveHundredException;
import com.fivehundredpx.api.auth.AccessToken;
import com.fivehundredpx.api.tasks.XAuth500pxTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity implements XAuth500pxTask.Delegate, LoadPhotoTask.Delegate{
    private static final String TAG = "MyDemo";
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private List<Photo> photos = new ArrayList<>();
    public UserAdapter mUserAdapter;
    protected ItemListener mListener;
    private final ItemListener mItemListener = new ItemListener() {
        @Override
        public void onItemClick(Photo photo, int position) {
            Toast.makeText(Application.getContext(), photo.getName(), Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        final String accesToken = preferences.getString(Application.PREF_ACCES_TOKEN, null);
        final String tokenSecret = preferences
                .getString(Application.PREF_TOKEN_SECRET, null);

        if (null != accesToken && null != tokenSecret) {
            onSuccess(new AccessToken(accesToken, tokenSecret));
        }
        Log.w(TAG, "success onCreate");
        XAuth500pxTask loginTask = new XAuth500pxTask(this);
        loginTask.execute(getString(R.string.px_consumer_key), getString(R.string.px_consumer_secret),getString(R.string.px_username),getString(R.string.px_pass));
    }

    @Override
    public void onSuccess(AccessToken result) {
        SharedPreferences preferences = getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Application.PREF_ACCES_TOKEN, result.getToken());
        editor.putString(Application.PREF_TOKEN_SECRET, result.getTokenSecret());
        editor.commit();

        LoadPhotoTask loadPhotoTask = new LoadPhotoTask(this);
        loadPhotoTask.execute(result);
    }

    @Override
    public void onFail(FiveHundredException e) {
        Log.w(TAG, "success "+ e.getMessage());
    }

    @Override
    public void success(List<Photo> listPhotos) {
        photos = listPhotos;
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("LoadMoreRecycleView");
        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);

        AutoFitGridLayoutManager layoutManager = new AutoFitGridLayoutManager(this, 500);
        mRecyclerView.setLayoutManager(layoutManager);
        mUserAdapter = new UserAdapter();
        mRecyclerView.setAdapter(mUserAdapter);

        mUserAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                Log.e("haint", "Load More");
                photos.add(null);

                mRecyclerView.post(new Runnable() {
                    public void run() {
                        mUserAdapter.notifyItemInserted(photos.size() - 1);
                    }
                });

                //Load more data for reyclerview
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("haint", "ahihi" + (photos.size() / 8 ));
                        //Remove loading item
                        photos.remove(photos.size() - 1);
                        mUserAdapter.notifyItemRemoved(photos.size());

                        int pageNum = (photos.size() / LoadPhotoTask.DEFAULT_NUM_RESULT) + 1;
                        List<Photo> photoByPage = null;
                        try {
                            photoByPage = new LoadPhotoTask().execute(null, pageNum).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        photos.addAll(photoByPage);
                        Log.e("haint", "ahihi size:" + photos.size());
                        mUserAdapter.notifyDataSetChanged();
                        mUserAdapter.setLoaded();
                    }
                }, 5000);
            }
        });
    }

    @Override
    public void fail() {
    }


    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;
        public LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar1);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder{
        public ImageView ivPhoto;
        public UserViewHolder(View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
        }
    }

    class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEW_TYPE_ITEM = 0;
        private final int VIEW_TYPE_LOADING = 1;

        private OnLoadMoreListener mOnLoadMoreListener;

        private boolean isLoading;
        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;

        public UserAdapter() {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    final RecyclerView.Adapter a = mRecyclerView != null ? mRecyclerView.getAdapter() : null;
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    Log.e(TAG, "LinearLayoutManager " + totalItemCount);

                    if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (mOnLoadMoreListener != null) {
                            mOnLoadMoreListener.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });

        }

        public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
            this.mOnLoadMoreListener = mOnLoadMoreListener;
        }

        @Override
        public int getItemViewType(int position) {
            return photos.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_user_item, parent, false);
                return new UserViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_loading_item, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof UserViewHolder) {
                final Photo user = photos.get(position);
                UserViewHolder userViewHolder = (UserViewHolder) holder;
                userViewHolder.ivPhoto.setImageBitmap(user.getPhotoBmp());
                userViewHolder.ivPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mItemListener.onItemClick(user, position);
                    }
                });

            } else if (holder instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return photos == null ? 0 : photos.size();
        }

        public void setLoaded() {
            isLoading = false;
        }
    }




}

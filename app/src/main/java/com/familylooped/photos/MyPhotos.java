package com.familylooped.photos;


import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.familylooped.R;
import com.familylooped.common.AppController;
import com.familylooped.common.Utilities;
import com.familylooped.common.activities.BaseActionBarActivity;
import com.familylooped.common.async.AsyncHttpRequest;
import com.familylooped.common.fragments.BaseFragment;
import com.familylooped.common.fragments.DialogClickListener;
import com.familylooped.common.logger.Log;
import com.google.gson.Gson;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MyPhotos#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyPhotos extends BaseFragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static String TAG = "My Photos";
    private static String NOTIFICATION = "notification";
    private ThinDownloadManager downloadManager;
    private ArrayList<ModelPhoto> photosUri;
    private ProgressDialog mProgressDialog;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private boolean is_notification;
    private GridView mGridView;
    public ArrayList<ModelMyPhoto> mList;
    private RequestQueue mRequestQueue;
    private AdapterMyPhoto mAdapterMyPhoto;
    private int DOWNLOAD_THREAD_POOL_SIZE = 4;
    Handler handler = new Handler();
    private int mDownloadIndex = 0;
    private ImageChooserManager imageChooserManager;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyPhotos.
     */
    // TODO: Rename and change types and number of parameters
    public static MyPhotos newInstance(String param1, String param2) {
        MyPhotos fragment = new MyPhotos();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static MyPhotos newInstance() {
        MyPhotos fragment = new MyPhotos();
        return fragment;
    }

    public static MyPhotos newInstance(boolean is_notification) {
        MyPhotos fragment = new MyPhotos();
        Bundle args = new Bundle();
        args.putBoolean(NOTIFICATION, is_notification);
        fragment.setArguments(args);
        return fragment;
    }


    public MyPhotos() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            is_notification = getArguments().getBoolean(NOTIFICATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_photos, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        init(view);
    }

    private void init(View view) {
        ((ImageButton) view.findViewById(R.id.btn_add_photo)).setOnClickListener(this);
        ((ImageButton) view.findViewById(R.id.btn_select)).setOnClickListener(this);
        ((ImageButton) view.findViewById(R.id.btn_delete)).setOnClickListener(this);
        ((ImageButton) view.findViewById(R.id.btn_back)).setOnClickListener(this);
        // createFolder();
        mList = new ArrayList<ModelMyPhoto>();
        mGridView = (GridView) view.findViewById(R.id.grid_view);
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("Downloading");
        mProgressDialog.setMessage("Downloading images...");


        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                showDialog("Do you want to remove this photo", "Yes", "No", new DialogClickListener() {
                    @Override
                    public void onPositiveButtonClick() {
                        removePhoto(position);
                    }
                });

                return false;
            }
        });
        if (is_notification) {
            download_photos();
        } else {
            showPhotos();
        }


    }

    private void showPhotos() {
        photosUri = new ArrayList<>();
        File file = new File(Environment.getExternalStorageDirectory() + "/FamilyLooped");
        File fileList[] = file.listFiles();
        if (fileList != null) {

            for (int i = 0; i < fileList.length; i++) {
                photosUri.add(new ModelPhoto(fileList[i].getAbsolutePath(), false));
            }
        }
        mAdapterMyPhoto = new AdapterMyPhoto(getActivity(), photosUri, MyPhotos.this);
        mGridView.setAdapter(mAdapterMyPhoto);

    }


    private void downloadFile(String url) {

        Uri downloadUri = Uri.parse(url);
        Uri destinationUri = Uri.parse(Environment.getExternalStorageDirectory() + "/FamilyLooped" + "/" + System.currentTimeMillis() + "_.jpg");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)

                //.addCustomHeader("Auth-Token", "YourTokenApiKey")
                //.setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {
                        Log.e("STATS ", "Download complete Success " + id);
                        mDownloadIndex++;
                        if (mDownloadIndex == mList.size()) {
                            showPhotos();
                            mProgressDialog.dismiss();
                        }
                    }

                    @Override
                    public void onDownloadFailed(int id, int errorCode, String message) {
                        Log.e("STATS ", "Download Failed " + id);
                    }

                    @Override
                    public void onProgress(int id, long totalBytes, int progress) {
                        Log.e("OnProgress ", "id " + id + " progress " + progress);

                    }
                });


        downloadManager = new ThinDownloadManager();

        downloadManager.add(downloadRequest);

    }

    private void removePhoto(final int position) {

        HashMap<String, String> urlParams = new HashMap<>();
        // urlParams.put("photoId", mList.get(position).getId());
        AsyncHttpRequest request = new AsyncHttpRequest(getActivity(), "delete_photo", Utilities.BASE_URL + "deletePhoto", urlParams, new AsyncHttpRequest.HttpResponseListener() {
            @Override
            public void onResponse(String response) {
                mList.remove(position);
                mAdapterMyPhoto.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        AppController.getInstance().addToRequestQueue(request);
    }

    private void download_photos() {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("userId", Utilities.getSaveData(getActivity(), Utilities.USER_ID));
        if (Utilities.getSaveData(getActivity(), Utilities.PHOTO_TIME) != null) {

            urlParams.put("dateTime", Utilities.getSaveData(getActivity(), Utilities.PHOTO_TIME));
        } else {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.ENGLISH);
            String timeString = time.format(c.getTime());
            StringBuffer currentTime = new StringBuffer(timeString);
            currentTime.insert(timeString.indexOf("+"), " ");
            Log.e("formatted string: ", "" + currentTime);
            Utilities.saveData(getActivity(), Utilities.PHOTO_TIME, "" + currentTime);
            urlParams.put("dateTime", "2015-05-01 11:01:04 +0500");

        }


        AsyncHttpRequest request = new AsyncHttpRequest(getActivity(), "getPictures", Utilities.BASE_URL + "getPictures", urlParams, new AsyncHttpRequest.HttpResponseListener() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    if (TextUtils.equals(object.getString("status"), Utilities.SUCCESS)) {
                        JSONArray data = object.getJSONArray("photos");
                        Gson gson = new Gson();
                        mProgressDialog.show();
                        ;
                        for (int i = 0; i < data.length(); i++) {
                            mList.add(gson.fromJson(data.getJSONObject(i).toString(), ModelMyPhoto.class));
                            downloadFile(data.getJSONObject(i).getString("image"));

                        }

                    } else {

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        AppController.getInstance().addToRequestQueue(request);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        /*inflater.inflate(R.menu.menu_my_photo, menu);
        ImageButton imageButton = (ImageButton) menu.findItem(R.id.action_play).getActionView();
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //changeFragment(SlideShow.newInstance(), SlideShow.TAG);
                Log.e("URL", "Image " + mList.get(0).getImage());
                //downloadFile(mList.get(0).getImage());

            }
        });*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add_photo:
                addPhoto();
                break;
            case R.id.btn_delete:
                break;
            case R.id.btn_select:
                break;

            case R.id.btn_back:
                ((BaseActionBarActivity)getActivity()).popFragmentIfStackExist();
                break;
        }
    }

    private void addPhoto() {

        imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE);
        imageChooserManager.setImageChooserListener(new ImageChooserListener() {
            @Override
            public void onImageChosen(final ChosenImage image) {

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (image != null) {

                            copyFile(image.getFileThumbnail(), Environment.getExternalStorageDirectory() + "/FamilyLooped");

                        }
                    }
                });
            }

            @Override
            public void onError(String s) {

            }
        });
        try {
            imageChooserManager.choose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean copyFile(String from, String to) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                int end = from.toString().lastIndexOf("/");
                String str1 = from.toString().substring(0, end);
                String str2 = from.toString().substring(end + 1, from.length());
                File source = new File(str1, str2);
                File destination = new File(to, str2);
                if (source.exists()) {
                    FileChannel src = new FileInputStream(source).getChannel();
                    FileChannel dst = new FileOutputStream(destination).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    showPhotos();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK &&
                (requestCode == ChooserType.REQUEST_PICK_PICTURE ||
                        requestCode == ChooserType.REQUEST_CAPTURE_PICTURE)) {
            imageChooserManager.submit(requestCode, data);
        }
    }
}

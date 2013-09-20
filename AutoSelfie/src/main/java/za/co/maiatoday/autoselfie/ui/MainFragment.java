package za.co.maiatoday.autoselfie.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.util.ImageUtils;
import za.co.maiatoday.autoselfie.util.SelfieStatus;

/**
 * Created by maia on 2013/09/01.
 */
public class MainFragment extends Fragment {   // Update status button
    private static final int REQUEST_IMAGE = 2;
    Button btnUpdateStatus;
    // EditText for update
    EditText txtUpdate;
    // lbl update
    TextView lblUpdate;
    private ImageView imageView;
    Button btnSnap;

    SelfieStatus selfie = new SelfieStatus();
    private boolean doHide = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);


        setHasOptionsMenu(true);

        imageView = (ImageView) view.findViewById(R.id.result);
        btnSnap = (Button) view.findViewById(R.id.btnSnap);
        // All UI elements
        btnUpdateStatus = (Button) view.findViewById(R.id.btnUpdateStatus);
        txtUpdate = (EditText) view.findViewById(R.id.txtUpdateStatus);
        txtUpdate.setEnabled(true);
        lblUpdate = (TextView) view.findViewById(R.id.lblUpdate);


        /**
         * Button click event to Update Status, will call UpdateTwitterStatusTask()
         * function
         * */
        btnUpdateStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                OnTwitterRequest activity = (OnTwitterRequest) getActivity();
                if (!activity.isTwitterLoggedInAlready()) {
                    activity.logInTwitter();
                } else {
                    if (selfie.processSelfie()) {
                        imageView.setImageBitmap(selfie.getBmpToPost());
                        lblUpdate.setText(selfie.getStatus());
                        if (!TextUtils.isEmpty(txtUpdate.getText().toString())) {
                            selfie.setStatus(txtUpdate.getText().toString());
                        }
                        activity.updateStatus(selfie);
                        if (doHide) {
                            Runnable r = new Runnable() {
                                public void run() {
                                    imageView.setImageBitmap(selfie.getOrig());
                                }
                            };
                            imageView.postDelayed(r, 2000);
                        }
                    }
                }
            }
        });

        btnSnap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                openImageIntent();
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selfie.processSelfie();
                imageView.setImageBitmap(selfie.getBmpToPost());
                lblUpdate.setText(selfie.getStatus());
                txtUpdate.setText(selfie.getStatus());

            }
        });
        imageView.setOnTouchListener(selfie);

//        BitmapDrawable d = (BitmapDrawable) getResources().getDrawable(R.drawable.autoselfie_test);
//        if (d != null) {
//            selfie.setOrig(d.getBitmap());
//        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        OnTwitterRequest activity = (OnTwitterRequest) getActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
            case REQUEST_IMAGE:
                processImage(data);
                imageView.setImageBitmap(selfie.getBmpToPost()); //TODO only for debug to see result here
                txtUpdate.setText(selfie.getStatus());
                break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    private Uri outputFileUri;

    /**
     * Open and intent to get an image, either from camera or gallery
     */
    private void openImageIntent() {

// Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "autoSelfie" + File.separator);
        root.mkdirs();
        final String fname = ImageUtils.getUniqueImageFilename();
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        startActivityForResult(chooserIntent, REQUEST_IMAGE);
    }

    /**
     * Process the intent data and get the image
     *
     * @param data
     */
    private void processImage(Intent data) {
        final boolean isCamera;
        if (data == null) {
            isCamera = true;
        } else {
            final String action = data.getAction();
            if (action == null) {
                isCamera = false;
            } else {
                isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }
        }

        Uri selectedImageUri;
        if (isCamera) {
            selectedImageUri = outputFileUri;
        } else {
            selectedImageUri = data == null ? null : data.getData();
        }
        try {
            Bitmap bitmap = ImageUtils.getSizedBitmap(getActivity(), selectedImageUri, imageView.getHeight());
            if (bitmap != null) {
                selfie.setOrig(bitmap);
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {

            Toast.makeText(getActivity(),
                "Problem loading file", Toast.LENGTH_LONG).show();
        }
    }

}

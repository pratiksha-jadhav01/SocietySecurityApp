package com.example.securiodb;

import android.content.Context;
import android.net.Uri;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to handle image uploads to Cloudinary using Volley multipart requests.
 */
public class CloudinaryUploader {

    // Cloudinary credentials provided by user
    public static final String CLOUD_NAME    = "dgvlx2nad";
    public static final String UPLOAD_PRESET = "visitor_upload";
    public static final String UPLOAD_URL    =
        "https://api.cloudinary.com/v1_1/dgvlx2nad/image/upload";

    /**
     * Callback interface for upload status
     */
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
        void onProgress(int percent);
    }

    /**
     * Converts a content Uri to a byte array.
     */
    public static byte[] uriToBytes(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) throw new IOException("Cannot open URI");
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        inputStream.close();
        return buffer.toByteArray();
    }

    /**
     * Main method to upload an image to Cloudinary using Volley.
     * @param context   Application context
     * @param imageUri  Uri of the image to upload
     * @param callback  UploadCallback to receive results
     */
    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        try {
            final byte[] imageBytes = uriToBytes(context, imageUri);
            callback.onProgress(10); // Initial progress

            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                UPLOAD_URL,
                response -> {
                    try {
                        String responseStr = new String(response.data);
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        // Extract the secure URL from Cloudinary's JSON response
                        String secureUrl = jsonResponse.getString("secure_url");
                        callback.onSuccess(secureUrl);
                    } catch (JSONException e) {
                        callback.onFailure("JSON Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    String msg = "Upload failed";
                    if (error.networkResponse != null) {
                        msg += " (Status code: " + error.networkResponse.statusCode + ")";
                    } else if (error.getMessage() != null) {
                        msg += ": " + error.getMessage();
                    }
                    callback.onFailure(msg);
                }
            ) {
                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("file", new DataPart(
                        "visitor_" + System.currentTimeMillis() + ".jpg",
                        imageBytes,
                        "image/jpeg"
                    ));
                    return params;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    // Using an unsigned upload preset as required
                    params.put("upload_preset", UPLOAD_PRESET);
                    return params;
                }
            };

            // Set a 60-second timeout as required for large image uploads
            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            queue.add(multipartRequest);
            callback.onProgress(30); // Upload initiated

        } catch (IOException e) {
            callback.onFailure("Image read error: " + e.getMessage());
        }
    }
}

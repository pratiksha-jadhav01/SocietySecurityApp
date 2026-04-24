package com.example.securiodb;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Custom Volley request to handle multipart/form-data uploads.
 */
public class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final String mBoundary;

    public VolleyMultipartRequest(int method, String url,
                                Response.Listener<NetworkResponse> listener,
                                Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mBoundary = "apiclient-" + System.currentTimeMillis();
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + mBoundary;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            // Add string parameters
            Map<String, String> params = getParams();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    buildTextPart(dos, mBoundary, entry.getKey(), entry.getValue());
                }
            }
            // Add byte data (files)
            Map<String, DataPart> data = getByteData();
            if (data != null) {
                for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                    buildFilePart(dos, mBoundary, entry.getKey(), entry.getValue());
                }
            }
            // Final boundary
            dos.writeBytes("--" + mBoundary + "--\r\n");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void buildTextPart(DataOutputStream dos, String boundary, String key, String value) throws IOException {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n");
        dos.writeBytes("\r\n");
        dos.writeBytes(value + "\r\n");
    }

    private void buildFilePart(DataOutputStream dos, String boundary, String key, DataPart dataFile) throws IOException {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key
                + "\"; filename=\"" + dataFile.getFileName() + "\"\r\n");
        dos.writeBytes("Content-Type: " + dataFile.getType() + "\r\n");
        dos.writeBytes("\r\n");
        dos.write(dataFile.getContent());
        dos.writeBytes("\r\n");
    }

    /**
     * Override this to provide file data.
     */
    protected Map<String, DataPart> getByteData() {
        return null;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    /**
     * Model for file data parts.
     */
    public static class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
        public String getType() { return type; }
    }
}

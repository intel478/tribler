package org.tribler.android;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpResponseException;

/**
 * Provides interface to deserialize JSON responses, using AsyncHttpResponseHandler.
 */
public abstract class JsonStreamAsyncHttpResponseHandler extends AsyncHttpResponseHandler {
    private final static String LOG_TAG = "JsonAsyncHttpRH";

    protected static Gson gson = new Gson();

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResponseMessage(HttpResponse response) throws IOException {
        // do not process if request has been cancelled
        if (!Thread.currentThread().isInterrupted()) {
            StatusLine status = response.getStatusLine();
            getResponseData(response.getEntity());
            // additional cancellation check as getResponseData() can take non-zero time to process
            if (!Thread.currentThread().isInterrupted()) {
                if (status.getStatusCode() >= 300) {
                    sendFailureMessage(status.getStatusCode(), response.getAllHeaders(), null, new HttpResponseException(status.getStatusCode(), status.getReasonPhrase()));
                } else {
                    sendSuccessMessage(status.getStatusCode(), response.getAllHeaders(), null);
                }
            }
        }
    }

    /**
     * Deconstructs response into given content handler
     *
     * @param entity returned HttpEntity
     * @throws java.io.IOException if there is problem assembling JSON response from stream
     * @see cz.msebera.android.httpclient.HttpEntity
     */
    protected void getResponseData(HttpEntity entity) throws IOException {
        if (entity != null) {
            InputStream instream = entity.getContent();
            InputStreamReader inputStreamReader = null;
            if (instream != null) {
                try {
                    inputStreamReader = new InputStreamReader(instream, getCharset());
                    JsonReader jsonReader = new JsonReader(inputStreamReader);
                    readJsonStream(jsonReader);
                    jsonReader.close();
                } catch (IOException e) {
                    AsyncHttpClient.log.e(LOG_TAG, "getResponseData exception", e);
                } finally {
                    AsyncHttpClient.silentCloseInputStream(instream);
                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                        } catch (IOException e) { /*ignore*/ }
                    }
                }
            }
        }
    }

    /**
     * This code reads a JSON document containing an array of messages.
     * It steps through array elements as a stream to avoid loading the complete document into memory.
     * It is concise because it uses Gson’s object-model to parse the individual messages:
     *
     * @param reader JsonReader
     * @return
     * @throws IOException
     */
    protected abstract void readJsonStream(JsonReader reader) throws IOException;

}
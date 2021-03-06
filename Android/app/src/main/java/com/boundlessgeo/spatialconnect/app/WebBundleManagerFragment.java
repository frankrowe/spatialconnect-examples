package com.boundlessgeo.spatialconnect.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Web bundles are stored in external storage.
 */
public class WebBundleManagerFragment extends Fragment implements ListView.OnItemClickListener {

    private ListView listView;
    private OnWebBundleSelectedListener webBundleSelectedListener;
    private static final String BUNDLE_DIRECTORY_NAME = "bundles";
    private String downloadUrl;
    private WebBundleAdapter webBundleAdapter;
    private File rootBundlesDir;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootBundlesDir = getActivity().getExternalFilesDir(BUNDLE_DIRECTORY_NAME);

        // inflate the views
        View view = inflater.inflate(R.layout.fragment_web_bundle, null);
        listView = (ListView) view.findViewById(R.id.web_bundle_list);

        // Set the adapter for the list view
        webBundleAdapter = new WebBundleAdapter(getActivity(), R.layout.item_web_bundle, getWebBundleFiles());
        listView.setAdapter(webBundleAdapter);

        // Set the list's click listener
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final EditText webBundleValue = (EditText) getActivity().findViewById(R.id.web_bundle_value);
        webBundleValue.setInputType(InputType.TYPE_CLASS_TEXT);
        webBundleValue.setImeOptions(EditorInfo.IME_ACTION_DONE);
        webBundleValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                downloadUrl = webBundleValue.getText().toString();
            }
        });
            // add callback to the delete button
        final Button button = (Button) getView().findViewById(R.id.download_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DownloadFile().execute(downloadUrl);
            }
        });
    }

    class DownloadFile extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... url) {
            int count;
            try {
                URLConnection conn = new URL(url[0]).openConnection();
                conn.connect();
                int contentLength = conn.getContentLength();

                // for now we assume bundles need to end with .zip
                File file = new File(rootBundlesDir, UUID.randomUUID().toString() + ".zip");

                InputStream input = new BufferedInputStream(conn.getInputStream());
                OutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) (total * 100 / contentLength));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                unzipFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            webBundleAdapter.clear();
            webBundleAdapter.addAll(getWebBundleFiles());
            webBundleAdapter.notifyDataSetChanged();
        }
    }


    /**
     * The MainActivity must implement this so it can update its selectedWebBundle and notify the WebView launcher.
     *
     * @see <a href="http://developer.android.com/guide/components/fragments.html#EventCallbacks">
     * http://developer.android.com/guide/components/fragments.html#EventCallbacks<</a>
     */
    public interface OnWebBundleSelectedListener {
        void onWebBundleSelectedListener(File file);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            webBundleSelectedListener = (OnWebBundleSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWebBundleSelectedListener");
        }
    }


    /**
     * When an item in the listView is clicked, this callback is called.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        listView.setItemChecked(position, true);
        webBundleSelectedListener.onWebBundleSelectedListener((File) listView.getItemAtPosition(position));
    }

    /**
     * Helper method to unzip any zip files in the bundle directory and return a List File objects for the
     * unzipped web bundle directories.
     */
    private ArrayList<File> getWebBundleFiles() {
        ArrayList<File> bundleList = new ArrayList<>();

        if (!rootBundlesDir.exists()) {
            rootBundlesDir.mkdir();
        }

        // if the zip file isn't already unzipped in the bundles directory, then unzip it.
        // this is to support bundles that are packaged with the application as well as zip files (bundles) that have
        // been downloaded since the last time the WebBundleAdapter has been notified of an update
        for (File f : rootBundlesDir.listFiles()) {
           unzipFile(f);
        }

        // add all unzipped bundles
        for (File f : rootBundlesDir.listFiles()) {
            if (f.isDirectory() && !f.getName().equals("__MACOSX")) {
                bundleList.add(f);
            }
        }

        return bundleList;
    }

    private void unzipFile(File f) {
        if (f.isFile() && !bundleIsUnzipped(f)) {
            ZipFile zipFile = null;
            File rootBundlesDir = getActivity().getExternalFilesDir(BUNDLE_DIRECTORY_NAME);
            File bundleDir = new File(rootBundlesDir, f.getName().replace(".zip",""));
            try {
                zipFile = new ZipFile(f);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    File entryDestination = new File(bundleDir, entry.getName());
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    }
                    else {
                        entryDestination.getParentFile().mkdirs();
                        InputStream in = zipFile.getInputStream(entry);
                        OutputStream out = new FileOutputStream(entryDestination);
                        IOUtils.copy(in, out);
                        IOUtils.closeQuietly(in);
                        out.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Helper method to determine if a web bundle has been unzipped in the bundles directory.
     *
     * @param bundle - a file that may or may not have been unziped
     * @return true if a directory with the bundle name exists in the bundles directory
     */
    private boolean bundleIsUnzipped(File bundle) {
        String bundleName = bundle.getName().replace(".zip", "");
        File bundlesDir = getActivity().getExternalFilesDir(BUNDLE_DIRECTORY_NAME);
        return new File(bundlesDir, bundleName).exists();
    }

    /**
     * Custom adapter to show the File name of the web bundle.
     */
    class WebBundleAdapter extends ArrayAdapter<File> {

        public WebBundleAdapter(Context context, int resource, ArrayList<File> files) {
            super(context, resource, files);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File file = getItem(position);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View rowView = inflater.inflate(R.layout.item_web_bundle, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.web_bundle_name);
            textView.setText(file.getName());
            return rowView;
        }
    }

}

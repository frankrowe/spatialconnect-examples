package com.boundlessgeo.spatialconnect.app;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;

import rx.functions.Action1;

public class DataStoreDetailsActivityFragment extends Fragment {

    private String id;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle b = getActivity().getIntent().getExtras();
        id = (String)b.get("id");
        return inflater.inflate(R.layout.fragment_data_store_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView storeId = (TextView)getView().findViewById(R.id.data_store_id);
        TextView storeName = (TextView)getView().findViewById(R.id.data_store_name);
        TextView storeType = (TextView)getView().findViewById(R.id.data_store_type);
        TextView storeVersion = (TextView)getView().findViewById(R.id.data_store_version);
        final TextView storeStatus = (TextView)getView().findViewById(R.id.data_store_status);
        Context c = getActivity().getBaseContext();
        SCServiceManager serviceManager =  SpatialConnectService.getInstance().getServiceManager(c);
        SCDataStore ds = serviceManager.getDataService().getStoreById(id);

        storeId.setText(ds.getStoreId());
        storeName.setText(ds.getName());
        storeType.setText(ds.getType());
        storeVersion.setText(String.valueOf(ds.getVersion()));
        storeStatus.setText(getStatus(ds.getStatus()));
        serviceManager.getDataService().storeEvents.subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                storeStatus.setText(getStatus(scStoreStatusEvent.getStatus()));
            }
        });
        serviceManager.getDataService().storeEvents.connect();
    }

    private String getStatus(SCDataStoreStatus scDataStoreStatus) {
        String status;
        switch (scDataStoreStatus) {
            case SC_DATA_STORE_STARTED:
                status = "Started";
                break;
            case SC_DATA_STORE_RUNNING:
                status = "Running";
                break;
            case SC_DATA_STORE_PAUSED:
                status = "Paused";
                break;
            case SC_DATA_STORE_STOPPED:
                status = "Stopped";
                break;
            case SC_DATA_STORE_DOWNLOADING:
                status = "Downloading";
                break;
            default:
                status = "Initializing";
                break;
        }
        return status;
    }
}

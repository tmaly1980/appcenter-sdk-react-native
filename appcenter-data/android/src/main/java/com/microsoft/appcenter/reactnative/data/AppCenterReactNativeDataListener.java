// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.reactnative.data;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentMetadata;
import com.microsoft.appcenter.data.models.RemoteOperationListener;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppCenterReactNativeDataListener implements RemoteOperationListener {

    private static final String ON_REMOTE_OPERATION_COMPLETED_EVENT = "AppCenterRemoteOperationCompleted";

    private ReactApplicationContext mReactApplicationContext;

    private List<Map.Entry<String, WritableMap>> mPendingEvents = new ArrayList<>();

    @SuppressWarnings("WeakerAccess")
    public final void setReactApplicationContext(ReactApplicationContext reactApplicationContext) {
        mReactApplicationContext = reactApplicationContext;
    }

    @Override
    public void onRemoteOperationCompleted(String operation, DocumentMetadata documentMetadata, DataException dataException) {
        sendEvent(ON_REMOTE_OPERATION_COMPLETED_EVENT, AppCenterReactNativeDataUtils.convertRemoteOperationDataToWritableMap(operation, documentMetadata, dataException));
    }

    private void sendEvent(String eventType, WritableMap report) {
        if (mReactApplicationContext != null) {
            if (mReactApplicationContext.hasActiveCatalystInstance()) {
                mReactApplicationContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(eventType, report);
            } else {
                mPendingEvents.add(new AbstractMap.SimpleEntry<>(eventType, report));
                mReactApplicationContext.addLifecycleEventListener(lifecycleEventListener);
            }
        }
    }

    private void replayPendingEvents() {
        for (Map.Entry<String, WritableMap> event : mPendingEvents) {
            sendEvent(event.getKey(), event.getValue());
        }
        mPendingEvents.clear();
    }

    private LifecycleEventListener lifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onHostResume() {
            mReactApplicationContext.removeLifecycleEventListener(lifecycleEventListener);
            replayPendingEvents();
        }

        @Override
        public void onHostPause() {
        }

        @Override
        public void onHostDestroy() {
        }
    };
}

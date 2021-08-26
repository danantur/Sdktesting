// 
// Decompiled by Procyon v0.5.36
// 

package com.microlife_sdk.model.protocol;

import java.util.Locale;
import java.util.Calendar;
import java.util.Iterator;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.content.IntentFilter;

import com.microlife_sdk.model.abcdef.e;
import com.microlife_sdk.model.data.DRecord;
import com.microlife_sdk.model.data.FunctionSettingValues;
import com.microlife_sdk.model.data.VersionData;
import com.microlife_sdk.model.data.User;
import com.microlife_sdk.model.data.SettingValues;
import com.microlife_sdk.model.data.DeviceInfo;
import com.microlife_sdk.model.data.CBPdataAndCalCBP;
import com.microlife_sdk.model.data.CurrentAndMData;
import android.net.Uri;
import java.util.ArrayList;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.Context;
import android.os.Message;
import com.microlife_sdk.model.XlogUtils;
import java.util.TimerTask;
import android.content.BroadcastReceiver;
import android.os.Handler;
import java.util.List;
import android.app.Activity;
import java.util.Timer;
import com.microlife_sdk.model.bluetooth.MyBluetoothLE;
import com.microlife_sdk.model.bluetooth.BluetoothLEClass;

public class WBOProtocol implements BluetoothLEClass.OnIMBluetoothLEListener, MyBluetoothLE.OnWriteStateListener
{
    public static final String TAG = "WBOProtocol";
    public static WBOProtocol protocol;
    public static final int RECEIVED_ERROR_COUNT = 15000;
    public static final String CMD_REPLY_RESULT_NACK = "91";
    public static final String SPACE_ASSCII = "00";
    public MyBluetoothLE myBluetooth;
    public int receiveErrorCount;
    public StringBuilder allReceivedCommand;
    public boolean isSimulationMode;
    public boolean isMeasurement;
    public int position;
    public Timer simulationTimer;
    public int origin;
    public String bondMacAddress;
    public Activity myAty;
    public String oldCom;
    public List<String> targetDeviceNames;
    public ConnectState mConnectState;
    public Handler mHandler;
    public MyBluetoothLE.OnWriteStateListener mOnWriteStateListener;
    public OnNotifyStateListener mOnNotifyStateListener;
    public OnConnectStateListener onConnectStateListener;
    public OnDataResponseListener onDataResponseListener;
    public BroadcastReceiver bondedBTReceiver;
    
    private void startSimulationTimer() {
        this.cancelSimulationTimer();
        this.position = 0;
        (this.simulationTimer = new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                WBOProtocol.access$100(WBOProtocol.this).sendEmptyMessage(100);
            }
        }, 100L, 700L);
    }
    
    private void cancelSimulationTimer() {
        final Timer simulationTimer;
        if ((simulationTimer = this.simulationTimer) != null) {
            simulationTimer.cancel();
            this.simulationTimer = null;
        }
    }
    
    public static WBOProtocol getInstance(final Activity activity, final boolean b, final boolean b2, final String s) {
        XlogUtils.initXlog(activity, b2);
        if (com.microlife_sdk.model.abcdef.b.a(s) == 3) {
            if (WBOProtocol.protocol == null) {
                WBOProtocol.protocol = new WBOProtocol(activity, b, b2);
            }
            return WBOProtocol.protocol;
        }
        return null;
    }
    
    public WBOProtocol(final Activity myAty, final boolean isSimulationMode, final boolean b) {
        this.receiveErrorCount = 0;
        this.isSimulationMode = false;
        this.isMeasurement = false;
        this.position = 0;
        this.origin = 66;
        this.bondMacAddress = "";
        this.oldCom = "";
        this.mHandler = new Handler() {
            public void handleMessage(final Message message) {
                final int what;
                if ((what = message.what) != 0) {
                    if (what != 100) {
                        if (what != 101) {
                            if (what != 1000) {
                                if (what == 1001) {
                                    final OnNotifyStateListener mOnNotifyStateListener;
                                    if ((mOnNotifyStateListener = WBOProtocol.this.mOnNotifyStateListener) != null) {
                                        mOnNotifyStateListener.onNotifyMessage((String)message.obj);
                                    }
                                }
                            }
                            else {
                                final MyBluetoothLE.OnWriteStateListener mOnWriteStateListener;
                                if ((mOnWriteStateListener = WBOProtocol.this.mOnWriteStateListener) != null) {
                                    mOnWriteStateListener.onWriteMessage(message.arg1 == 1, (String)message.obj);
                                }
                            }
                        }
                        else {
                            WBOProtocol.this.connectionStatus(17);
                        }
                    }
                    else {
                        final WBOProtocol a = WBOProtocol.this;
                        ++a.position;
                        final OnConnectStateListener onConnectStateListener;
                        if ((onConnectStateListener = a.onConnectStateListener) != null) {
                            onConnectStateListener.onScanResult("abcde12345678" + WBOProtocol.this.position, "Fuel " + WBOProtocol.this.position, WBOProtocol.this.position - 40);
                        }
                        final WBOProtocol a2;
                        if ((a2 = WBOProtocol.this).position >= 10) {
                            WBOProtocol.access$000(a2).cancel();
                            WBOProtocol.access$002(WBOProtocol.this, null);
                        }
                    }
                }
                else {
                    WBOProtocol.this.dataResult((String)message.obj);
                }
            }
        };
        this.bondedBTReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                switch (((BluetoothDevice)intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getBondState()) {
                    case 11: {
                        XlogUtils.xLog(WBOProtocol.access$200(), "bondedBTReceiver bonding...");
                        break;
                    }
                    case 10: {
                        XlogUtils.xLog(WBOProtocol.access$200(), "bondedBTReceiver can't bond...");
                    }
                    case 12: {
                        XlogUtils.xLog(WBOProtocol.access$200(), "bondedBTReceiver bonded...");
                        WBOProtocol.access$300(WBOProtocol.this).unregisterReceiver(WBOProtocol.this.bondedBTReceiver);
                        final WBOProtocol a = WBOProtocol.this;
                        a.connect(WBOProtocol.access$400(a));
                        break;
                    }
                }
            }
        };
        this.isSimulationMode = isSimulationMode;
        if (this.isSimulationMode) {
            return;
        }
        (this.targetDeviceNames = new ArrayList<String>()).add("WatchBP Office");
        if (this.myBluetooth == null) {
            this.myBluetooth = MyBluetoothLE.getInstance((Context)myAty, b, 1800);
            this.myAty = myAty;
        }
        final MyBluetoothLE myBluetooth;
        if ((myBluetooth = this.myBluetooth) != null) {
            myBluetooth.setOnIMBluetoothLEListener(this);
            this.myBluetooth.setOnWriteStateListener(this);
        }
        this.allReceivedCommand = new StringBuilder();
    }
    
    public static Uri getLogZip(final String s) {
        return XlogUtils.getLogZip(s);
    }
    
    public static void sendSupportMail(final String s, final String s2, final String s3) {
        XlogUtils.sendSupportMail(s, s2, s3);
    }
    
    private void disconnectWBOffice() {
        if (this.isSimulationMode) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("04", "");
        XlogUtils.xLog(WBOProtocol.TAG, "disconnectWBO\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, true, true, false);
    }
    
    private void handleReceived(String str) {
        final String s = str;
        XlogUtils.xLog(WBOProtocol.TAG, "handleReceived message : " + str);
        final Boolean value;
        if (value = (s.equals(this.oldCom) ^ true)) {
            this.oldCom = str;
        }
        final int cmd;
        final int n = cmd = this.getCmd(str);
        final String s2 = str;
        this.origin = Integer.parseInt(str.substring(2, 4), 16);
        final boolean equals = (str = s2.substring(10, s2.length() - 4)).equals("91");
        XlogUtils.xLog(WBOProtocol.TAG, "CMD : " + cmd + " data : " + str);
        Label_1027: {
            if (n != 0) {
                if (cmd != 1) {
                    if (cmd != 16) {
                        if (cmd != 19) {
                            switch (cmd) {
                                default: {
                                    switch (cmd) {
                                        default: {
                                            switch (cmd) {
                                                default: {
                                                    this.receiveError(str);
                                                    return;
                                                }
                                                case 40: {
                                                    this.isMeasurement = false;
                                                    final e e2;
                                                    final e e = e2 = new e(str);
                                                    e.a(2);
                                                    final int c = e.c(2);
                                                    final int c2 = e.c(2);
                                                    final boolean b = e.c(2) == 0;
                                                    final Boolean b2 = value;
                                                    final e e3 = e2;
                                                    e3.a(6);
                                                    final String a = e3.a(e3.a());
                                                    final CurrentAndMData currentAndMData = new com.microlife_sdk.model.data.CurrentAndMData();
                                                    final CurrentAndMData currentAndMData2 = currentAndMData;
                                                    final String s3 = a;
                                                    new CurrentAndMData();
                                                    currentAndMData.importHexString(s3, this.origin);
                                                    if (b2) {
                                                        this.onDataResponseListener.onResponseMeasurementResultsForEachMeasurement(currentAndMData2, c, c2, b);
                                                        break Label_1027;
                                                    }
                                                    break Label_1027;
                                                }
                                                case 39: {
                                                    final e e5;
                                                    final e e4 = e5 = new e(str);
                                                    STATUS status = STATUS.MeasurementWait;
                                                    final int c3;
                                                    if ((c3 = e4.c(2)) != 1) {
                                                        if (c3 != 2) {
                                                            if (c3 == 4) {
                                                                status = STATUS.MeasurementStop;
                                                            }
                                                        }
                                                        else {
                                                            status = STATUS.MeasurementStart;
                                                        }
                                                    }
                                                    else {
                                                        status = STATUS.MeasurementWait;
                                                    }
                                                    final boolean isMeasurement = status != STATUS.MeasurementStop;
                                                    final Boolean b3 = value;
                                                    final e e6 = e5;
                                                    this.isMeasurement = isMeasurement;
                                                    final int c4 = e6.c(2);
                                                    final int c5 = e6.c(2);
                                                    final int n2 = e6.c(2) * 256 + e5.c(2);
                                                    final int n3 = e6.c(2) * 256 + e5.c(2);
                                                    if (b3) {
                                                        this.onDataResponseListener.onResponseRemoteMeasurementStatusEvery5seconds(status, c4, c5, n2, n3);
                                                        break Label_1027;
                                                    }
                                                    break Label_1027;
                                                }
                                                case 38: {
                                                    this.onDataResponseListener.onResponseRemoteMeasurementStatusEvery5seconds(STATUS.MeasurementStop, 0, 0, 0, 0);
                                                    break Label_1027;
                                                }
                                                case 37: {
                                                    if (equals) {
                                                        break Label_1027;
                                                    }
                                                    CBPdataAndCalCBP.Dformat dformat = CBPdataAndCalCBP.Dformat.NoCBPRaw;
                                                    switch (new e(str).c(2)) {
                                                        case 3: {
                                                            dformat = CBPdataAndCalCBP.Dformat.FullCBPRaw;
                                                            break;
                                                        }
                                                        case 1:
                                                        case 2: {
                                                            dformat = CBPdataAndCalCBP.Dformat.LowCBPRaw;
                                                            break;
                                                        }
                                                        case 0: {
                                                            dformat = CBPdataAndCalCBP.Dformat.NoCBPRaw;
                                                            break;
                                                        }
                                                    }
                                                    if (value) {
                                                        this.onDataResponseListener.onResponseStartRemoteMeasurement(dformat);
                                                        break Label_1027;
                                                    }
                                                    break Label_1027;
                                                }
                                            }
                                        }
                                        case 13: {
                                            this.onDataResponseListener.onResponseWriteDeviceTime(equals ^ true);
                                            break Label_1027;
                                        }
                                        case 12: {
                                            final Boolean b4 = value;
                                            final DeviceInfo deviceInfo;
                                            com.microlife_sdk.model.abcdef.d.a(str, deviceInfo = new DeviceInfo());
                                            if (b4) {
                                                this.onDataResponseListener.onResponseReadDeviceTime(deviceInfo);
                                                break Label_1027;
                                            }
                                            break Label_1027;
                                        }
                                        case 11: {
                                            final Boolean b5 = value;
                                            final String s4 = str;
                                            final DeviceInfo deviceInfo2 = new com.microlife_sdk.model.data.DeviceInfo();
                                            final DeviceInfo deviceInfo3 = deviceInfo2;
                                            new DeviceInfo();
                                            com.microlife_sdk.model.abcdef.d.a(s4, deviceInfo2, this.origin);
                                            if (b5) {
                                                this.onDataResponseListener.onResponseReadDeviceInfo(deviceInfo3);
                                                break Label_1027;
                                            }
                                            break Label_1027;
                                        }
                                    }
                                }
                                case 7: {
                                    if (equals) {
                                        break;
                                    }
                                    final SettingValues settingValues = new com.microlife_sdk.model.data.SettingValues();
                                    final SettingValues settingValues2 = settingValues;
                                    final String s5 = str;
                                    new SettingValues();
                                    settingValues.importHexString(s5, this.origin);
                                    this.onDataResponseListener.onResponseReadSettingValues(settingValues2);
                                    break;
                                }
                                case 5: {
                                    final Boolean b6 = value;
                                    final e e8;
                                    final e e7 = e8 = new e(str);
                                    final User user = new com.microlife_sdk.model.data.User();
                                    final User user2 = user;
                                    final e e9 = e8;
                                    new User();
                                    user.setID(e9.b(60));
                                    final VersionData versionData;
                                    com.microlife_sdk.model.abcdef.d.b(e7.a(e7.a()), versionData = new VersionData());
                                    if (b6) {
                                        this.writeDeviceTime();
                                        this.onDataResponseListener.onResponseReadUserAndVersionData(user2, versionData);
                                        break;
                                    }
                                    break;
                                }
                                case 4: {
                                    break;
                                }
                                case 8: {
                                    this.onDataResponseListener.onResponseWriteSettingValues(equals ^ true);
                                    break;
                                }
                                case 6: {
                                    this.onDataResponseListener.onResponseWriteUserID(equals ^ true);
                                    break;
                                }
                                case 3: {
                                    this.onDataResponseListener.onResponseClearHistorys(equals ^ true);
                                    break;
                                }
                            }
                        }
                        else {
                            str = new e(str).b(str.length());
                            this.onDataResponseListener.onResponseReadBTModuleName(str);
                        }
                    }
                    else if (!equals) {
                        final FunctionSettingValues functionSettingValues = new com.microlife_sdk.model.data.FunctionSettingValues();
                        final FunctionSettingValues functionSettingValues2 = functionSettingValues;
                        final String s6 = str;
                        new FunctionSettingValues();
                        functionSettingValues.importHexString(s6, this.origin);
                        this.onDataResponseListener.onResponseReadFunctionSettingValues(functionSettingValues2);
                    }
                }
                else {
                    final boolean b7 = equals;
                    final CBPdataAndCalCBP cbPdataAndCalCBP = new CBPdataAndCalCBP();
                    if (!b7) {
                        cbPdataAndCalCBP.importHexString(str, this.origin);
                    }
                    this.onDataResponseListener.onResponseReadCBPData(cbPdataAndCalCBP, equals);
                }
            }
            else if (!equals) {
                final Boolean b8 = value;
                final String s7 = str;
                final DRecord dRecord = new com.microlife_sdk.model.data.DRecord();
                final DRecord dRecord2 = dRecord;
                new DRecord();
                com.microlife_sdk.model.abcdef.d.a(s7, dRecord, this.origin);
                if (b8) {
                    this.onDataResponseListener.onResponseReadAllHistorys(dRecord2);
                }
            }
        }
        this.receiveErrorCount = 0;
    }
    
    private boolean isCorrectHeader(final String s) {
        return s.startsWith("4D");
    }
    
    private boolean isCorrectEnd(final String s) {
        s.endsWith("-1");
        return true;
    }
    
    private int getCorrectLength(final String s) {
        int int1 = 0;
        if (s.length() > 8) {
            int1 = Integer.parseInt(s.substring(4, 8), 16);
        }
        return (int1 + 4) * 2;
    }
    
    private int getCmd(final String s) {
        return Integer.parseInt(s.substring(8, 10), 16);
    }
    
    private void receiveError(final String str) {
        XlogUtils.xLog(WBOProtocol.TAG, "Receive error Count = " + this.receiveErrorCount + " message : " + str);
        ++this.receiveErrorCount;
        int length = this.allReceivedCommand.length();
        if (this.receiveErrorCount > 15000) {
            this.receiveErrorCount = 0;
            this.allReceivedCommand.delete(0, length);
        }
        else {
            final int index = str.indexOf("-14D");
            final StringBuilder allReceivedCommand = this.allReceivedCommand;
            final int start = 0;
            if (index != -1) {
                length = index + 2;
            }
            allReceivedCommand.delete(start, length);
        }
        XlogUtils.xLog(WBOProtocol.TAG, "RECEIVED ERROR Delete message : " + (Object)this.allReceivedCommand);
    }
    
    public static /* synthetic */ Timer access$000(final WBOProtocol wboProtocol) {
        return wboProtocol.simulationTimer;
    }
    
    public static /* synthetic */ Timer access$002(final WBOProtocol wboProtocol, final Timer simulationTimer) {
        return wboProtocol.simulationTimer = simulationTimer;
    }
    
    public static /* synthetic */ Handler access$100(final WBOProtocol wboProtocol) {
        return wboProtocol.mHandler;
    }
    
    public static /* synthetic */ String access$200() {
        return WBOProtocol.TAG;
    }
    
    public static /* synthetic */ Activity access$300(final WBOProtocol wboProtocol) {
        return wboProtocol.myAty;
    }
    
    public static /* synthetic */ String access$400(final WBOProtocol wboProtocol) {
        return wboProtocol.bondMacAddress;
    }
    
    public void setOnWriteStateListener(final MyBluetoothLE.OnWriteStateListener mOnWriteStateListener) {
        this.mOnWriteStateListener = mOnWriteStateListener;
    }
    
    public void setOnNotifyStateListener(final OnNotifyStateListener mOnNotifyStateListener) {
        this.mOnNotifyStateListener = mOnNotifyStateListener;
    }
    
    public void setOnConnectStateListener(final OnConnectStateListener onConnectStateListener) {
        this.onConnectStateListener = onConnectStateListener;
    }
    
    public void setOnDataResponseListener(final OnDataResponseListener onDataResponseListener) {
        this.onDataResponseListener = onDataResponseListener;
    }
    
    public void readRSSI() {
        if (this.isSimulationMode) {
            this.cancelSimulationTimer();
            return;
        }
        this.myBluetooth.readRSSI();
    }
    
    public String getSDKVersion() {
        return Global.sdkVersion;
    }
    
    public boolean isSupportBluetooth(final Activity activity) {
        return this.isSimulationMode || this.myBluetooth.isSupportBluetooth(activity);
    }
    
    public boolean isEnableBt() {
        return this.isSimulationMode || this.myBluetooth.isBTEnabled();
    }
    
    public boolean isScanning() {
        return this.isSimulationMode || this.myBluetooth.isScanning();
    }
    
    public boolean isConnected() {
        return this.isSimulationMode || this.myBluetooth.isConnected();
    }
    
    public void startScan(final int n) {
        if (this.isSimulationMode) {
            this.startSimulationTimer();
            return;
        }
        this.myBluetooth.startLEScan(n, false);
    }
    
    public void stopScan() {
        if (this.isSimulationMode) {
            this.cancelSimulationTimer();
            return;
        }
        this.myBluetooth.stopLEScan();
    }
    
    public void bond(final String bondMacAddress) {
        this.myAty.registerReceiver(this.bondedBTReceiver, new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED"));
        final BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bondMacAddress);
        try {
            if (remoteDevice.getBondState() == 10) {
                final BluetoothDevice bluetoothDevice = remoteDevice;
                XlogUtils.xLog(WBOProtocol.TAG, "bondedBTReceiver BOND_NONE..");
                this.bondMacAddress = bondMacAddress;
                bluetoothDevice.createBond();
            }
            else {
                XlogUtils.xLog(WBOProtocol.TAG, "bondedBTReceiver BOND..");
                this.myAty.unregisterReceiver(this.bondedBTReceiver);
                this.connect(bondMacAddress);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            XlogUtils.xLog(WBOProtocol.TAG, "Exception Bond\uff1a" + ex.getMessage());
        }
    }
    
    public void connect(final String bondMacAddress) {
        if (this.isSimulationMode) {
            this.mHandler.removeMessages(0);
            this.cancelSimulationTimer();
            (this.simulationTimer = new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    WBOProtocol.access$100(WBOProtocol.this).sendEmptyMessage(101);
                }
            }, 1500L);
            return;
        }
        this.bondMacAddress = bondMacAddress;
        this.myBluetooth.sendCount = 0;
        MyBluetoothLE.MY_UUID_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
        MyBluetoothLE.MY_UUID_NOTIFY = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
        final ArrayList list = new ArrayList();
        final ArrayList list2 = list;
        new ArrayList();
        list.add(bondMacAddress);
        this.myBluetooth.connect(list2);
    }
    
    public void disconnect() {
        if (this.isSimulationMode) {
            this.onConnectStateListener.onConnectionState(ConnectState.Disconnect);
            return;
        }
        this.myBluetooth.disconnect(18);
    }
    
    @Override
    public void onBtStateChanged(final boolean b) {
        this.onConnectStateListener.onBtStateChanged(b);
    }
    
    @Override
    public void scanResult(final String s, final String s2, final int n, final byte[] array) {
        final Iterator<String> iterator = this.targetDeviceNames.iterator();
        while (iterator.hasNext()) {
            if (s2.startsWith(iterator.next())) {
                this.onConnectStateListener.onScanResult(s, s2, n);
            }
        }
    }
    
    @Override
    public void connectionStatus(final int i) {
        XlogUtils.xLog(WBOProtocol.TAG, "connectionStatus : " + i);
        if (i != 4) {
            if (i != 20) {
                if (i != 17) {
                    if (i == 18) {
                        this.mConnectState = ConnectState.Disconnect;
                    }
                }
                else {
                    this.mConnectState = ConnectState.Connected;
                    this.readUserAndVersionData();
                }
            }
            else {
                this.mConnectState = ConnectState.ConnectTimeout;
            }
        }
        else {
            this.mConnectState = ConnectState.ScanFinish;
        }
        this.onConnectStateListener.onConnectionState(this.mConnectState);
    }
    
    @Override
    public void onWriteMessage(final boolean arg1, final String obj) {
        final Message message = new Message();
        final Message message4;
        final Message message3;
        final Message message2 = message3 = (message4 = message);
        new Message();
        message3.what = 1000;
        message4.arg1 = (arg1 ? 1 : 0);
        message.obj = obj;
        this.mHandler.sendMessage(message2);
    }
    
    public void readAllHistorys() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("00", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readAllHistorys\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, true);
    }
    
    public void readCBPData(final int i, final CBPdataAndCalCBP.Dformat dformat) {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String s = "01";
        int n = 0;
        final int n2;
        if ((n2 = dformat.ordinal()) != 1) {
            if (n2 != 2) {
                if (n2 == 3) {
                    n = 3;
                }
            }
            else {
                n = 1;
            }
        }
        else {
            n = 0;
        }
        final Object[] args;
        final Object[] array = args = new Object[2];
        final int j = n;
        args[0] = i;
        array[1] = j;
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3(s, String.format("%04X%02X", args));
        XlogUtils.xLog(WBOProtocol.TAG, "readCBPData\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, true);
    }
    
    public void clearAllHistorys() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("03", "");
        XlogUtils.xLog(WBOProtocol.TAG, "clearAllHistorys\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, true);
    }
    
    public void disconnectWBO() {
        if (this.isMeasurement) {
            this.stopRemoteMeasurement();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    WBOProtocol.this.disconnectWBOffice();
                }
            }, 3000L);
        }
        else {
            this.disconnectWBOffice();
        }
    }
    
    public void readUserAndVersionData() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("05", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readUserAndVersionData\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void writeUserID(final String str) {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("06", new e(str).a(30, true));
        XlogUtils.xLog(WBOProtocol.TAG, "writeUserData origin\uff1a" + this.origin + " ID\uff1a" + str);
        XlogUtils.xLog(WBOProtocol.TAG, "writeUserData\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void readSettingValues() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("07", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readSettingValues\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void writeSettingValues(final SettingValues settingValues) {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String s = "08";
        final int n = (settingValues.isSW_AUTO_hide() ? 1 : 0) | (settingValues.isSW_SEL_silent() ? 1 : 0) << 1 | (settingValues.isSW_AUS_Hide() ? 1 : 0) << 2 | (settingValues.isSW_AVG_no_include_first() ? 1 : 0) << 3 | (settingValues.isSW_CBP() ? 1 : 0) << 4 | (settingValues.isSW_AFib() ? 1 : 0) << 5 | (settingValues.isSW_AMPM() ? 1 : 0) << 6 | (settingValues.isSW_Kpa() ? 1 : 0) << 7;
        final Object[] array = new Object[6];
        final Object[] array4;
        final Object[] array3;
        final Object[] array2;
        final Object[] args = array2 = (array3 = (array4 = array));
        final int i = n;
        final Object[] array5 = args;
        args[0] = settingValues.getAUS_HI_infPressure();
        array5[1] = settingValues.getHI_infPressure();
        array2[2] = i;
        array3[3] = settingValues.getRestTime();
        array4[4] = settingValues.getIntervalTime();
        array[5] = settingValues.getAutoMeasureNumber();
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3(s, String.format("%04X0000%04X%02X0000%04X%04X%02X00", args));
        XlogUtils.xLog(WBOProtocol.TAG, "writeSettingValues\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void readDeviceIDAndInfo() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("0B", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readDeviceIDAndInfo\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void readDeviceTime() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("0C", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readDeviceTime\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void writeDeviceTime() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final Calendar instance = Calendar.getInstance();
        final Object[] args;
        final Object[] array = args = new Object[6];
        final Calendar calendar = instance;
        final Object[] array2 = args;
        final Calendar calendar2 = instance;
        final Object[] array3 = args;
        final Calendar calendar3 = instance;
        final Object[] array4 = args;
        final Calendar calendar4 = instance;
        final Object[] array5 = args;
        final Calendar calendar5 = instance;
        args[0] = instance.get(1) % 100;
        array5[1] = calendar5.get(2) + 1;
        array4[2] = calendar4.get(5);
        array3[3] = calendar3.get(11);
        array2[4] = calendar2.get(12);
        array[5] = calendar.get(13);
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("0D", String.format("%02X%02X%02X%02X%02X%02X", args));
        XlogUtils.xLog(WBOProtocol.TAG, "writeDeviceTime\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void readFunctionSettingValue() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("10", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readSettingValues\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void readBTModuleName() {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("13", "");
        XlogUtils.xLog(WBOProtocol.TAG, "readBTModuleName\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, false, false, false);
    }
    
    public void startRemoteMeasurement(final CBPdataAndCalCBP.Dformat dformat) {
        if (this.isSimulationMode | this.isMeasurement) {
            return;
        }
        this.isMeasurement = true;
        final String s = "25";
        int i = 0;
        final int n;
        if ((n = dformat.ordinal()) != 1) {
            if (n != 2) {
                if (n == 3) {
                    i = 3;
                }
            }
            else {
                i = 1;
            }
        }
        else {
            i = 0;
        }
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3(s, String.format("%02X", i));
        XlogUtils.xLog(WBOProtocol.TAG, "startRemoteMeasurement\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, true, false, true);
    }
    
    public void stopRemoteMeasurement() {
        if (this.isSimulationMode) {
            return;
        }
        this.isMeasurement = false;
        final String buildCmdStringForWBO3 = this.myBluetooth.buildCmdStringForWBO3("26", "");
        XlogUtils.xLog(WBOProtocol.TAG, "stopRemoteMeasurement\uff1a" + buildCmdStringForWBO3);
        final StringBuilder allReceivedCommand = this.allReceivedCommand;
        allReceivedCommand.delete(0, allReceivedCommand.length());
        this.myBluetooth.writeBLWMessage(buildCmdStringForWBO3, true, true, false);
    }
    
    @Override
    public void dataResult(String s) {
        if (s.startsWith("Software_Revision_String")) {
            XlogUtils.xLog(WBOProtocol.TAG, "Software Revision String  -> " + s.split("==")[1]);
            return;
        }
        if ((s = s.toUpperCase(Locale.US)).contains("==")) {
            s = s.split("==")[1];
        }
        final Message message = new Message();
        final Message message2 = message;
        final String obj = s;
        final Message message3 = message2;
        new Message();
        message3.what = 1001;
        message.obj = obj;
        this.mHandler.sendMessage(message2);
        XlogUtils.xLog(WBOProtocol.TAG, "dataResult  -> " + s);
        try {
            this.allReceivedCommand.append(s);
            try {
                final String s2 = s = this.allReceivedCommand.toString();
                XlogUtils.xLog(WBOProtocol.TAG, "allReceivedCommand = " + s);
                final boolean correctHeader = this.isCorrectHeader(s2);
                final String s3 = s;
                final boolean correctEnd = this.isCorrectEnd(s);
                try {
                    final int correctLength = this.getCorrectLength(s3);
                    try {
                        XlogUtils.xLog(WBOProtocol.TAG, "headerCorrect : " + correctHeader + " endCorrect : " + correctEnd + " lengthCorrect : " + correctLength + " message.length : " + s.length());
                        Label_0679: {
                            if (!correctHeader || !correctEnd || s.length() < correctLength) {
                                break Label_0679;
                            }
                            this.receiveErrorCount = 0;
                            XlogUtils.xLog(WBOProtocol.TAG, " All received message -> " + s);
                            while (true) {
                                if (this.allReceivedCommand.length() == 0) {
                                    return;
                                }
                                final int correctLength2 = this.getCorrectLength(s = this.allReceivedCommand.toString());
                                try {
                                    final String s4 = s = this.allReceivedCommand.substring(0, correctLength2);
                                    final int n = correctLength2;
                                    XlogUtils.xLog(WBOProtocol.TAG, "Start parsing message -> " + s);
                                    XlogUtils.xLog(WBOProtocol.TAG, "Start parsing New lengthCorrect -> " + correctLength2);
                                    final int endIndex;
                                    final String substring;
                                    final String s5 = substring = s4.substring(endIndex = n - 4, correctLength2);
                                    final String substring2 = s.substring(8, 10);
                                    try {
                                        XlogUtils.xLog(WBOProtocol.TAG, "cmd = " + substring2);
                                        final String calcChecksum4 = this.myBluetooth.calcChecksum4(s.substring(0, 2), s.substring(2, 4), s.substring(4, 8), s.substring(8, 10), s.substring(10, endIndex));
                                        XlogUtils.xLog(WBOProtocol.TAG, "receiveChecksum = " + substring + " calcChecksum = " + calcChecksum4);
                                        Label_0644: {
                                            if (!s5.equals(calcChecksum4)) {
                                                break Label_0644;
                                            }
                                            int i = this.getCmd(s);
                                            int cmd = 0;
                                            Label_0604: {
                                                if (this.isSimulationMode || this.myBluetooth.getCommArraySize() <= 0) {
                                                    break Label_0604;
                                                }
                                                final String s6 = s;
                                                i = this.getCmd(this.myBluetooth.getComm(0));
                                                try {
                                                    cmd = this.getCmd(s6);
                                                    try {
                                                        XlogUtils.xLog(WBOProtocol.TAG, "writeCmd = " + i + " receiveCmd = " + cmd);
                                                        if (i == cmd) {
                                                            this.myBluetooth.sendCount = 0;
                                                            this.myBluetooth.removeComm(0);
                                                        }
                                                        final String s7 = s;
                                                        this.allReceivedCommand.delete(0, correctLength2);
                                                        try {
                                                            this.handleReceived(s7);
                                                            continue;
                                                        }
                                                        catch (Exception ex) {
                                                            ex.printStackTrace();
                                                            this.receiveError(s);
                                                        }
                                                    }
                                                    catch (Exception ex2) {}
                                                }
                                                catch (Exception ex3) {}
                                            }
                                        }
                                    }
                                    catch (Exception ex4) {}
                                }
                                catch (Exception ex5) {}
                                break;
                            }
                        }
                    }
                    catch (Exception ex6) {}
                }
                catch (Exception ex7) {}
            }
            catch (Exception ex8) {}
        }
        catch (Exception ex9) {}
        Label_0741:;
    }
    
    public enum STATUS
    {
        MeasurementWait, 
        MeasurementStart, 
        MeasurementStop;
    }
    
    public enum ConnectState
    {
        ScanFinish, 
        Connected, 
        Disconnect, 
        ConnectTimeout;
    }
    
    public interface OnDataResponseListener
    {
        void onResponseReadAllHistorys(final DRecord p0);
        
        void onResponseReadCBPData(final CBPdataAndCalCBP p0, final boolean p1);
        
        void onResponseClearHistorys(final boolean p0);
        
        void onResponseReadUserAndVersionData(final User p0, final VersionData p1);
        
        void onResponseWriteUserID(final boolean p0);
        
        void onResponseReadSettingValues(final SettingValues p0);
        
        void onResponseWriteSettingValues(final boolean p0);
        
        void onResponseReadDeviceInfo(final DeviceInfo p0);
        
        void onResponseReadDeviceTime(final DeviceInfo p0);
        
        void onResponseWriteDeviceTime(final boolean p0);
        
        void onResponseReadFunctionSettingValues(final FunctionSettingValues p0);
        
        void onResponseReadBTModuleName(final String p0);
        
        void onResponseStartRemoteMeasurement(final CBPdataAndCalCBP.Dformat p0);
        
        void onResponseRemoteMeasurementStatusEvery5seconds(final STATUS p0, final int p1, final int p2, final int p3, final int p4);
        
        void onResponseMeasurementResultsForEachMeasurement(final CurrentAndMData p0, final int p1, final int p2, final boolean p3);
    }
    
    public interface OnConnectStateListener
    {
        void onBtStateChanged(final boolean p0);
        
        void onScanResult(final String p0, final String p1, final int p2);
        
        void onConnectionState(final ConnectState p0);
    }
    
    public interface OnNotifyStateListener
    {
        void onNotifyMessage(final String p0);
    }
}

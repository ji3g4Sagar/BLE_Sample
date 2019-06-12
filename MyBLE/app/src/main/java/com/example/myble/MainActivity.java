package com.example.myble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 3000; //3 seconds
    private static final String TAG = "Yuan tag";
    public TextView systolic, diastolic, alldata_value, glucose, time;
    public ArrayList<String> deviceList = new ArrayList<String>();
    public Button pre, glu, synchronize;
    public String Alldata=" ";
    public final static String DEVICE_NAME_FORA = "FORA";  // 定翼裝置名稱，利用裝置名稱過濾不必要的藍牙設備。
    public final static UUID FORA_SERVICE_UUID = UUID.fromString("FORA廠商提供代碼");
    public final static UUID FORA_CHARACTERISTIC_UUID = UUID.fromString("FORA廠商提供代碼");
    public final static UUID Client_Characteristic_Configuration = convertFromInteger(0x2902);
    public String userDataType = "-1";   // 設定使用者想要取得的資料屬於血糖還是血壓
                                         // 0：血糖; 1：血壓




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        bluetoothManager = (BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 以下兩個if，檢查手機硬體是否為BLE裝置ˇ
        if (!getPackageManager().hasSystemFeature
                (PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "硬體不支援", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 檢查手機是否開啟藍芽裝置
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "請開啟藍芽裝置", Toast.LENGTH_SHORT).show();
            Intent enableBluetooth = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        systolic = (TextView) findViewById(R.id.systolic_value);
        diastolic = (TextView) findViewById(R.id.diastolic_value);
        glucose = (TextView) findViewById(R.id.glucose_value);
        alldata_value = (TextView) findViewById(R.id.All_data);
        time = (TextView)findViewById(R.id.time_value);
        pre = (Button) findViewById(R.id.PRE);
        glu = (Button) findViewById(R.id.GLU);
        synchronize = (Button) findViewById(R.id.mysynchronize);
        pre.setOnClickListener(btn);
        synchronize.setOnClickListener(btn);
        glu.setOnClickListener(btn);
    }

    private View.OnClickListener btn = new View.OnClickListener() {
        public void onClick(View v) {

            switch (v.getId()) {
                case (R.id.GLU):                   // 這邊對應到 血糖
                    scanLeDevice(true);     // 呼叫scanLeDevice()，開始掃描
                    glu.setEnabled(false);
                    pre.setEnabled(false);
                    userDataType = "0";            //在血糖燈裡面設定使用者要取得的資料種類
                                                  // userDataType:0 表示 血糖 
                    break;
                case (R.id.PRE):                   //這邊對應到血壓
                    scanLeDevice(true);    // 呼叫scanLeDevice()，開始掃描
                    glu.setEnabled(false);
                    pre.setEnabled(false);
                    userDataType = "1";           //在血壓燈裡面設定使用者要取得的資料種類
                                                  // userDataType:1 表示 血壓 
                    break;
                case (R.id.mysynchronize):
                    Log.d(TAG, "in synchronize");
                    alldata_value.setText(Alldata);
                    systolic.setText(" ");
                    diastolic.setText(" ");
                    time.setText(" ");
                    glucose.setText(" ");
                    pre.setEnabled(true);
                    glu.setEnabled(true);
                    break;
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() { //利用SCAN_PERIOD設定裝置掃描的時間，掃描時間到就開始對掃描到的裝置連線
                @Override
                public void run() {  
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); // 開始連線後利用stopLeScan，停止手機掃描週邊裝置
                    ExecutorService executor = Executors.newFixedThreadPool(5); 
                    for (int i=0;i<deviceList.size();i++){
                        final BluetoothDevice tempdevice = mBluetoothAdapter.getRemoteDevice(deviceList.get(i)); // 利用deviceList中存放的 mac address 建立BluetoothDevice物件

                        ConnThread t = new ConnThread(tempdevice, mGattCallback, getApplicationContext());
                         //為每一個deviceList中儲存的BluetoothDevice物件建立一條thread，各別去做connectGatt的動作，
                        Thread runningThread = new Thread(t);
                        executor.execute(runningThread);
                    }
                    executor.shutdown(); //表示executor不再接收新的job
                    while(!executor.isTerminated()){ //等待每一條連線的thread 執行完成
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Waitting for other thread!");
                    }
                }
            }, SCAN_PERIOD);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {//開始掃描後的callback，被connectGattt觸發。
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if(device.getName()!=null && !(deviceList.contains(device.getAddress()))){
                if(device.getName().contains(DEVICE_NAME_FORA)){
                    Log.d(TAG,"BLE device : " + device.getName());
                    deviceList.add(device.getAddress());
                    // 把所有掃描到的裝置存放在 deviceList這個ArrayList中，
                }
            }
        }
    };
    public final MyBluetoothGattCallback mGattCallback = new MyBluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Log.d(TAG, "status: "+String.valueOf(status)+"  newState: "+String.valueOf(newState));
            if(newState == BluetoothProfile.STATE_CONNECTED){ //連線中
                Log.d(TAG, "State to connect");
                gatt.discoverServices(); //觸發onServicesDiscovered()
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){ //斷線
                Log.d(TAG, "State to disconnect");
                this.dataAvailable = false;
            }
            else if ((status == 8 && newState == 0) || (status == 133 && newState == 0)) { //連線上有問題的地方 8代表斷開; 133是安卓的bug，需要另外處理
                gatt.disconnect();
                gatt.close();
                gatt.getDevice().connectGatt(getApplicationContext(), false, mGattCallback);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {  // 被gatt.discoverService()觸發，有發現新的services
            try {  // 用sleep避免 service找不的bug
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Find service");

                displayGattServices(gatt.getServices(), gatt);
            }
            else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, String.valueOf(status));
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "Write the descriptor successfully");
                BluetoothGattCharacteristic Char = gatt.getService(FORA_SERVICE_UUID).getCharacteristic(FORA_CHARACTERISTIC_UUID);
                byte [] arrayOfByte = new byte[8];

                if(this.hasGetDataNum == false){
                    arrayOfByte [0] = (byte) 0x51;  // 起始信號
                    arrayOfByte [1] = (byte) 0x2B;  // 取得資料筆數代碼
                    arrayOfByte [2] = (byte) 0x01;  // 表示第幾個使用者
                    arrayOfByte [3] = (byte) 0x00;  // 對0x2B來說，剩下index 無意義，給0x00即可
                    arrayOfByte [4] = (byte) 0x00;  //
                    arrayOfByte [5] = (byte) 0x00;  //
                    arrayOfByte [6] = (byte) 0xA3;  // 結束信號
                }
                else if(this.dataNotExist == true){
                    arrayOfByte [0] = (byte) 0x51;  // 起始信號
                    arrayOfByte [1] = (byte) 0x50;  // 關機 代碼
                    arrayOfByte [2] = (byte) 0x00;  // 對0x50來說 剩餘index 給0x00即可
                    arrayOfByte [3] = (byte) 0x00;  //
                    arrayOfByte [4] = (byte) 0x00;  //
                    arrayOfByte [5] = (byte) 0x00;  //
                    arrayOfByte [6] = (byte) 0xA3;  // 結束信號
                    Log.d(TAG, "找不到對應資料！！！！ 裝置即將關機");
                }
                else if(this.dataAvailable == false){
                    arrayOfByte [0] = (byte) 0x51;  // 起始信號
                    arrayOfByte [1] = (byte) 0x25;  // 用來判斷資料是血壓還是血糖
                    arrayOfByte [2] = this.whichdataIndex;  // 2、3為表示為0 表示取最後一筆
                    arrayOfByte [3] = (byte) 0x00;  // 同上
                    arrayOfByte [4] = (byte) 0x00;  // 對0x25來說無意義，給0x00即可
                    arrayOfByte [5] = (byte) 0x01;  // 0x1對 0x26來說表示取使用者1的資料
                    arrayOfByte [6] = (byte) 0xA3;  // 結束信號

                }
                else if(dataAvailable == true){
                    arrayOfByte [0] = (byte) 0x51;  // 起始信號
                    arrayOfByte [1] = (byte) 0x26;  // 血糖、血壓數值代碼
                    arrayOfByte [2] = this.whichdataIndex;  // 2、3為表示為0 表示取最後一筆
                    arrayOfByte [3] = (byte) 0x00;  // 同上
                    arrayOfByte [4] = (byte) 0x00;  // 對0x26來說無意義，給0即可
                    arrayOfByte [5] = (byte) 0x01;  // 0x1對 0x26來說表示 取使用者1的資料
                    arrayOfByte [6] = (byte) 0xA3;  // 結束信號
                }
                // byte[7] 為check sum
                arrayOfByte [7] =
                        ((byte)(arrayOfByte[0] + arrayOfByte[1] +
                                arrayOfByte[2] + arrayOfByte[3] +
                                arrayOfByte[4] + arrayOfByte[5] +
                                arrayOfByte[6]& 0xFF ));
                Char.setValue(arrayOfByte);
                boolean result = gatt.writeCharacteristic(Char);
                //boolean result = mBluetoothGatt.writeCharacteristic(Char);
                Log.d(TAG, "UUID"+Char.getUuid().toString());
                Log.d(TAG, "Char write result: "+String.valueOf(result));
            }
            else {
                Log.d(TAG, "Write the descriptor failed!!");
            }
        }
        //
        // onCharacteristicChanged()
        // Triggered when a GATT Characteristic’s data has changed
        // 上面是官方文件寫的
        //
        //利用descriptor.setvalue設定Client characteristic的indicate屬性為true後會觸發這裡
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "in characteristicChange");
            gatt.setCharacteristicNotification(characteristic, true);
            byte[] data = characteristic.getValue();
            Log.d(TAG, String.valueOf(data[0]&0xFF));
            Log.d(TAG, String.valueOf(data[1]&0xFF));
            Log.d(TAG, String.valueOf(data[2]&0xFF));
            Log.d(TAG, String.valueOf(data[3]&0xFF));
            Log.d(TAG, String.valueOf(data[4]&0xFF));
            Log.d(TAG, String.valueOf(data[5]&0xFF));
            Log.d(TAG, String.valueOf(data[6]&0xFF));
            Log.d(TAG, String.valueOf(data[7]&0xFF));
            if(this.hasGetDataNum == false){
                int Num = data[2]& 0xFF;
                Log.d(TAG, "NUMBER"+String.valueOf(Num));
                if(Num==0){
                    this.dataNotExist = true;
                }
                else{
                    this.hasGetDataNum = true;
                    this.dataNum = Num;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Client_Characteristic_Configuration);
                gatt.writeDescriptor(descriptor);
            }
            else if(this.dataAvailable == false){
                Log.d(TAG, String.valueOf(data[1])+ String.valueOf(data[3]));
                Log.d(TAG, "dataNUm: "+String.valueOf(this.dataNum));
                if(this.getDataTime <= this.dataNum){
                    String dataForDataTypeAndMinute =
                            addZero(Integer.toBinaryString(Integer.parseInt(Integer.toHexString(data[4]&0xFF), 16)), 8);
                    String dataForHour =
                            addZero(Integer.toBinaryString(Integer.parseInt(Integer.toHexString(data[5]&0xFF), 16)), 8);
                    this.dataType = dataForDataTypeAndMinute.substring(0,1);
                    this.Minute = Integer.valueOf(dataForDataTypeAndMinute.substring(2,8),2).toString();
                    this.Hour = Integer.valueOf(dataForHour.substring(3,8),2).toString();
                    Log.d(TAG, "Hour"+this.Hour);
                    Log.d(TAG, "Minute: "+this.Minute);
                    Log.d(TAG, "First bit: "+this.dataType);  // 0: 血糖; 1:血壓.
                    Log.d(TAG, "Userdatatype: "+userDataType); //userDataType 表示使用者要求的資料格式

                    if(userDataType.equals(this.dataType)){
                        this.dataAvailable = true;
                    }
                    else{
                        this.whichdataIndex = (byte) (this.whichdataIndex +1) ;
                        this.getDataTime ++;
                    }
                    Log.d(TAG, "INDEX");
                    System.out.println(this.whichdataIndex);
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Client_Characteristic_Configuration);
                gatt.writeDescriptor(descriptor);
            }
            else if(this.dataAvailable == true){                                                //取值在這邊取！
                if(userDataType.equals("0")){                                                   // 這個if 取得血糖！
                    final int Glucose = data[2] & 0xFF;
                    this.Glucose = data[2] &0xFF;
                    Log.d(TAG, "Glucose: "+String.valueOf(Glucose));
                    Alldata = Alldata + "[" + String.valueOf(Glucose) + "]";
                    glucose.append(String.valueOf(Glucose));
                    time.append(this.Hour+" : "+this.Minute);
                }
                else if(userDataType.equals("1") ){                                             //這邊 取得血壓！！
                    final int Systolic = data[2]& 0xFF;  // & 0xff 避免數值超過128 時變成 二補數的bug  例如 129 變成 -127
                    final int Diastolic = data[4]& 0xFF;
                    this.Systolic = data[2]& 0xFF;
                    this.Diastolic = data[4]&0xFF;
                    Log.d(TAG, "Systolic: " + String.valueOf(Systolic));  //Systolic
                    Log.d(TAG, "Diastolic: " + String.valueOf(Diastolic));  //Diastolic
                    Alldata = Alldata+ "["+String.valueOf(Systolic)+", "+String.valueOf(Diastolic)+"] ";
                    systolic.append(String.valueOf(Systolic));
                    diastolic.append(String.valueOf(Diastolic));
                    time.append(this.Hour+" : "+this.Minute);
                }
                disconnection(gatt, this);
            }
            else{
                Log.d(TAG, "找不到對應資料！！！！");
            }
        }
    };
    public static void displayGattServices(List<BluetoothGattService> gattServices, BluetoothGatt gatt) {
        Log.d(TAG, "in displayGattServices");
        if (gattServices == null){
            Log.d(TAG, "gattService null");
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            if(gattService.getUuid().equals(FORA_SERVICE_UUID)){
                Log.d(TAG, "Service uuid: "+ gattService.getUuid());
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    gatt.setCharacteristicNotification(gattCharacteristic, true);
                    if(FORA_CHARACTERISTIC_UUID.equals(gattCharacteristic.getUuid())){
                        Log.d(TAG, "Char uuid :"+ gattCharacteristic.getUuid());
                        //BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptors().get(0);
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(Client_Characteristic_Configuration);
                        Log.d(TAG, "!!!!DES UUID: "+descriptor.getUuid().toString());
                        if ((gattCharacteristic.PROPERTY_NOTIFY)> 0 ){
                            if (descriptor != null) {
                                Log.d(TAG, "notify > 0");
                                descriptor.setValue((BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                            }
                        }
                        else if((gattCharacteristic.PROPERTY_INDICATE) >0 ){
                            Log.d(TAG, "Indicate > 0");
                            if(descriptor != null){
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                        }
                        Boolean result = gatt.writeDescriptor(descriptor);
                        Log.d(TAG, "Write descriptor indication result: "+result.toString());
                    }
                }
            }
        }
    }
    public void disconnection(BluetoothGatt gatt, MyBluetoothGattCallback obj){
        gatt.disconnect();

        Log.d(TAG, "in disconnection!!!!");
        obj.dataAvailable = false;
        obj.Minute="";
        obj.Hour = "";
        obj.dataNotExist = false;
        obj.hasGetDataNum = false;
        obj.dataAvailable = false;
        obj.whichdataIndex = (byte)0x00;
        obj.Glucose=-1;
        obj.Diastolic=-1;
        obj.Systolic=-1;
        obj.dataType="-1";
        obj.dataNum = -1;
        obj.getDataTime = 0;

        deviceList.clear();
        if(deviceList.isEmpty()){
            Log.d(TAG, "deviceList is empty");
        }
        else{
            Log.d(TAG, "deviceList has something!");
        }
        gatt.close();
    }
    public static UUID convertFromInteger(int i) { //用來把ing轉譯成UUID
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
    public static String addZero(String paramString, int paramInt) {  //處理接回來的 byteArray 位數不足（不足8bit）的bug （接回來的型態為String)
        if (paramString.length() > paramInt) {
            return paramString;     //表示收到回來的值，MSB為1，不需要另外處理。
        }
        int j = paramString.length();
        String str = "";
        int i = 0;
        //表示MSB不為零，需要把最高位全部補0
        //例如 回傳值是10001 就會補成 00010001
        while (i < paramInt - j)
        {
            str = str + "0";
            i += 1;
        }
        return str + paramString;
    }
}
class MyBluetoothGattCallback extends BluetoothGattCallback {  //覆寫 BluetoothGattCallback，加上 dataType 和 dataAvailable這兩個member variable。
    public String dataType = "-1"; // -1: 沒有任何狀態; 0: 血糖; 1: 血壓。
    public String Minute = "", Hour = "";
    public byte whichdataIndex = (byte)0x00;
    public boolean dataAvailable = false, hasGetDataNum = false, dataNotExist = false;
    public int dataNum = -1, getDataTime = 0,Systolic=-1, Diastolic=-1, Glucose=-1;
}
class ConnThread implements Runnable {
    private BluetoothDevice innerDevice;
    private MyBluetoothGattCallback innerCallBack;
    private Context innerContext;
    ConnThread(BluetoothDevice device, MyBluetoothGattCallback callback, Context context){
        this.innerDevice = device;
        this.innerCallBack = callback;
        this.innerContext = context;
    }
    public void run(){

        this.innerDevice.connectGatt(innerContext, false, innerCallBack);
    }
}


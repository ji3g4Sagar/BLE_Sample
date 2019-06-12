# 專案介紹
## 以Fora的血壓血糖二合機(D40)為例，基於BLE連線，取出測量數值的範例程式。

# 專案建立流程
1. clone 此專案
	* git clone https://github.com/ji3g4Sagar/BLE_Sample.git
2. 利用Android studio 開啟 MyBLE資料夾
3. 確認專案開啟
  * 使用介面
  <img src="https://github.com/ji3g4Sagar/BLE_Sample/blob/master/UI.png" width="20%" height="20%">

  * 介面介紹
	1. 血壓按鈕，針對二合一血壓機，欲取得「血壓」測量數值。
	2. 血糖按鈕，針對二合一血壓機，欲取得「血糖」測量數值。
	3. 同步資料按鈕，取值後，將資料串成json，並列在最下方的textview中，同時初始化所有變數，準備下一次連線取值。
	4. Systolic，收縮壓
	5. Diastolic，舒張壓
	6. Glucose，血糖值
	7. Time，資料時間標記

# 程式說明Part1
## 裝置掃描到裝置連線

1. 檢測手機硬體
* 原始碼位置： onCreate()
* 原始碼：
```

// 檢查裝置是否支援BLE
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

```
2. UI元件綁定
* 原始碼位置： onCreate()
* 原始碼：
```

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

```

3. 呼叫掃描功能以及後續連線
* 原始碼位置： scanLeDevice(final boolean enable)
* 原始碼：
```

if (enable) {
    mHandler.postDelayed(new Runnable() { //利用SCAN_PERIOD設定裝置掃描的時間，掃描結束後對掃描得到的裝置進行連線
        @Override
        public void run() {  
            mBluetoothAdapter.stopLeScan(mLeScanCallback); // 開始連線後利用stopLeScan，停止掃描週邊裝置
            ExecutorService executor = Executors.newFixedThreadPool(5);  //建立工作pool
            for (int i=0;i<deviceList.size();i++){
           		// 利用deviceList中存放的 mac address 建立BluetoothDevice物件
                final BluetoothDevice tempdevice = mBluetoothAdapter.getRemoteDevice(deviceList.get(i)); 
                //為每一個deviceList中儲存的BluetoothDevice物件建立一條thread，各別去做connectGatt的動作
                ConnThread t = new ConnThread(tempdevice, mGattCallback, getApplicationContext());
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
    mBluetoothAdapter.startLeScan(mLeScanCallback); // 上面的postDelayed() 在SCAN_PERIOD 的秒數到之前，優先執行這行，掃描週邊裝置
}
else {
    mBluetoothAdapter.stopLeScan(mLeScanCallback);
}

```

4. 掃描週邊裝置
* 原始碼位置： mLeScanCallback
* 原始碼：
```

if(device.getName()!=null && !(deviceList.contains(device.getAddress()))){  //檢查掃描到的裝置名稱是否為空以及裝置是否已經是否已經儲存過
    if(device.getName().contains(DEVICE_NAME_FORA)){   // 利用裝置名稱，過濾非FORA的裝置
        Log.d(TAG,"BLE device : " + device.getName());
        deviceList.add(device.getAddress()); // 把所有掃描到的裝置存放在 deviceList這個ArrayList中
    }
}

```

***

# 程式說明Part2
## 連線後取測量值步驟

* 邏輯：覆寫藍牙中的callbakc function，在對應的callback function中，設計要執行的功能。

1. 連線後藍芽CallBack事件

	i. onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	* 觸發時機： 每當有任何裝置連線狀態改變。
	* 參數意義：
		+ gatt: 藍芽連線物件
		+ status: Function 執行是否成功，[注意：0表示成功]
		+ newState: 連線狀態

	*原始碼：
	```

    if(newState == BluetoothProfile.STATE_CONNECTED){ //連線中
        gatt.discoverServices(); //觸發onServicesDiscovered()
    }
    else if(newState == BluetoothProfile.STATE_DISCONNECTED){ //斷線
        this.dataAvailable = false;
    }
    else if ((status == 8 && newState == 0) || (status == 133 && newState == 0)) { //連線上有問題的地方 8代表斷開; 133是安卓的bug，需要另外處理
        gatt.disconnect();
        gatt.close();
        gatt.getDevice().connectGatt(getApplicationContext(), false, mGattCallback);
    }

	```

	ii. onServicesDiscovered(BluetoothGatt gatt, int status)
	* 觸發時機： 每當有任何藍牙物件(gatt)呼叫discoverServices()
	* 參數意義：
		+ gatt: 藍芽連線物件
		+ status: Function執行是否成功，[注意：0表示成功]

	*原始碼：
	```

	try {  // 用sleep避免找不的service的 bug
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

    ```

    iii. onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    * 觸發時機： 每當有任何的gatt.descriptor的物件被寫入 
    例如這句：gatt.writeDescriptor(descriptor);
	* 參數意義：
		+ gatt: 藍芽連線物件
		+ descriptor：藍芽descriptor物件，可以在這個callback中做對應操作
		+ status: Function執行是否成功，[注意：0表示成功]

	*原始碼：
	``` 

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

    ````



















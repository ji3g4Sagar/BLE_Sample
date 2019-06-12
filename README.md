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

# 程式說明
1. UI元件綁定、檢測手機硬體
* 程式位置： onCreate()
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



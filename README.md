# 專案介紹
## 以Fora的血壓血糖二合機(D40)為例，基於BLE連線，取出測量數值的範例程式。

# 專案建立流程
1. clone 此專案
	* git clone https://github.com/ji3g4Sagar/BLE_Sample.git
2. 利用Android studio 開啟 MyBLE資料夾
3. 確認專案開啟
  * 使用介面
  <img src="https://github.com/ji3g4Sagar/BLE_Sample/blob/master/UI.png" width="20%" height="20%">

  * 介面功能介紹
  	1. 血壓按鈕，針對二合一血壓機，欲取得「血壓」測量數值。
  	2. 血糖按鈕，針對二合一血壓機，欲取得「血糖」測量數值。
  	3. 同步資料按鈕，取值後，將資料串成json，並列在最下方的textview中，同時初始化所有變數，準備下一次連線取值。
  	4. Systolic，收縮壓
  	5. Diastolic，舒張壓
  	6. Glucose，血糖值
  	7. Time，資料時間標記

# 程式說明


package net.flyget.bluetoothhelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private String number1 = "__", number2 = "__", number3 = "__";// 三个数字

	private static final int REQUEST_ENABLE_BT = 0;
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	private static final String TAG = "MainActivity";
	private BluetoothAdapter mBluetoothAdapter;
	private ConnectThread mConnectThread;
	public ConnectedThread mConnectedThread;
	private TextView mScanBtn;
	private TextView mTextView1, mTextView2, mTextView3, datatimeTv;
	private static final int MSG_NEW_DATA = 3;
	private String mTitle;
	private String dateFormat = "yyyy年MM月dd日 HH:mm:ss";
	private TimerTask task;
	private Timer timer = new Timer();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Set up the window layout
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		mTitle = "Bluetooth Debug Helper";
		setTitle(mTitle);

		mScanBtn = (TextView) findViewById(R.id.scanBtn);
		mScanBtn.setOnClickListener(this);

		mTextView1 = (TextView) findViewById(R.id.mTextView1);
		mTextView2 = (TextView) findViewById(R.id.mTextView2);
		mTextView3 = (TextView) findViewById(R.id.mTextView3);
		datatimeTv = (TextView) findViewById(R.id.datetime);
		datatimeTv.setText(new SimpleDateFormat(dateFormat).format(new Date()));
		task = new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(1);
			}
		};
		timer.schedule(task, 200, 200);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.custom_title);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			datatimeTv.setText(new SimpleDateFormat(dateFormat).format(new Date()));
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		}

	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_NEW_DATA:
				int tes = getInt(number1);
				if (tes >= 30) {
					mTextView1.setTextColor(0xffff0000);
				} else {
					mTextView1.setTextColor(0xff000000);
				}
				mTextView1.setText("温度:" + number1 + "℃");
				tes = getInt(number2);
				if (tes >= 85) {
					mTextView2.setTextColor(0xffff0000);
				} else {
					mTextView2.setTextColor(0xff000000);
				}
				mTextView2.setText("湿度:" + number2 + "%");
				tes = getInt(number3);
				if (tes >= 30) {
					mTextView3.setTextColor(0xffff0000);
				} else {
					mTextView3.setTextColor(0xff000000);
				}
				mTextView3.setText("烟雾浓度:" + number3 + "%");
				break;
			default:
				break;
			}
		}

	};

	public int getInt(String s) {
		String tem = s;
		if (s.indexOf(".") != -1) {
			tem = s.substring(0, s.indexOf("."));
		}
		int ret = Integer.parseInt(tem);
		return ret;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled Launch the DeviceListActivity to see
				// devices and do scan
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "蓝牙不可用");
				Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
				return;
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (resultCode != Activity.RESULT_OK) {
				return;
			} else {
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				// Attempt to connect to the device
				connect(device);
			}
			break;
		}
	}

	public void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);
		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {

				Log.e(TAG, "unable to connect() socket", e);
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				return;
			}

			mConnectThread = null;

			// Start the connected thread
			// Start the thread to manage the connection and perform
			// transmissions
			mConnectedThread = new ConnectedThread(mmSocket);
			mConnectedThread.start();

		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			setDaemon(true);
			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			int bytes;
			byte[] mbuf = new byte[1];
			// Keep listening to the InputStream while connected
			while (true) {
				try {
					boolean hasdata = false;
					List<Byte> data = new LinkedList<Byte>();
					while (true) {
						bytes = mmInStream.read(mbuf);
						if (bytes == 1) {
							if (mbuf[0] == (byte) '*') {
								if (hasdata) {// 无数据时不退出
									hasdata = false;
									break;
								} else {
									continue;
								}
							} else {
								hasdata = true;
								for (Byte byte1 : mbuf) {
									data.add(byte1);
								}
							}
						} else if (bytes == -1) {
							break;
						}
					}
					StringBuffer buil = new StringBuffer();
					for (Byte byte1 : data) {
						char tem = (char) (byte) byte1;
						if (tem == 13 || tem == ' ' || tem == '*' || tem == '\t' || tem == '\n') {
							continue;
						}
						buil.append(tem);
					}
					String A1 = getIndexStr(buil, "p:", "C");
					if (A1 != null) {
						number1 = A1;
						A1 = getIndexStr(buil, "i:", "%");
						if (A1 != null) {
							number2 = A1;
							A1 = getIndexStr(buil, "2:", "%B");
							if (A1 != null) {
								number3 = A1;
								mHandler.sendEmptyMessage(MSG_NEW_DATA);
							}
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					break;
				}
			}
			cancel();
		}

		public String getIndexStr(StringBuffer sou, String st, String en) {
			int s1 = sou.indexOf(st);
			int e1 = sou.indexOf(en);
			if (s1 != -1 && e1 != -1) {
				String A1 = sou.substring(s1 + 2, e1);
				return A1;
			} else
				return null;
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private boolean isBackCliecked = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isBackCliecked) {
				this.finish();
			} else {
				isBackCliecked = true;
				Toast t = Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_LONG);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
			}
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		isBackCliecked = false;
		if (v == mScanBtn) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
			// if(mConnectedThread != null) {mConnectedThread.cancel();
			// mConnectedThread = null;}
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		timer.cancel();
	}
}

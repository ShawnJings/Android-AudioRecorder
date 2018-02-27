package com.phuket.tour.audiorecorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.phuket.tour.audiorecorder.recorder.AudioConfigurationException;
import com.phuket.tour.audiorecorder.recorder.AudioRecordRecorderService;
import com.phuket.tour.audiorecorder.recorder.StartRecordingException;
import com.phuket.tour.audiorecorder.utils.LogUtils;
import com.phuket.tour.audiorecorder.utils.SDCardUtils;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
/**
 * 点击record开始录音，点击stop停止录音
 * 利用adb pull 导出PCM文件
 * 		adb pull /mnt/sdcard/vocal.pcm ~/Desktop/
 * 利用ffplay播放声音
 * 		ffplay -f s16le  -sample_rate 44100  -channels 1 -i ~/Desktop/vocal.pcm
 * 利用ffmpeg将PCM文件转换为WAV文件
 * 		ffmpeg -f s16le  -sample_rate 44100  -channels 1 -i ~/Desktop/vocal.pcm -acodec pcm_s16le ~/Desktop/ssss.wav
 */
public class AudioRecorderActivity extends Activity {

	private TextView recorder_time_tip;
	private Button recorder_btn;
	private static final int DISPLAY_RECORDING_TIME_FLAG = 100000;
	private int record = R.string.record;
	private int stop = R.string.stop;

	private boolean isRecording = false;
	private AudioRecordRecorderService recorderService;
	private String outputPath = "/mnt/sdcard/vocal.pcm";
	private Timer timer;
	private int recordingTimeInSecs = 0;
	private TimerTask displayRecordingTimeTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		findView();
		initView();
		bindListener();
	}

	private void findView() {
		recorder_time_tip = (TextView) findViewById(R.id.recorder_time_tip);
		recorder_btn = (Button) findViewById(R.id.recorder_btn);
	}

	private void initView() {
		String timeTip = "00:00";
		recorder_time_tip.setText(timeTip);
	}

	private void bindListener() {
		recorder_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					isRecording = false;
					recorder_btn.setText(getString(record));
					recordingTimeInSecs = 0;
					recorderService.stop();
					mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
					displayRecordingTimeTask.cancel();
					timer.cancel();
				} else {
					permissionCheck();
				}
			}
		});
	}

	private void beginRecord() {

		String sdTempPath = SDCardUtils.getExternalSdCardPath(this) + File.separator + "1RecordAudio";
		File sdTempFile = new File(sdTempPath);
		if (!sdTempFile.exists()) {
			sdTempFile.mkdir();
		}

		outputPath = new File(sdTempFile, new Date().getTime() + ".pcm").getAbsolutePath();

		isRecording = true;
		recorder_btn.setText(getString(stop));
		//启动AudioRecorder来录音
		recorderService = AudioRecordRecorderService.getInstance();
		try {
			recorderService.initMetaData();
			recorderService.start(outputPath);
			//启动一个定时器来监测时间
			recordingTimeInSecs = 0;
			timer = new Timer();
			displayRecordingTimeTask = new TimerTask() {
				@Override
				public void run() {
					mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
					recordingTimeInSecs++;
				}
			};
			timer.schedule(displayRecordingTimeTask, 0, 1000);
		} catch (AudioConfigurationException e) {
			Toast.makeText(AudioRecorderActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (StartRecordingException e) {
			Toast.makeText(AudioRecorderActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DISPLAY_RECORDING_TIME_FLAG:
				int minutes = recordingTimeInSecs / 60;
				int seconds = recordingTimeInSecs % 60;
				String timeTip = String.format("%02d:%02d", minutes, seconds);
				recorder_time_tip.setText(timeTip);
				break;
			default:
				break;
			}
		}
	};

	private final int PERMISSION_REQUEST_CODE = 0x001;
	private static final String[] permissionManifest = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.RECORD_AUDIO
	};

	private void permissionCheck() {
		LogUtils.e("Permission", "permissionCheck begin");
		if (Build.VERSION.SDK_INT >= 23) {

			LogUtils.e("Permission", "SDK_INT >= 23");
			boolean permissionState = true;
			for (String permission : permissionManifest) {
				if (ContextCompat.checkSelfPermission(this, permission)
						!= PackageManager.PERMISSION_GRANTED) {
					permissionState = false;
				}
			}
			if (!permissionState) {
				LogUtils.e("Permission", "permissionState false");
				ActivityCompat.requestPermissions(this, permissionManifest, PERMISSION_REQUEST_CODE);
			} else {
				LogUtils.e("Permission", "permissionState true");
				beginRecord();
			}
		} else {
			LogUtils.e("Permission", "SDK_INT < 23");

			beginRecord();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			boolean isGrant = true;

			LogUtils.e("Permission", "onRequestPermissionsResult");

			for (int i = 0; i < permissions.length; i++) {
				LogUtils.e("Video", "permission: " + permissions[i] + " = " + grantResults[i]);
				if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
					isGrant = false;
				}
			}
			if (isGrant) {
				beginRecord();
			}
		}
	}
}

## 说明
### 来源：
该项目主要是修改自：https://github.com/zhanxiaokai/Android-AudioRecorder
非常感谢原作者的奉献！该项目主要功能是录制一段PCM语音，并将其保存到手机中。
### 目的：
原项目是Eclipse工程，现在改由AndroidStudio工程
### 改动
1. compileSdkVersion修改为25，增加了读取SD卡和录音的权限请求
2. 音频文件放在SD卡的根目录下

		String sdTempPath = SDCardUtils.getExternalSdCardPath(this) + File.separator + "1RecordAudio";
		File sdTempFile = new File(sdTempPath);
		if (!sdTempFile.exists()) {
			sdTempFile.mkdir();
		}
		outputPath = new File(sdTempFile, new Date().getTime() + ".pcm").getAbsolutePath();

3. 其中定然有很多不足之处，请大家斧正！
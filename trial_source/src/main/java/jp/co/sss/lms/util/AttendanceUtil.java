package jp.co.sss.lms.util;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.mapper.MSectionMapper;

/**
 * 勤怠管理のユーティリティクラス
 * 
 * @author 東京ITスクール
 */
@Component
public class AttendanceUtil {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private MSectionMapper mSectionMapper;
	@Autowired
	private TrainingTime trainingTime;

	/**
	 * SSS定時・出退勤時間を元に、遅刻早退を判定をする
	 * 
	 * @param trainingStartTime 開始時刻
	 * @param trainingEndTime   終了時刻
	 * @return 遅刻早退を判定メソッド
	 */
	public AttendanceStatusEnum getStatus(TrainingTime trainingStartTime,
			TrainingTime trainingEndTime) {
		return getStatus(trainingStartTime, trainingEndTime, Constants.SSS_WORK_START_TIME,
				Constants.SSS_WORK_END_TIME);
	}

	/**
	 * 与えられた定時・出退勤時間を元に、遅刻早退を判定する
	 * 
	 * @param trainingStartTime 開始時刻
	 * @param trainingEndTime   終了時刻
	 * @param workStartTime     定時開始時刻
	 * @param workEndTime       定時終了時刻
	 * @return 判定結果
	 */
	private AttendanceStatusEnum getStatus(TrainingTime trainingStartTime,
			TrainingTime trainingEndTime, TrainingTime workStartTime, TrainingTime workEndTime) {
		// 定時が不明な場合、NONEを返却する
		if (workStartTime == null || workStartTime.isBlank() || workEndTime == null
				|| workEndTime.isBlank()) {
			return AttendanceStatusEnum.NONE;
		}
		boolean isLate = false, isEarly = false;
		// 定時より1分以上遅く出社していたら遅刻(＝はセーフ)
		if (trainingStartTime != null && trainingStartTime.isNotBlank()) {
			isLate = (trainingStartTime.compareTo(workStartTime) > 0);
		}
		// 定時より1分以上早く退社していたら早退(＝はセーフ)
		if (trainingEndTime != null && trainingEndTime.isNotBlank()) {
			isEarly = (trainingEndTime.compareTo(workEndTime) < 0);
		}
		if (isLate && isEarly) {
			return AttendanceStatusEnum.TARDY_AND_LEAVING_EARLY;
		}
		if (isLate) {
			return AttendanceStatusEnum.TARDY;
		}
		if (isEarly) {
			return AttendanceStatusEnum.LEAVING_EARLY;
		}
		return AttendanceStatusEnum.NONE;
	}

	/**
	 * 中抜け時間を時(hour)と分(minute)に変換
	 *
	 * @param min 中抜け時間
	 * @return 時(hour)と分(minute)に変換したクラス
	 */
	public TrainingTime calcBlankTime(int min) {
		int hour = min / 60;
		int minute = min % 60;
		TrainingTime total = new TrainingTime(hour, minute);
		return total;
	}

	/**
	 * 時刻分を丸めた本日日付を取得
	 * 
	 * @return "yyyy/M/d"形式の日付
	 */
	public Date getTrainingDate() {
		Date trainingDate;
		try {
			trainingDate = dateUtil.parse(dateUtil.toString(new Date()));
		} catch (ParseException e) {
			// DateUtil#toStringとparseは同様のフォーマットを使用しているため、起こりえないエラー
			throw new IllegalStateException();
		}
		return trainingDate;
	}

	/**
	 * 休憩時間取得
	 * 
	 * @return 休憩時間
	 */
	public LinkedHashMap<Integer, String> setBlankTime() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		map.put(null, "");
		for (int i = 15; i < 480;) {
			int hour = i / 60;
			int minute = i % 60;
			String time;

			if (hour == 0) {
				time = minute + "分";

			} else if (minute == 0) {
				time = hour + "時間";
			} else {
				time = hour + "時" + minute + "分";
			}

			map.put(i, time);

			i = i + 15;

		}
		return map;
	}

	/**
	 * 研修日の判定
	 * 
	 * @param courseId
	 * @param trainingDate
	 * @return 判定結果
	 */
	public boolean isWorkDay(Integer courseId, Date trainingDate) {
		Integer count = mSectionMapper.getSectionCountByCourseId(courseId, trainingDate);
		if (count > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Task.26 時間のプルダウンマップを生成
	 * 
	 * @return 時間のプルダウンマップ
	 */
	public LinkedHashMap<Integer,String> getHourMap(){
		
//		AttendanceForm をインスタンス化し、ログインユーザー情報や選択用マップ（時・分）を設定する。
//		>>>時間,分のプルダウン用のマップを生成する
//
//		時間マップ
//		{0,"00"},{1,"01"}...{23,"23"}
		LinkedHashMap<Integer, String> hourMap = new LinkedHashMap<>();
//		# 処理
		hourMap.put(null, "");
		for (Integer i = 0; i < 24; i++) {
			hourMap.put (i, String.format("%02d", i));
			}
			
			return hourMap;
		
	}
	
	/**
	 * Task.26 分のプルダウンマップを生成
	 * 
	 * @return 分のプルダウンマップ
	 */
	public LinkedHashMap<Integer,String> getMinuteMap(){
		
//		分マップ
//		{0,"00"},{1,"01"}...{59,"59"}
		LinkedHashMap<Integer, String> minuteMap = new LinkedHashMap<>();
//		# 処理
//		 * 分マップに{null,""}を追加する
		minuteMap.put(null, "");
		for (Integer i = 0; i < 60; i++) {
			minuteMap.put (i, String.format("%02d", i));
		}

		return minuteMap;
	}
	
	/**
	 * Task.26 時間(時)の切り出し
	 * 
	 * @param trainingTime 開始時刻or終了時刻
	 * @return 切り出された時間(時)
	 */
//	public Integer getHour(String trainingTime) {
//		
//		Integer hour = hourMap.get(i);
//		
//		
//		
//		return hour;
//		
//	}
	
	/**
	 * Task.26 時間(分)の切り出し
	 * 
	 * @param trainingTime 開始時刻or終了時刻
	 * @return 切り出された時間(分)
	 */
//	public Integer getMinute(String trainingTime) {
//		
//		Integer minute;
//		
//		
//		
//		
//		return minute;
//	}
	
	/**
	 * Task.26 受講時間数を算出する
	 * 
	 * @param trainingStartTime
	 * @param trainingEndTime
	 * @return 受講合計時間数
	 */
	public TrainingTime calcJukoTime(TrainingTime trainingStartTime, TrainingTime trainingEndTime) {
		
		return trainingTime;
		
	}
	
	/**
	 * Task.26 中抜け時間(文字列)を数字に変換
	 * 
	 * @param min
	 * @return brankTime
	 */
//	public Integer reverseBlankTime(String time) {
//		
//		return brankTime;
//	}
	

}

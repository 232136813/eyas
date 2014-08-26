package com.lenovo.ecs.eyas.util;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeUnitExt{
	private static final Logger logger = LoggerFactory.getLogger(TimeUnitExt.class);
	public static TimeUnit getTimeUnit(String unit){
		if(unit == null || unit.equalsIgnoreCase("millisecond")){
			return TimeUnit.MILLISECONDS;
		}
		if(unit.equalsIgnoreCase("second")){
			return TimeUnit.SECONDS;
		}
		if(unit.equalsIgnoreCase("nanosecond")){
			return TimeUnit.NANOSECONDS;
		}
		if(unit.equalsIgnoreCase("microsecond")){
			return TimeUnit.MICROSECONDS;
		}
		if(unit.equalsIgnoreCase("minute")){
			return TimeUnit.MINUTES;
		}
		
		if(unit.equalsIgnoreCase("hour")){
			return TimeUnit.HOURS;
		}
		
		if(unit.equalsIgnoreCase("day")){
			return TimeUnit.DAYS;
		}
		return TimeUnit.MILLISECONDS;

		
	}
	
	public static Long getTimeInMillSec(String srcTime){
		if(srcTime != null){
			String[] srcTimetrings = srcTime.split("\\.");
			if(srcTimetrings.length == 2){
				TimeUnit unit = TimeUnitExt.getTimeUnit(srcTimetrings[1]);
				try{
					Integer time = Integer.parseInt(srcTimetrings[0]);
					return unit.toMillis(time);
				}catch(Exception e){
					logger.error("Get Time In MilliSec error srcTime = " + srcTime);
				}
			}else if(srcTimetrings.length == 1){
				try {
					return Long.parseLong(srcTime);
				} catch (NumberFormatException e) {
					logger.error("Get Time In MilliSec error srcTime = " + srcTime);
				}
			}
		}
		return null;
	}
	
	
}

package com.lenovo.ecs.eyas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum StorageUnit {
	
	BYTE{
		
		
		public long toByte(long storage) {
			return storage;
		}

		
		public long toKilobyte(long storage) {
			return storage/unit;
		}

		
		public long toMegabyte(long storage) {
			return toKilobyte(storage)/unit;
		}

		
		public long toGigabyte(long storage) {

			return toGigabyte(storage)/unit;
		}

		
		public long toTrillionbyte(long storage) {

			return toGigabyte(storage)/unit;
		}

		
		public long toPetabyte(long storage) {

			return toTrillionbyte(storage)/unit;
		}
		
	},
	KILOBYTE{
		
		public long toByte(long storage) {

			return storage * unit;
		}

		
		public long toKilobyte(long storage) {
			return storage;
		}

		
		public long toMegabyte(long storage) {
			return storage / unit;
		}

		
		public long toGigabyte(long storage) {

			return toMegabyte(storage) / unit;
		}

		
		public long toTrillionbyte(long storage) {

			return toGigabyte(storage) / unit;
		}

		
		public long toPetabyte(long storage) {

			return toTrillionbyte(storage) / unit;
		}
	}, 
	MEGABYTE{
		
		public long toByte(long storage) {

			return toKilobyte(storage) * unit;
		}

		
		public long toKilobyte(long storage) {

			return storage * unit;
		}

		
		public long toMegabyte(long storage) {

			return storage;
		}

		
		public long toGigabyte(long storage) {

			return storage / unit;
		}

		
		public long toTrillionbyte(long storage) {

			return toGigabyte(storage) / unit;
		}

		
		public long toPetabyte(long storage) {

			return toTrillionbyte(storage) / unit;
		}
	}, 
	GIGABYTE{
		
		public long toByte(long storage) {

			return toKilobyte(storage) * unit;
		}

		
		public long toKilobyte(long storage) {

			return toMegabyte(storage) * unit;
		}

		
		public long toMegabyte(long storage) {

			return storage * unit;
		}

		
		public long toGigabyte(long storage) {

			return storage;
		}

		
		public long toTrillionbyte(long storage) {

			return storage / unit;
		}

		
		public long toPetabyte(long storage) {
			return toTrillionbyte(storage) /unit;
		}
	},
	TRILLIONBYTE{
		
		public long toByte(long storage) {
			return toKilobyte(storage) * unit;
		}

		
		public long toKilobyte(long storage) {

			return toMegabyte(storage) * unit;
		}

		
		public long toMegabyte(long storage) {

			return toGigabyte(storage) * unit;
		}

		
		public long toGigabyte(long storage) {

			return storage * unit;
		}

		
		public long toTrillionbyte(long storage) {

			return storage;
		}

		
		public long toPetabyte(long storage) {

			return storage * unit;
		}
	},
	PETABYTE{
		
		public long toByte(long storage) {

			return toKilobyte(storage) * unit;
		}

		
		public long toKilobyte(long storage) {

			return toMegabyte(storage) * unit;
		}

		
		public long toMegabyte(long storage) {

			return toGigabyte(storage) * unit;
		}

		
		public long toGigabyte(long storage) {

			return toTrillionbyte(storage) * unit;
		}

		
		public long toTrillionbyte(long storage) {

			return storage * unit;
		}

		
		public long toPetabyte(long storage) {

			return storage;
		}
	};

	static final long unit = 1024;
	private static final Logger logger = LoggerFactory.getLogger(StorageUnit.class);
    public long toByte(long storage) {
        throw new AbstractMethodError();
    }
    
    public long toKilobyte(long storage){
    	throw new AbstractMethodError();
    }
    
    public long toMegabyte(long storage){
    	throw new AbstractMethodError();
    }
    
    public long toGigabyte(long storage){
      	throw new AbstractMethodError();
    }
    public long toTrillionbyte(long storage){
      	throw new AbstractMethodError();
    }
    
    public long toPetabyte (long storage){
      	throw new AbstractMethodError();
    }
    
    public static StorageUnit getUnit(String unit){

    	if(unit == null || unit.equalsIgnoreCase("byte")){
    		return StorageUnit.BYTE;
    	}
    	if(unit.equalsIgnoreCase("kilobyte"))
    		return StorageUnit.KILOBYTE;
    	if(unit.equalsIgnoreCase("megabyte"))
    		return StorageUnit.MEGABYTE;
    	if(unit.equalsIgnoreCase("gigabyte"))
    		return StorageUnit.GIGABYTE;
    	if(unit.equalsIgnoreCase("trillionbyte"))
    		return StorageUnit.TRILLIONBYTE;
    	if(unit.equalsIgnoreCase("petabyte"))
    		return StorageUnit.PETABYTE;
    	
    	return StorageUnit.BYTE;
    }
    
    
    public static Long getStorageInBytes(String srcStorage){
		if(srcStorage != null){
			String[] srcStorageStrings = srcStorage.split("\\.");
			if(srcStorageStrings.length == 2){
				StorageUnit unit = getUnit(srcStorageStrings[1]);
				try{
					Long storage = Long.parseLong(srcStorageStrings[0]);
					return unit.toByte(storage);
				}catch(Exception e){
					logger.error("Get Storage In Byte error srcStorage = " + srcStorage);
				}
			}else if(srcStorageStrings.length == 1){
				try{
					return Long.parseLong(srcStorage);
				}catch(NumberFormatException e){
					logger.error("Get Storage In Byte error srcStorage = " + srcStorage);
				}
			}
		}
		return null;
    }

}


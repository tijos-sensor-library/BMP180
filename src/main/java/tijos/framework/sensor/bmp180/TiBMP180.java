package tijos.framework.sensor.bmp180;

import java.io.IOException;

import tijos.framework.devicecenter.TiI2CMaster;
import tijos.util.BigBitConverter;
import tijos.util.Delay;

/*
*  BMP180 Digital Pressure Sensor Class
*  BMP180 from Bosch Sensortec
*  https://os.mbed.com/users/Wosser1sProductions/code/BMP180/docs/tip/
*  
*  @author TiJOS
*/

/**
 * Bosch BMP180 Digital Pressure Sensor Class
 * Getting temperature and pressure values from Bosch BMP180 barometer
 * @author Administrator
 * */
public class TiBMP180 {

	/**
	 *	default device address is 0x77  
	 */
	private static final int BMP180_I2C_ADDRESS = 0x77;

	/**
	 * < 1 pressure sample : 4.5 ms delay
	 */
	public static final int OSS_ULTRA_LOW_POWER = 0;

	/**
	 * < 2 pressure samples : 7.5 ms delay
	 */
	public static final int OSS_STANDARD = 1;

	/**
	 * < 4 pressure samples : 13.5 ms delay
	 */
	public static final int OSS_HIGH_RESOLUTION = 2;

	/**
	 * < 8 pressure samples :25.5 ms delay
	 */
	public static final int OSS_ULTRA_HIGH_RESOLUTION = 3;

	/**
	 *  the temperature fully compensated value is returned in this variable.
	 *  Degrees celsius with one decimal so 253 is 25.3 C.
	 */
	private double temperature = Double.NaN;

	/**
	 * the barometric pressure fully compensated value is returned in this
	 * variable. Pressure is in Pa so 88007 is 88.007 kPa.
	 */
	private double pressure = Double.NaN;

	/**
	 * TiI2CMaster object
	 */
	private TiI2CMaster bmp180i2c;

	/**
	 * over sampling setting
	 */
	private int oss;
	
	/**
	 * Parameters for calculation
	 */
	private int ac2, ac3, ac5, ac6, b1, b2, md;
	private long ac1, ac4;
	private long x1, x2, x3, mc;
	private long b3, b4, b5, b6, b7;
	
	/**
	 * Initialize with default oss settings - OSS_ULTRA_LOW_POWER
	 * @param i2c I2C master object for communication
	 */
	public TiBMP180(TiI2CMaster i2c) {
		this(i2c, OSS_ULTRA_LOW_POWER);
	}

	/**
	 * Initialization with i2c object and oss settings
	 * 
	 * @param i2c
	 *            I2C master object for communication
	 * @param oss
	 *            over sampling setting -
	 *            OSS_ULTRA_LOW_POWER/OSS_STANDARD/OSS_HIGH_RESOLUTION/OSS_ULTRA_HIGH_RESOLUTION
	 */
	public TiBMP180(TiI2CMaster i2c, int oss) {

		this.bmp180i2c = i2c;
		this.oss = oss;

	}

	/**
	 * Read calibration coefficients
	 * @throws IOException
	 */
	public void begin() throws IOException {

		this.bmp180i2c.setBaudRate(400);

		byte[] data = new byte[22];

		// read calibration data
		data[0] = (byte) 0xAA;

		// set the eeprom pointer position to 0xAA
		// read 11 x 16 bits at this position
		bmp180i2c.read(BMP180_I2C_ADDRESS, 0XAA, data, 0, 22);
		

		Delay.msDelay(10);

		// store calibration data for further calculus
		ac1 = BigBitConverter.ToInt16(data, 0);
		ac2 = BigBitConverter.ToInt16(data, 2);
		ac3 = BigBitConverter.ToInt16(data, 4);
		ac4 = BigBitConverter.ToUInt16(data, 6);
		ac5 = BigBitConverter.ToUInt16(data, 8);
		ac6 = BigBitConverter.ToUInt16(data, 10);

		b1 = BigBitConverter.ToInt16(data, 12);
		b2 = BigBitConverter.ToInt16(data, 14);

		mc = BigBitConverter.ToInt16(data, 18);
		md = BigBitConverter.ToInt16(data, 20);

	}

	/**
	 * Temperature and pressure measurement
	 * @throws IOException
	 */
	public void measure() throws IOException {
				
		readTemperature();
		
		readPressure();
	}

	/**
	 * return the latest temperature 
	 * @return temperature
	 */
	public double getTemperature() {
		return this.temperature;
	}

	/**
	 * return the latest pressure
	 * @return
	 */
	public double getPressure() {
		return this.pressure;
	}

	/**
	 * Perform temperature measurement
	 * 
	 * @throws IOException
	 */
	private void readTemperature() throws IOException {

		byte[] data = new byte[2];
		// request temperature measurement
		data[0] = (byte) 0xF4;
		data[1] = 0x2E;

		// write 0XF2 into reg 0XF4
		bmp180i2c.write(BMP180_I2C_ADDRESS, data, 0, 2);

		Delay.msDelay(5);

		// read raw temperature data
		data[0] = (byte) 0xF6;

		// set eeprom pointer position to 0XF6
		bmp180i2c.write(BMP180_I2C_ADDRESS,data, 0, 1);

		// get 16 bits at this position
		bmp180i2c.read(BMP180_I2C_ADDRESS,data, 0, 2);

		long temp = BigBitConverter.ToUInt16(data, 0);
		this.temperature = trueTemperature(temp);
	}

	/**
	 * Perform pressure measurement
	 * 
	 * @throws IOException
	 */
	private void readPressure() throws IOException {
		byte[] data = new byte[4];
		// request pressure measurement
		data[0] = (byte) 0xF4;
		data[1] = (byte) (0x34 + (oss << 6));

		// write 0x34 + (m_oss << 6) into reg 0XF4
		bmp180i2c.write(BMP180_I2C_ADDRESS,data, 0, 2);

		// Rounded up wait times to be safe
		switch (oss) {
		case OSS_ULTRA_LOW_POWER:
			Delay.msDelay(5);
			break;
		case OSS_STANDARD:
			Delay.msDelay(8);
			break;
		case OSS_HIGH_RESOLUTION:
			Delay.msDelay(14);
			break;
		case OSS_ULTRA_HIGH_RESOLUTION:
			Delay.msDelay(26);
			break;
		default:
			Delay.msDelay(5);
			break;
		}

		// read raw pressure data
		data[0] = (byte) 0xF6;

		// set eeprom pointer position to 0XF6
		bmp180i2c.write(BMP180_I2C_ADDRESS,data, 0, 1);

		// get 16 bits at this position
		data[0] = 0;
		bmp180i2c.read(BMP180_I2C_ADDRESS,data, 1, 3);

		long p = BigBitConverter.ToUInt32(data, 0);

		p >>= (8 - oss);
		
		this.pressure = this.truePressure(p);
	}

	/**
	 * Calculation of the temperature from the digital output
	 */
	private double trueTemperature(long ut) {

		// straight out from the documentation
		x1 = ((ut - ac6) * ac5) >> 15;
		x2 = (mc << 11) / (x1 + md);
		b5 = x1 + x2;

		// convert to Celsius
		return ((b5 + 8) >> 4) / 10.0;
	}

	/**
	 * Calculation of the pressure from the digital output
	 */
	private double truePressure(long up) {
		long p;

		// straight out from the documentation
		b6 = b5 - 4000;
		x1 = (b2 * ((b6 * b6) >> 12)) >> 11;
		x2 = (ac2 * b6) >> 11;
		x3 = x1 + x2;

		b3 = (((ac1 * 4 + x3) << oss) + 2) >> 2;

		x1 = (ac3 * b6) >> 13;
		x2 = (b1 * ((b6 * b6) >> 12)) >> 16;
		x3 = (x1 + x2 + 2) >> 2;

		b4 = (ac4 * (x3 + 32768)) >> 15;
		b7 = (up - b3) * (50000 >> oss);

		if (b7 < 0x80000000){
			p = (b7 << 1) / b4;
		}
		else{
			p = (b7 / b4) << 1;
		}

		x1 = (p >> 8) * (p >> 8);
		x1 = (x1 * 3038) >> 16;
		x2 = (-7357 * p) >> 16;
		p = p + ((x1 + x2 + 3791) >> 4); 

		// pressure in pa
		return p;
	}
	
	@SuppressWarnings("unused")
	private void printfAllParameters()
	{
		System.out.printf("ac1 = %d ac2 = %d ac3 = %d ac4 = %d ac5 = %d ac6 = %d \n", ac1,ac2,ac3,ac4,ac5, ac6);
		System.out.printf("b1 = %d b2 = %d b3 = %d b4 = %d b5 = %d b6 = %d b7 = 5d \n", b1,b2, b3,b4,b5, b6, b7);
		
		System.out.printf("x1 = %d x2 = %d x3=%d \n", x1,x2, x3);
		System.out.printf("mc = %d md = %d \n", mc, md);
		
	}

}

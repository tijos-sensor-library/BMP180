package tijos.framework.sensor.bmp180;

import java.io.IOException;

import tijos.framework.devicecenter.TiI2CMaster;
import tijos.framework.sensor.bmp180.TiBMP180;
import tijos.util.Delay;

public class TiBMP180Sample {

	public static void main(String[] args) {

		try {
			/*
			 * 定义使用的TiI2CMaster port
			 */
			int i2cPort0 = 0;

			/*
			 * 资源分配， 将i2cPort0分配给TiI2CMaster对象i2c0
			 */
			TiI2CMaster i2c0 = TiI2CMaster.open(i2cPort0);

			TiBMP180 bmp180 = new TiBMP180(i2c0, TiBMP180.OSS_ULTRA_HIGH_RESOLUTION);

			bmp180.begin();		
			
			int num = 1000;
			while (num -- > 0) {
				try {

					bmp180.measure();

					double temperature = bmp180.getTemperature();
					double pressure = bmp180.getPressure();

					System.out.println("pressure = " + pressure + " temperature = " + temperature);

					Delay.msDelay(2000);
				} catch (Exception ex) {

					ex.printStackTrace();
				}

			}
		} catch (IOException ie) {
			ie.printStackTrace();
		}

	}
}

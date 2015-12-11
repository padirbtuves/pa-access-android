package ioio.examples.hello;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

/**
 * This is the thread on which all the IOIO activity happens. It will be run
 * every time the application is resumed and aborted when it is paused. The
 * method setup() will be called right after a connection with the IOIO has
 * been established (which might happen several times!). Then, loop() will
 * be called repetitively until the IOIO gets disconnected.
 */
class Looper extends BaseIOIOLooper {
	/** The on-board LED. */
	private DigitalOutput led_;
	private DigitalOutput lock;

	private final MainActivity parent;
	
	
	public Looper(MainActivity parent) {
		this.parent = parent;
	}
	/**
	 * Called every time a connection with IOIO has been established.
	 * Typically used to open pins.
	 *
	 * @throws ConnectionLostException
	 *             When IOIO connection is lost.
	 *
	 * @see ioio.lib.util.BaseIOIOLooper#setup()
	 */
	@Override
	protected void setup() throws ConnectionLostException {
		parent.showVersions(ioio_, "IOIO connected!");
		led_ = ioio_.openDigitalOutput(0, false);
		lock = ioio_.openDigitalOutput(4, false);

		parent.enableUi(true);
	}

	/**
	 * Called repetitively while the IOIO is connected.
	 *
	 * @throws ConnectionLostException
	 *             When IOIO connection is lost.
	 * @throws InterruptedException
	 *             When the IOIO thread has been interrupted.
	 *
	 * @see ioio.lib.util.IOIOLooper#loop()
	 */
	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		boolean val = System.currentTimeMillis() - parent.getLastOpen() < 5000;
		
		led_.write(val);
		lock.write(val);
		
		Thread.sleep(100);
		parent.showLockInfo(val);
	}

	/**
	 * Called when the IOIO is disconnected.
	 *
	 * @see ioio.lib.util.IOIOLooper#disconnected()
	 */
	@Override
	public void disconnected() {
		parent.enableUi(false);
		parent.toast("IOIO disconnected");
	}

	/**
	 * Called when the IOIO is connected, but has an incompatible firmware
	 * version.
	 *
	 * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
	 */
	@Override
	public void incompatible() {
		parent.showVersions(ioio_, "Incompatible firmware version!");
	}
	
	public void open() {
		parent.setLastOpen(System.currentTimeMillis());
	}

	
	
}
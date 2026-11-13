/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.util;

import java.util.Random;

import core.*;

/**
 * Energy model for routing modules. Handles power use from scanning (device
 * discovery), scan responses, and data transmission. If scanning is done more
 * often than 1/s, constant scanning is assumed (and power consumption does not
 * increase from {@link #scanEnergy} value).
 */
public class EnergyModel implements ModuleCommunicationListener {
	/** Initial units of energy -setting id ({@value}). Can be either a
	 * single value, or a range of two values. In the latter case, the used
	 * value is a uniformly distributed random value between the two values. */
	public static final String INIT_ENERGY_S = "initialEnergy";

	/** Setting id for energy usage per PIT lookup -setting id ({@value}). `*/
	public static final String PIT_LOOKUP_ENERGY_S = "pitLookupEnergy";
	/** Setting id for energy usage per PIT insert -setting id ({@value}). `*/
    public static final String PIT_INSERT_ENERGY_S = "pitInsertEnergy";
	/** Setting id for energy usage per PIT update -setting id ({@value}). `*/
	public static final String PIT_UPDATE_ENERGY_S = "pitUpdateEnergy";
	/** Setting id for energy usage per PIT remove -setting id ({@value}). `*/	
	public static final String PIT_REMOVE_ENERGY_S = "pitRemoveEnergy";
	/** Setting id for energy usage per static cache lookup. */
	public static final String STATIC_CACHE_LOOKUP_ENERGY_S = "staticCacheLookupEnergy";
	/** Setting id for energy usage per static cache write. */
	public static final String STATIC_CACHE_WRITE_ENERGY_S = "staticCacheWriteEnergy";
	/** Setting id for energy usage per static cache initial write. */
	public static final String STATIC_CACHE_INIT_WRITE_ENERGY_S = "staticCacheInitWriteEnergy";
	/** Setting id for energy usage per packet transmission. */
	public static final String PACKET_TRANSMISSION_ENERGY_S = "packetTransmissionEnergy";
	/** Setting id for energy usage per packet reception. */
	public static final String PACKET_RECEPTION_ENERGY_S = "packetReceptionEnergy";
	/** Setting id for energy usage per opportunistic cache lookup. */
	public static final String OPPO_CACHE_LOOKUP_ENERGY_S = "oppoCacheLookupEnergy";
	/** Setting id for energy usage per opportunistic cache write. */
	public static final String OPPO_CACHE_WRITE_ENERGY_S = "oppoCacheWriteEnergy";

	/** Energy update warmup period -setting id ({@value}). Defines the
	 * simulation time after which the energy level starts to decrease due to
	 * scanning, transmissions, etc. Default value = 0. If value of "-1" is
	 * defined, uses the value from the report warmup setting
	 * {@link report.Report#WARMUP_S} from the namespace
	 * {@value report.Report#REPORT_NS}. */
	public static final String WARMUP_S = "energyWarmup";

	/** {@link ModuleCommunicationBus} identifier for the "current amount of
	 * energy left" variable. Value type: double */
	public static final String ENERGY_VALUE_ID = "Energy.value";
	/** {@link ModuleCommunicationBus} identifier for the total consumed energy.
	 * Value type: double */
	public static final String ENERGY_CONSUMED_ID = "Energy.consumed";

	/** Initial energy levels from the settings */
	private final double[] initEnergy;
	private double warmupTime;
	/** current energy level */
	private double currentEnergy;
	/** total energy consumed so far */
	private double totalEnergyConsumed;
	/** energy usage per PIT lookup */
	private double pitLookupEnergy;
	/** total PIT lookup energy consumed by this node */
	private double totalPitLookupEnergy;
	/** energy usage per PIT insert */
	private double pitInsertEnergy;
	/** total PIT insert energy consumed by this node */
	private double totalPitInsertEnergy;
	/** energy usage per PIT update */
	private double pitUpdateEnergy;
	/** total PIT update energy consumed by this node */
	private double totalPitUpdateEnergy;
	/** energy usage per PIT remove */
	private double pitRemoveEnergy;
	/** total PIT remove energy consumed by this node */
	private double totalPitRemoveEnergy;
	/** energy usage per static cache lookup */
	private double staticCacheLookupEnergy;
	/** total static cache lookup energy consumed by this node */
	private double totalStaticCacheLookupEnergy;
	/** energy usage per static cache write */
	private double staticCacheWriteEnergy;
	/** total static cache write energy consumed by this node */
	private double totalStaticCacheWriteEnergy;
	/** energy usage per static cache initial write */
	private double staticCacheInitWriteEnergy;
	/** total static cache initial write energy consumed by this node */
	private double totalStaticCacheInitWriteEnergy;
	/** energy usage per packet transmission */
	private double packetTransmissionEnergy;
	/** total packet transmission energy consumed by this node */
	private double totalPacketTransmissionEnergy;
	/** energy usage per packet reception */
	private double packetReceptionEnergy;
	/** total packet reception energy consumed by this node */
	private double totalPacketReceptionEnergy;
	/** energy usage per opportunistic cache lookup */
	private double oppoCacheLookupEnergy;
	/** total opportunistic cache lookup energy consumed by this node */
	private double totalOppCacheLookupEnergy;
	/** energy usage per opportunistic cache write */
	private double oppoCacheWriteEnergy;
	/** total opportunistic cache write energy consumed by this node */
	private double totalOppCacheWriteEnergy;
	/** sim time of the last energy updated */
	private double lastUpdate;
	private ModuleCommunicationBus comBus;
	private static Random rng = null;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EnergyModel(Settings s) {
		this.initEnergy = s.getCsvDoubles(INIT_ENERGY_S);
		this.totalEnergyConsumed = 0;
		this.totalPitLookupEnergy = 0;
		this.totalPitInsertEnergy = 0;
		this.totalPitUpdateEnergy = 0;
		this.totalPitRemoveEnergy = 0;
		this.totalStaticCacheLookupEnergy = 0;
		this.totalStaticCacheWriteEnergy = 0;
		this.totalStaticCacheInitWriteEnergy = 0;
		this.totalPacketTransmissionEnergy = 0;
		this.totalPacketReceptionEnergy = 0;
		this.totalOppCacheLookupEnergy = 0;
		this.totalOppCacheWriteEnergy = 0;

		if (this.initEnergy.length != 1 && this.initEnergy.length != 2) {
			throw new SettingsError(INIT_ENERGY_S + " setting must have " +
					"either a single value or two comma separated values");
		}

		this.pitLookupEnergy = s.getDouble(PIT_LOOKUP_ENERGY_S, 0.0);
		this.pitInsertEnergy = s.getDouble(PIT_INSERT_ENERGY_S, 0.0);
		this.pitUpdateEnergy = s.getDouble(PIT_UPDATE_ENERGY_S, 0.0);
		this.pitRemoveEnergy = s.getDouble(PIT_REMOVE_ENERGY_S, 0.0);
		this.staticCacheLookupEnergy = s.getDouble(STATIC_CACHE_LOOKUP_ENERGY_S, 0.0);
		this.staticCacheWriteEnergy = s.getDouble(STATIC_CACHE_WRITE_ENERGY_S, 0.0);
		this.staticCacheInitWriteEnergy = s.getDouble(STATIC_CACHE_INIT_WRITE_ENERGY_S, 0.0);
		this.packetTransmissionEnergy = s.getDouble(PACKET_TRANSMISSION_ENERGY_S, 0.0);
		this.packetReceptionEnergy = s.getDouble(PACKET_RECEPTION_ENERGY_S, 0.0);
		this.oppoCacheLookupEnergy = s.getDouble(OPPO_CACHE_LOOKUP_ENERGY_S, 0.0);
		this.oppoCacheWriteEnergy = s.getDouble(OPPO_CACHE_WRITE_ENERGY_S, 0.0);
		if (s.contains(WARMUP_S)) {
			this.warmupTime = s.getInt(WARMUP_S);
			if (this.warmupTime == -1) {
				this.warmupTime = new Settings(report.Report.REPORT_NS).
					getInt(report.Report.WARMUP_S);
			}
		}
		else {
			this.warmupTime = 0;
		}
	}

	/**
	 * Copy constructor.
	 * @param proto The model prototype where setting values are copied from
	 */
	protected EnergyModel(EnergyModel proto) {
		this.initEnergy = proto.initEnergy;
		setEnergy(this.initEnergy);
		this.warmupTime  = proto.warmupTime;
		this.pitLookupEnergy = proto.pitLookupEnergy;
		this.totalPitLookupEnergy = proto.totalPitLookupEnergy;
		this.pitInsertEnergy = proto.pitInsertEnergy;
		this.totalPitInsertEnergy = proto.totalPitInsertEnergy;
		this.pitUpdateEnergy = proto.pitUpdateEnergy;
		this.totalPitUpdateEnergy = proto.totalPitUpdateEnergy;
		this.pitRemoveEnergy = proto.pitRemoveEnergy;
		this.totalPitRemoveEnergy = proto.totalPitRemoveEnergy;
		this.staticCacheLookupEnergy = proto.staticCacheLookupEnergy;
		this.totalStaticCacheLookupEnergy = proto.totalStaticCacheLookupEnergy;
		this.staticCacheWriteEnergy = proto.staticCacheWriteEnergy;
		this.totalStaticCacheWriteEnergy = proto.totalStaticCacheWriteEnergy;
		this.staticCacheInitWriteEnergy = proto.staticCacheInitWriteEnergy;
		this.totalStaticCacheInitWriteEnergy = proto.totalStaticCacheInitWriteEnergy;
		this.packetTransmissionEnergy = proto.packetTransmissionEnergy;
		this.totalPacketTransmissionEnergy = proto.totalPacketTransmissionEnergy;
		this.packetReceptionEnergy = proto.packetReceptionEnergy;
		this.totalPacketReceptionEnergy = proto.totalPacketReceptionEnergy;
		this.oppoCacheLookupEnergy = proto.oppoCacheLookupEnergy;
		this.totalOppCacheLookupEnergy = proto.totalOppCacheLookupEnergy;
		this.oppoCacheWriteEnergy = proto.oppoCacheWriteEnergy;
		this.totalOppCacheWriteEnergy = proto.totalOppCacheWriteEnergy;
		this.totalEnergyConsumed = proto.totalEnergyConsumed;
		this.comBus = null;
		this.lastUpdate = 0;
	}

	public EnergyModel replicate() {
		return new EnergyModel(this);
	}

	/**
	 * Sets the current energy level into the given range using uniform
	 * random distribution.
	 * @param range The min and max values of the range, or if only one value
	 * is given, that is used as the energy level
	 */
	protected void setEnergy(double range[]) {
		if (range.length == 1) {
			this.currentEnergy = range[0];
		}
		else {
			if (rng == null) {
				rng = new Random((int)(range[0] + range[1]));
			}
			this.currentEnergy = range[0] +
				rng.nextDouble() * (range[1] - range[0]);
		}
	}

	/**
	 * Returns the current energy level
	 * @return the current energy level
	 */
	public double getEnergy() {
		return this.currentEnergy;
	}

	/**
	 * Returns the total energy consumed by the node so far.
	 * @return total consumed energy
	 */
	public double getConsumedEnergy() {
		return this.totalEnergyConsumed;
	}

	/**
	 * Returns the initial configured energy level used for this node.
	 * @return initial energy value
	 */
	public double getInitialEnergy() {
		if (this.initEnergy.length == 1) {
			return this.initEnergy[0];
		}
		return (this.initEnergy[0] + this.initEnergy[1]) / 2.0;
	}

	public double getPitLookupEnergy() { return this.pitLookupEnergy; }
	public double getTotalPitLookupEnergy() { return this.totalPitLookupEnergy; }
	public double getPitInsertEnergy() { return this.pitInsertEnergy; }
	public double getTotalPitInsertEnergy() { return this.totalPitInsertEnergy; }
	public double getPitUpdateEnergy() { return this.pitUpdateEnergy; }
	public double getTotalPitUpdateEnergy() { return this.totalPitUpdateEnergy; }
	public double getPitRemoveEnergy() { return this.pitRemoveEnergy; }
	public double getTotalPitRemoveEnergy() { return this.totalPitRemoveEnergy; }
	public double getStaticCacheLookupEnergy() { return this.staticCacheLookupEnergy; }
	public double getTotalStaticCacheLookupEnergy() { return this.totalStaticCacheLookupEnergy; }
	public double getStaticCacheWriteEnergy() { return this.staticCacheWriteEnergy; }
	public double getTotalStaticCacheWriteEnergy() { return this.totalStaticCacheWriteEnergy; }
	public double getStaticCacheInitWriteEnergy() { return this.staticCacheInitWriteEnergy; }
	public double getTotalStaticCacheInitWriteEnergy() { return this.totalStaticCacheInitWriteEnergy; }
	public double getPacketTransmissionEnergy() { return this.packetTransmissionEnergy; }
	public double getTotalPacketTransmissionEnergy() { return this.totalPacketTransmissionEnergy; }
	public double getPacketReceptionEnergy() { return this.packetReceptionEnergy; }
	public double getTotalPacketReceptionEnergy() { return this.totalPacketReceptionEnergy; }
	public double getOppCacheLookupEnergy() { return this.oppoCacheLookupEnergy; }
	public double getTotalOppCacheLookupEnergy() { return this.totalOppCacheLookupEnergy; }
	public double getOppCacheWriteEnergy() { return this.oppoCacheWriteEnergy; }
	public double getTotalOppCacheWriteEnergy() { return this.totalOppCacheWriteEnergy; }

	/**
	 * Updates the current energy so that the given amount is reduced from it.
	 * If the energy level goes below zero, sets the level to zero.
	 * Does nothing if the warmup time has not passed.
	 * @param amount The amount of energy to reduce
	 */
	protected void reduceEnergy(double amount) {
		if (SimClock.getTime() < this.warmupTime) {
			return;
		}

		if (comBus == null) {
			return; /* model not initialized (via update) yet */
		}

		this.totalEnergyConsumed += amount;
		comBus.updateProperty(ENERGY_CONSUMED_ID, this.totalEnergyConsumed);

		if (amount >= this.currentEnergy) {
			comBus.updateProperty(ENERGY_VALUE_ID, 0.0);
		} else {
			comBus.updateDouble(ENERGY_VALUE_ID, -amount);
		}

	}

	/**
	 * CCN-only energy accounting does not include generic radio scanning or
	 * transmission costs. This update remains to initialize the bus and maintain
	 * the current energy value for the simulator.
	 */
	public void update(NetworkInterface iface, ModuleCommunicationBus comBus) {
		double simTime = SimClock.getTime();
		double delta = simTime - this.lastUpdate;

		if (this.comBus == null) {
			this.comBus = comBus;
			this.comBus.addProperty(ENERGY_VALUE_ID, this.currentEnergy);
			this.comBus.addProperty(ENERGY_CONSUMED_ID, this.totalEnergyConsumed);
			this.comBus.subscribe(ENERGY_VALUE_ID, this);
		}
		this.lastUpdate = simTime;
	}

	/**
	 * Called by the combus if the energy value is changed
	 * @param key The energy ID
	 * @param newValue The new energy value
	 */
	public void moduleValueChanged(String key, Object newValue) {
		this.currentEnergy = (Double)newValue;
	}

	/**
	 * Reduces the energy reserve for the amount that is used when looking up entries
	 * in the PIT.
	 */
	public void reducePitLookupEnergy() {
		this.totalPitLookupEnergy += this.pitLookupEnergy;
		reduceEnergy(this.pitLookupEnergy);
	}

	public void reducePitInsertEnergy() {
		this.totalPitInsertEnergy += this.pitInsertEnergy;
		reduceEnergy(this.pitInsertEnergy);
	}

	public void reducePitUpdateEnergy() {
		this.totalPitUpdateEnergy += this.pitUpdateEnergy;
		reduceEnergy(this.pitUpdateEnergy);
	}

	public void reducePitRemoveEnergy() {
		this.totalPitRemoveEnergy += this.pitRemoveEnergy;
		reduceEnergy(this.pitRemoveEnergy);
	}

	public void reduceStaticCacheLookupEnergy() {
		this.totalStaticCacheLookupEnergy += this.staticCacheLookupEnergy;
		reduceEnergy(this.staticCacheLookupEnergy);
	}

	public void reduceStaticCacheWriteEnergy() {
		this.totalStaticCacheWriteEnergy += this.staticCacheWriteEnergy;
		reduceEnergy(this.staticCacheWriteEnergy);
	}

	public void reduceStaticCacheInitWriteEnergy() {
		this.totalStaticCacheInitWriteEnergy += this.staticCacheInitWriteEnergy;
		reduceEnergy(this.staticCacheInitWriteEnergy);
	}

	public void reducePacketTransmissionEnergy() {
		this.totalPacketTransmissionEnergy += this.packetTransmissionEnergy;
		reduceEnergy(this.packetTransmissionEnergy);
	}

	public void reducePacketReceptionEnergy() {
		this.totalPacketReceptionEnergy += this.packetReceptionEnergy;
		reduceEnergy(this.packetReceptionEnergy);
	}

	public void reduceOppCacheLookupEnergy() {
		this.totalOppCacheLookupEnergy += this.oppoCacheLookupEnergy;
		reduceEnergy(this.oppoCacheLookupEnergy);
	}

	public void reduceOppCacheWriteEnergy() {
		this.totalOppCacheWriteEnergy += this.oppoCacheWriteEnergy;
		reduceEnergy(this.oppoCacheWriteEnergy);
	}
}
